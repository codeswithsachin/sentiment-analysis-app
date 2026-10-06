package sentiment.analyzer;

import sentiment.storage.LexiconStore;

import java.util.*;

/**
 * Rule-based analyzer (VADER style). Every word has a score from the lexicon; rules adjust it:
 * negation, booster words, ALL CAPS, exclamation marks and "but". The sum is normalised to -1..+1.
 */
public final class LexiconAnalyzer implements Analyzer {
    private static final Map<String, Double> BOOST = new HashMap<>();
    static {
        for (String w : new String[]{"very", "really", "extremely", "so", "absolutely", "totally", "incredibly"}) BOOST.put(w, 0.293);
        BOOST.put("too", 0.2);
        for (String w : new String[]{"slightly", "somewhat", "barely", "kinda"}) BOOST.put(w, -0.293);
    }

    private final LexiconStore lexicon;

    public LexiconAnalyzer(LexiconStore lexicon) { this.lexicon = lexicon; }

    @Override public String id() { return "lexicon"; }

    @Override public String name() { return "Lexicon (rule based)"; }

    @Override public Result analyze(String text) {
        Result r = new Result();
        r.text = text; r.analyzer = id();
        List<String> toks = Tokenizer.split(text);
        int n = toks.size();
        String[] clean = new String[n];
        boolean mixedCase = false;
        for (int i = 0; i < n; i++) {
            clean[i] = Tokenizer.clean(toks.get(i));
            if (toks.get(i).matches(".*[a-z].*")) mixedCase = true;
        }
        int but = Arrays.asList(clean).indexOf("but");
        double sum = 0;
        double[] val = new double[n];
        for (int i = 0; i < n; i++) {
            Double base = lexicon.get(clean[i]);
            if (base == null) continue;
            double v = base, sg = Math.signum(v);
            String letters = toks.get(i).replaceAll("[^A-Za-z]", "");
            if (mixedCase && letters.length() > 1 && letters.equals(letters.toUpperCase())) v += sg * 0.733;   // CAPS
            boolean negated = false;
            for (int k = 1; k <= 3 && i - k >= 0; k++) {
                String p = clean[i - k];
                if (BOOST.containsKey(p)) v += sg * BOOST.get(p) * (1 - 0.05 * (k - 1));                       // booster
                if (Tokenizer.isNegation(p)) negated = true;
            }
            if (negated) v *= -0.74;                                                                           // negation
            if (but >= 0 && i != but) v *= (i < but) ? 0.5 : 1.5;                                              // "but" rule
            val[i] = v; sum += v;
        }
        long bangs = text.chars().filter(c -> c == '!').count();
        double ex = Math.min(bangs, 4) * 0.292;                                                                // "!"
        if (sum > 0) sum += ex; else if (sum < 0) sum -= ex;
        r.compound = sum / Math.sqrt(sum * sum + 15);                                                          // normalise
        double p = 0, ng = 0, u = 0;
        for (int i = 0; i < n; i++) {
            r.tokens.add(toks.get(i)); r.scores.add(val[i]);
            if (val[i] > 0) p += val[i] + 1; else if (val[i] < 0) ng += Math.abs(val[i]) + 1; else u++;
        }
        double tot = (p + ng + u) == 0 ? 1 : (p + ng + u);
        r.pos = p / tot; r.neg = ng / tot; r.neu = u / tot;
        r.label = Labels.fromScore(r.compound);
        return r;
    }
}
