package sentiment.analyzer;

import sentiment.util.Csv;

import java.util.ArrayList;
import java.util.List;

/** Labelled examples read from a CSV file with two columns: text,label  (label = positive / neutral / negative). */
public final class Dataset {
    public final List<Sample> samples = new ArrayList<>();
    public int skipped;

    public static Dataset parse(String csv) {
        Dataset d = new Dataset();
        for (String[] r : Csv.parse(csv)) {
            if (r.length < 2 || r[0].trim().isEmpty()) { d.skipped++; continue; }
            int label = Labels.parse(r[r.length - 1]);
            if (label < 0) { d.skipped++; continue; }      // also skips the header row
            d.samples.add(new Sample(r[0].trim(), label));
        }
        return d;
    }

    public int[] counts() {
        int[] c = new int[3];
        for (Sample s : samples) c[s.label]++;
        return c;
    }
}
