package sentiment.util;

import java.util.ArrayList;
import java.util.List;

/** Minimal CSV reader / writer (handles quotes, commas and line breaks inside quotes). */
public final class Csv {
    private Csv() { }

    public static List<String[]> parse(String text) {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder f = new StringBuilder();
        boolean q = false, any = false;
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (q) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { f.append('"'); i++; } else q = false;
                } else f.append(c);
            } else if (c == '"') { q = true; any = true; }
            else if (c == ',') { row.add(f.toString()); f.setLength(0); any = true; }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                if (any || f.length() > 0) { row.add(f.toString()); rows.add(row.toArray(new String[0])); }
                row = new ArrayList<>(); f.setLength(0); any = false;
            } else { f.append(c); any = true; }
        }
        if (any || f.length() > 0) { row.add(f.toString()); rows.add(row.toArray(new String[0])); }
        return rows;
    }

    public static String esc(String s) {
        if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0 && s.indexOf('\r') < 0) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
