package sentiment.test;

import sentiment.analyzer.*;
import sentiment.storage.LexiconStore;
import sentiment.util.Csv;
import sentiment.util.Json;
import sentiment.util.Res;

import java.util.*;

/**
 * Offline self-test (no network): checks the lexicon analyzer's rules and the Naive Bayes classifier's
 * accuracy on the bundled training set, plus the small Json/Csv helpers.
 * Run: java -cp out sentiment.test.EngineSelfTest
 */
public final class EngineSelfTest {
    static int checks, failures;

    static void check(boolean ok, String msg) {
        checks++;
        if (!ok) { failures++; System.out.println("FAIL: " + msg); }
    }

    public static void main(String[] args) throws Exception {
        LexiconStore lex = new LexiconStore();
        LexiconAnalyzer a = new LexiconAnalyzer(lex);

        check(a.analyze("I love this, it is great!").label.equals("Positive"), "clearly positive text");
        check(a.analyze("I hate this, it is terrible.").label.equals("Negative"), "clearly negative text");
        check(a.analyze("The parcel arrived on Tuesday.").label.equals("Neutral"), "neutral factual text");
        check(a.analyze("This is not good at all.").compound < 0, "negation should flip polarity");
        Result caps = a.analyze("This is GREAT");
        Result plain = a.analyze("this is great");
        check(caps.compound > plain.compound, "ALL CAPS should increase intensity");
        Result bang = a.analyze("great!!!");
        Result noBang = a.analyze("great");
        check(bang.compound > noBang.compound, "exclamation marks should increase intensity");
        Result veryGood = a.analyze("this is very good");
        Result good = a.analyze("this is good");
        check(veryGood.compound > good.compound, "booster word should increase intensity");

        // JSON round trip
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", 1.5); m.put("b", "hi \"there\""); m.put("c", Arrays.asList(1, 2, 3));
        String j = Json.write(m);
        Object back = Json.parse(j);
        check(back instanceof Map && ((Map<?, ?>) back).get("b").equals("hi \"there\""), "JSON round trip preserves strings");

        // CSV with quotes and commas
        List<String[]> rows = Csv.parse("text,label\n\"hello, world\",positive\n\"line1\nline2\",neutral\n");
        check(rows.size() == 3 && rows.get(1)[0].equals("hello, world"), "CSV parses quoted commas");
        check(rows.get(2)[0].equals("line1\nline2"), "CSV parses quoted newlines");

        // Naive Bayes: train/test split on the bundled dataset, expect solid accuracy
        String csv = Res.readDefault("data/training.csv");
        check(csv != null, "bundled training.csv is readable");
        Dataset d = Dataset.parse(csv);
        check(d.samples.size() > 100, "bundled dataset has a reasonable number of rows");
        List<Sample> all = new ArrayList<>(d.samples);
        Collections.shuffle(all, new Random(1));
        int nTest = all.size() / 4;
        List<Sample> test = all.subList(0, nTest), train = all.subList(nTest, all.size());
        NaiveBayesAnalyzer nb = new NaiveBayesAnalyzer();
        nb.train(train);
        int correct = 0;
        for (Sample s : test) if (nb.analyze(s.text).label.equals(Labels.NAMES[s.label])) correct++;
        double acc = (double) correct / test.size();
        check(acc > 0.75, "Naive Bayes test accuracy should be reasonably high, was " + acc);
        System.out.println("Naive Bayes held-out accuracy: " + String.format(Locale.ROOT, "%.1f%%", acc * 100));

        // Retraining changes predictions (proves it is not hard-coded)
        List<Sample> tiny = Arrays.asList(new Sample("zibblewonk", 0), new Sample("zibblewonk", 0), new Sample("gorptastic", 2), new Sample("gorptastic", 2));
        NaiveBayesAnalyzer nb2 = new NaiveBayesAnalyzer();
        nb2.train(tiny);
        check(nb2.analyze("zibblewonk").label.equals("Positive"), "NB should learn an unseen made-up word as positive");
        check(nb2.analyze("gorptastic").label.equals("Negative"), "NB should learn an unseen made-up word as negative");

        // Lexicon editing at runtime
        check(lex.get("zzcustomword") == null, "unknown word starts unscored");
        lex.set("zzcustomword", 3.0);
        check(lex.get("zzcustomword") == 3.0, "custom word can be added");
        check(a.analyze("zzcustomword").label.equals("Positive"), "custom word affects analysis immediately");
        lex.remove("zzcustomword");
        check(lex.get("zzcustomword") == null, "custom word can be removed");

        System.out.println(checks + " checks, " + failures + " failed.");
        if (failures > 0) System.exit(1);
        System.out.println("ALL TESTS PASSED");
    }
}
