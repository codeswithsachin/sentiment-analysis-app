package sentiment.service;

import sentiment.analyzer.*;

import java.util.*;

/** Measures how good an analyzer is on labelled data: accuracy, precision, recall, F1 and confusion matrix. */
public final class Evaluation {
    private Evaluation() { }

    public static Map<String, Object> run(List<Sample> data, double testFraction, long seed, Analyzer lexicon) {
        List<Sample> all = new ArrayList<>(data);
        Collections.shuffle(all, new Random(seed));
        int nTest = Math.max(1, (int) Math.round(all.size() * testFraction));
        List<Sample> test = new ArrayList<>(all.subList(0, nTest));
        List<Sample> train = new ArrayList<>(all.subList(nTest, all.size()));
        NaiveBayesAnalyzer nb = new NaiveBayesAnalyzer();       // trained on the training part only
        nb.train(train);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("trainSize", train.size()); out.put("testSize", test.size());
        List<Object> models = new ArrayList<>();
        models.add(metrics(lexicon, test));
        models.add(metrics(nb, test));
        out.put("models", models);
        return out;
    }

    static Map<String, Object> metrics(Analyzer a, List<Sample> test) {
        int[][] cm = new int[3][3];                              // rows = actual, columns = predicted
        for (Sample s : test) cm[s.label][Labels.index(a.analyze(s.text).label)]++;
        int correct = 0;
        for (int i = 0; i < 3; i++) correct += cm[i][i];
        List<Object> per = new ArrayList<>();
        double f1sum = 0;
        for (int c = 0; c < 3; c++) {
            int tp = cm[c][c], fp = 0, fn = 0;
            for (int k = 0; k < 3; k++) if (k != c) { fp += cm[k][c]; fn += cm[c][k]; }
            double p = tp + fp == 0 ? 0 : (double) tp / (tp + fp), r = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
            double f1 = p + r == 0 ? 0 : 2 * p * r / (p + r);
            f1sum += f1;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", Labels.NAMES[c]); m.put("precision", p); m.put("recall", r); m.put("f1", f1); m.put("support", tp + fn);
            per.add(m);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id()); m.put("name", a.name());
        m.put("accuracy", test.isEmpty() ? 0.0 : (double) correct / test.size());
        m.put("macroF1", f1sum / 3);
        m.put("confusion", cm); m.put("perClass", per);
        return m;
    }
}
