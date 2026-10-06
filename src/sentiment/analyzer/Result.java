package sentiment.analyzer;

import java.util.*;

/** Outcome of analysing one text. */
public final class Result {
    public String text, label, analyzer;
    public double compound;                       // -1 (very negative) .. +1 (very positive)
    public double pos, neu, neg;                  // shares that add up to 1
    public final List<String> tokens = new ArrayList<>();
    public final List<Double> scores = new ArrayList<>();   // contribution of each token (0 = none)

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("text", text); m.put("label", label); m.put("analyzer", analyzer);
        m.put("compound", compound); m.put("pos", pos); m.put("neu", neu); m.put("neg", neg);
        List<Object> t = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("t", tokens.get(i)); x.put("v", scores.get(i));
            t.add(x);
        }
        m.put("tokens", t);
        return m;
    }
}
