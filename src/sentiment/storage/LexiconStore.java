package sentiment.storage;

import sentiment.util.Csv;
import sentiment.util.Res;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * The sentiment lexicon (word -> score). The bundled lexicon is read-only; your changes are saved in
 * data/lexicon-custom.csv, so you can add slang words or change scores while the app is running.
 */
public final class LexiconStore {
    private final Map<String, Double> base = new TreeMap<>();
    private final Map<String, Double> custom = new TreeMap<>();
    private final Set<String> removed = new TreeSet<>();
    private final File customFile = Res.dataFile("lexicon-custom.csv");
    private volatile Map<String, Double> effective = new HashMap<>();

    public LexiconStore() throws IOException {
        String def = Res.readDefault("data/lexicon.csv");
        if (def != null) for (String[] r : Csv.parse(def)) put(base, r);
        if (customFile.isFile()) {
            for (String[] r : Csv.parse(new String(Files.readAllBytes(customFile.toPath()), StandardCharsets.UTF_8))) {
                if (r.length < 2) continue;
                String w = r[0].trim().toLowerCase();
                if (r[1].trim().equalsIgnoreCase("REMOVED")) removed.add(w); else put(custom, r);
            }
        }
        rebuild();
    }

    private static void put(Map<String, Double> m, String[] r) {
        if (r.length < 2) return;
        try { m.put(r[0].trim().toLowerCase(), Double.parseDouble(r[1].trim())); } catch (NumberFormatException ignored) { }
    }

    private void rebuild() {
        Map<String, Double> e = new HashMap<>(base);
        e.putAll(custom);
        for (String w : removed) e.remove(w);
        effective = e;
    }

    /** Score of a word or null. Lock-free, safe to call from many threads. */
    public Double get(String word) { return effective.get(word); }

    public int size() { return effective.size(); }

    public synchronized void set(String word, double score) throws IOException {
        word = word.trim().toLowerCase();
        if (word.isEmpty() || word.contains(" ")) throw new IllegalArgumentException("Enter a single word (no spaces).");
        if (score < -4 || score > 4) throw new IllegalArgumentException("Score must be between -4 and +4.");
        removed.remove(word);
        custom.put(word, score);
        save(); rebuild();
    }

    public synchronized void remove(String word) throws IOException {
        word = word.trim().toLowerCase();
        custom.remove(word);
        if (base.containsKey(word)) removed.add(word);
        save(); rebuild();
    }

    public synchronized void resetCustom() throws IOException {
        custom.clear(); removed.clear();
        Files.deleteIfExists(customFile.toPath());
        rebuild();
    }

    private void save() throws IOException {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Double> e : custom.entrySet()) b.append(Csv.esc(e.getKey())).append(',').append(e.getValue()).append('\n');
        for (String w : removed) b.append(Csv.esc(w)).append(",REMOVED\n");
        Files.write(customFile.toPath(), b.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Rows for the Lexicon page: word, score, source (bundled / custom). */
    public synchronized List<Map<String, Object>> list(String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : new TreeMap<>(effective).entrySet()) {
            if (!q.isEmpty() && !e.getKey().contains(q)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("word", e.getKey()); m.put("score", e.getValue());
            m.put("source", custom.containsKey(e.getKey()) ? (base.containsKey(e.getKey()) ? "changed" : "added") : "built-in");
            out.add(m);
            if (out.size() >= limit) break;
        }
        return out;
    }
}
