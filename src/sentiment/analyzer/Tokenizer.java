package sentiment.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Splits text into tokens and normalises them. Shared by both analyzers. */
public final class Tokenizer {
    private Tokenizer() { }

    /** Emojis the app understands (written as escapes so the source compiles on any system). */
    public static final String[] EMOJI = {
        "\uD83D\uDE00", "\uD83D\uDE0A", "\uD83D\uDE0D", "\u2764", "\uD83D\uDC4D", "\uD83D\uDE02", "\uD83D\uDD25",
        "\uD83D\uDE21", "\uD83D\uDE22", "\uD83D\uDC4E", "\uD83D\uDE1E", "\uD83E\uDD2C", "\uD83D\uDE2D"};

    /** Raw tokens, in the original order (used for highlighting in the UI). */
    public static List<String> split(String text) {
        String t = text.replace("\uFE0F", "").replace('\u2019', '\'');
        for (String e : EMOJI) t = t.replace(e, " " + e + " ");
        List<String> out = new ArrayList<>();
        for (String s : t.trim().split("\\s+")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    /** Lower-case token without surrounding punctuation ("Great!!" gives "great"). */
    public static String clean(String raw) {
        String r = raw.toLowerCase(Locale.ROOT);
        for (String e : EMOJI) if (r.equals(e)) return r;
        return r.replaceAll("^[^\\p{L}\\p{N}']+|[^\\p{L}\\p{N}']+$", "");
    }

    private static final java.util.Set<String> NEG = new java.util.HashSet<>(java.util.Arrays.asList(
            "not", "no", "never", "nothing", "nobody", "neither", "hardly", "without", "cannot",
            "dont", "cant", "wont", "isnt", "wasnt", "didnt", "doesnt", "couldnt", "shouldnt", "arent"));

    public static boolean isNegation(String w) { return NEG.contains(w) || w.endsWith("n't"); }
}
