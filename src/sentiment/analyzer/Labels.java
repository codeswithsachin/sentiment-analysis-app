package sentiment.analyzer;

/** The three sentiment classes. Index order is used everywhere (confusion matrices, probabilities). */
public final class Labels {
    private Labels() { }

    public static final String[] NAMES = {"Positive", "Neutral", "Negative"};

    public static int index(String label) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(label)) return i;
        return -1;
    }

    /** Converts a label written in a CSV file ("positive", "pos", "Negative", "neu"...) to its index, or -1. */
    public static int parse(String raw) {
        if (raw == null) return -1;
        String s = raw.trim().toLowerCase();
        if (s.startsWith("pos")) return 0;
        if (s.startsWith("neu")) return 1;
        if (s.startsWith("neg")) return 2;
        return -1;
    }

    public static String fromScore(double compound) {
        return compound >= 0.05 ? NAMES[0] : compound <= -0.05 ? NAMES[2] : NAMES[1];
    }
}
