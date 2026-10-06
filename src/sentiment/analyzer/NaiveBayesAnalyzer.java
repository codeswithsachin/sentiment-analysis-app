package sentiment.analyzer;

import java.util.*;

/**
 * Machine-learning analyzer: multinomial Naive Bayes with Laplace smoothing, written from scratch.
 * It LEARNS word statistics from labelled examples, so it can be retrained on your own data.
 * Negation handling: the 3 words after "not", "never"... are marked (not_good), so "not good" differs from "good".
 */
public final class NaiveBayesAnalyzer implements Analyzer {
    private static final class Model {
        final Map<String, int[]> counts = new HashMap<>();   // word -> count per class
        final int[] words = new int[3];                       // total words per class
        final int[] docs = new int[3];                        // documents per class
        int total;
    }

    private volatile Model model;

    @Override public String id() { return "naivebayes"; }

    @Override public String name() { return "Naive Bayes (machine learning)"; }

    public boolean isTrained() { return model != null && model.total > 0; }

    public int sampleCount() { return model == null ? 0 : model.total; }

    public int vocabularySize() { return model == null ? 0 : model.counts.size(); }

    public synchronized void train(List<Sample> data) {
        Model m = new Model();
        for (Sample s : data) {
            m.docs[s.label]++; m.total++;
            for (String f : features(Tokenizer.split(s.text))) {
                if (f == null) continue;
                m.counts.computeIfAbsent(f, k -> new int[3])[s.label]++;
                m.words[s.label]++;
            }
        }
        model = m;
    }

    /** One feature per token (null for tokens that are only punctuation). */
    static List<String> features(List<String> toks) {
        List<String> out = new ArrayList<>();
        int negLeft = 0;
        for (String t : toks) {
            String w = Tokenizer.clean(t);
            if (w.isEmpty()) { out.add(null); continue; }
            String f;
            if (Tokenizer.isNegation(w)) { negLeft = 3; f = w; }
            else { f = negLeft > 0 ? "not_" + w : w; if (negLeft > 0) negLeft--; }
            if (t.endsWith(".") || t.endsWith(",") || t.endsWith("!") || t.endsWith("?") || t.endsWith(";")) negLeft = 0;
            out.add(f);
        }
        return out;
    }

    @Override public Result analyze(String text) {
        Result r = new Result();
        r.text = text; r.analyzer = id();
        Model m = model;
        List<String> toks = Tokenizer.split(text);
        List<String> feats = features(toks);
        if (m == null || m.total == 0) {                      // not trained yet
            for (String t : toks) { r.tokens.add(t); r.scores.add(0.0); }
            r.label = Labels.NAMES[1]; r.neu = 1;
            return r;
        }
        int vocab = Math.max(1, m.counts.size());
        double[] lp = new double[3];
        for (int c = 0; c < 3; c++) lp[c] = Math.log((m.docs[c] + 1.0) / (m.total + 3.0));
        for (String f : feats) {
            int[] cnt = f == null ? null : m.counts.get(f);
            if (cnt == null) continue;
            for (int c = 0; c < 3; c++) lp[c] += Math.log((cnt[c] + 1.0) / (m.words[c] + vocab));
        }
        double max = Math.max(lp[0], Math.max(lp[1], lp[2])), sum = 0;
        double[] p = new double[3];
        for (int c = 0; c < 3; c++) { p[c] = Math.exp(lp[c] - max); sum += p[c]; }
        int best = 0;
        for (int c = 0; c < 3; c++) { p[c] /= sum; if (p[c] > p[best]) best = c; }
        r.pos = p[0]; r.neu = p[1]; r.neg = p[2];
        r.compound = p[0] - p[2];
        r.label = Labels.NAMES[best];
        for (int i = 0; i < toks.size(); i++) {               // contribution = log-likelihood ratio positive vs negative
            String f = feats.get(i);
            int[] cnt = f == null ? null : m.counts.get(f);
            double v = 0;
            if (cnt != null && cnt[0] + cnt[1] + cnt[2] >= 2)
                v = Math.log((cnt[0] + 1.0) / (m.words[0] + vocab)) - Math.log((cnt[2] + 1.0) / (m.words[2] + vocab));
            r.tokens.add(toks.get(i)); r.scores.add(Math.max(-3, Math.min(3, v)));
        }
        return r;
    }
}
