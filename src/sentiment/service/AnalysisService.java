package sentiment.service;

import sentiment.analyzer.*;
import sentiment.storage.HistoryStore;
import sentiment.storage.LexiconStore;
import sentiment.util.Csv;
import sentiment.util.Res;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Business logic of the app. The web layer only converts HTTP to calls on this class. */
public final class AnalysisService {
    public final LexiconStore lexicon;
    public final LexiconAnalyzer lex;
    public final NaiveBayesAnalyzer nb = new NaiveBayesAnalyzer();
    public final HistoryStore history;
    private volatile List<Sample> dataset = new ArrayList<>();
    private volatile String datasetName = "";
    private final List<String> feedPool = new ArrayList<>();
    private final Random rnd = new Random();
    private final File customTraining = Res.dataFile("training-custom.csv");

    public AnalysisService() throws IOException {
        lexicon = new LexiconStore();
        lex = new LexiconAnalyzer(lexicon);
        history = new HistoryStore();
        String csv;
        if (customTraining.isFile()) { csv = new String(Files.readAllBytes(customTraining.toPath()), StandardCharsets.UTF_8); datasetName = "your uploaded dataset"; }
        else { csv = Res.readDefault("data/training.csv"); datasetName = "built-in sample dataset"; }
        if (csv != null) { dataset = Dataset.parse(csv).samples; nb.train(dataset); }
        String feed = Res.readDefault("data/feed.csv");
        if (feed != null) for (String[] r : Csv.parse(feed)) if (r.length > 0 && !r[0].trim().isEmpty()) feedPool.add(r[0].trim());
    }

    public Analyzer analyzer(String id) { return "naivebayes".equals(id) || "nb".equals(id) ? nb : lex; }

    public Result analyze(String text, String analyzerId, boolean save) throws IOException {
        Result r = analyzer(analyzerId).analyze(text);
        if (save) history.addAll(Collections.singletonList(r));
        return r;
    }

    public List<Result> batch(List<String> texts, String analyzerId, boolean save) throws IOException {
        Analyzer a = analyzer(analyzerId);
        List<Result> out = new ArrayList<>();
        for (String t : texts) out.add(a.analyze(t));
        if (save) history.addAll(out);
        return out;
    }

    /** Extracts the texts of a CSV upload: uses the column called text/review/comment/post/tweet, else the first column. */
    public static List<String> textsFromCsv(String csv) {
        List<String[]> rows = Csv.parse(csv);
        List<String> out = new ArrayList<>();
        if (rows.isEmpty()) return out;
        int col = 0, start = 0;
        String[] head = rows.get(0);
        for (int i = 0; i < head.length; i++) {
            String h = head[i].trim().toLowerCase();
            if (h.equals("text") || h.equals("review") || h.equals("comment") || h.equals("post") || h.equals("tweet") || h.equals("content")) { col = i; start = 1; break; }
        }
        for (int i = start; i < rows.size(); i++) if (col < rows.get(i).length && !rows.get(i)[col].trim().isEmpty()) out.add(rows.get(i)[col].trim());
        return out;
    }

    // ---------------- training and evaluation ----------------
    public Map<String, Object> train(String csv) throws IOException {
        Dataset d;
        if (csv == null || csv.trim().isEmpty()) {
            Files.deleteIfExists(customTraining.toPath());
            d = Dataset.parse(Res.readDefault("data/training.csv"));
            datasetName = "built-in sample dataset";
        } else {
            d = Dataset.parse(csv);
            int[] c = d.counts();
            if (d.samples.size() < 30 || c[0] == 0 || c[2] == 0)
                throw new IllegalArgumentException("Need at least 30 rows with 'positive' and 'negative' labels (format: text,label).");
            Files.write(customTraining.toPath(), csv.getBytes(StandardCharsets.UTF_8));
            datasetName = "your uploaded dataset";
        }
        dataset = d.samples;
        nb.train(dataset);
        Map<String, Object> m = datasetInfo();
        m.put("skipped", d.skipped);
        return m;
    }

    public Map<String, Object> datasetInfo() {
        List<Sample> ds = dataset;
        int[] c = new int[3];
        for (Sample s : ds) c[s.label]++;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", datasetName); m.put("size", ds.size()); m.put("counts", c);
        m.put("vocabulary", nb.vocabularySize());
        List<Object> ex = new ArrayList<>();
        for (int i = 0; i < Math.min(8, ds.size()); i++) {
            Sample s = ds.get(rnd.nextInt(ds.size()));
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("text", s.text); e.put("label", Labels.NAMES[s.label]);
            ex.add(e);
        }
        m.put("examples", ex);
        return m;
    }

    public Map<String, Object> evaluate(double testFraction, long seed) {
        if (dataset.size() < 10) throw new IllegalArgumentException("Not enough training data to evaluate.");
        return Evaluation.run(dataset, Math.max(0.05, Math.min(0.5, testFraction)), seed, lex);
    }

    // ---------------- live feed ----------------
    private static final String[] PLATFORMS = {"Twitter", "Instagram", "Facebook", "YouTube"};

    public Map<String, Object> feedNext(String analyzerId) throws IOException {
        if (feedPool.isEmpty()) throw new IllegalArgumentException("No feed data found (data/feed.csv).");
        String text = feedPool.get(rnd.nextInt(feedPool.size()));
        Result r = analyze(text, analyzerId, true);
        Map<String, Object> m = r.toMap();
        m.put("handle", "@user" + (100 + rnd.nextInt(900)));
        m.put("platform", PLATFORMS[rnd.nextInt(PLATFORMS.length)]);
        return m;
    }

    // ---------------- dashboard ----------------
    public Map<String, Object> stats() {
        List<HistoryStore.Entry> h = history.snapshot();
        int[] counts = new int[3];
        double sum = 0;
        Map<String, Integer> byAnalyzer = new TreeMap<>();
        Map<String, Integer> posWords = new HashMap<>(), negWords = new HashMap<>();
        for (HistoryStore.Entry e : h) {
            counts[Math.max(0, Labels.index(e.label))]++;
            sum += e.compound;
            byAnalyzer.merge(e.analyzer, 1, Integer::sum);
            for (String tok : Tokenizer.split(e.text)) {
                String w = Tokenizer.clean(tok);
                Double s = lexicon.get(w);
                if (s == null || w.length() < 2) continue;
                if (s > 0) posWords.merge(w, 1, Integer::sum); else if (s < 0) negWords.merge(w, 1, Integer::sum);
            }
        }
        List<Object> trend = new ArrayList<>();
        for (int i = Math.max(0, h.size() - 60); i < h.size(); i++) trend.add(h.get(i).compound);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", h.size()); m.put("counts", counts);
        m.put("avg", h.isEmpty() ? 0.0 : sum / h.size());
        m.put("trend", trend); m.put("byAnalyzer", new LinkedHashMap<String, Object>(byAnalyzer));
        m.put("topPositive", top(posWords)); m.put("topNegative", top(negWords));
        return m;
    }

    private static List<Object> top(Map<String, Integer> counts) {
        List<Map.Entry<String, Integer>> es = new ArrayList<>(counts.entrySet());
        es.sort((a, b) -> b.getValue() != a.getValue().intValue() ? b.getValue() - a.getValue() : a.getKey().compareTo(b.getKey()));
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < Math.min(8, es.size()); i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("word", es.get(i).getKey()); m.put("count", es.get(i).getValue());
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lexiconSize", lexicon.size()); m.put("historyCount", history.size());
        m.put("nbTrained", nb.isTrained()); m.put("nbSamples", nb.sampleCount()); m.put("dataset", datasetName);
        return m;
    }
}
