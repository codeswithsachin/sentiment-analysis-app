package sentiment.analyzer;

/** One labelled training example. */
public final class Sample {
    public final String text;
    public final int label;   // index into Labels.NAMES

    public Sample(String text, int label) { this.text = text; this.label = label; }
}
