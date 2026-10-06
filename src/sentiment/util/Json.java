package sentiment.util;

import java.util.*;

/** Tiny JSON writer and parser (no external libraries). */
public final class Json {
    private Json() { }

    // ---------------- writer ----------------
    public static String write(Object o) { StringBuilder b = new StringBuilder(); w(b, o); return b.toString(); }

    @SuppressWarnings("unchecked")
    private static void w(StringBuilder b, Object o) {
        if (o == null) b.append("null");
        else if (o instanceof String) quote(b, (String) o);
        else if (o instanceof Boolean) b.append(o);
        else if (o instanceof Double || o instanceof Float) {
            double d = ((Number) o).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) b.append("null"); else b.append(String.format(Locale.ROOT, "%.4f", d));
        } else if (o instanceof Number) b.append(o);
        else if (o instanceof Map) {
            b.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                if (!first) b.append(',');
                first = false;
                quote(b, e.getKey()); b.append(':'); w(b, e.getValue());
            }
            b.append('}');
        } else if (o instanceof Iterable) {
            b.append('[');
            boolean first = true;
            for (Object x : (Iterable<Object>) o) { if (!first) b.append(','); first = false; w(b, x); }
            b.append(']');
        } else if (o instanceof int[]) {
            b.append('[');
            int[] a = (int[]) o;
            for (int i = 0; i < a.length; i++) { if (i > 0) b.append(','); b.append(a[i]); }
            b.append(']');
        } else if (o instanceof int[][]) {
            b.append('[');
            int[][] a = (int[][]) o;
            for (int i = 0; i < a.length; i++) { if (i > 0) b.append(','); w(b, a[i]); }
            b.append(']');
        } else quote(b, o.toString());
    }

    private static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
            }
        }
        b.append('"');
    }

    // ---------------- parser ----------------
    public static Object parse(String s) {
        try {
            P p = new P(s);
            Object v = p.val();
            p.ws();
            if (p.i != s.length()) throw new IllegalArgumentException("Invalid JSON");
            return v;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid JSON");
        }
    }

    private static final class P {
        final String s; int i;
        P(String s) { this.s = s; }

        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        IllegalArgumentException err() { return new IllegalArgumentException("Invalid JSON"); }

        Object val() {
            ws();
            char c = s.charAt(i);
            if (c == '{') return obj();
            if (c == '[') return arr();
            if (c == '"') return str();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (st == i) throw err();
            return Double.parseDouble(s.substring(st, i));
        }

        Map<String, Object> obj() {
            i++;
            Map<String, Object> m = new LinkedHashMap<>();
            ws();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws(); String k = str(); ws();
                if (s.charAt(i++) != ':') throw err();
                m.put(k, val()); ws();
                char c = s.charAt(i++);
                if (c == '}') break;
                if (c != ',') throw err();
            }
            return m;
        }

        List<Object> arr() {
            i++;
            List<Object> l = new ArrayList<>();
            ws();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(val()); ws();
                char c = s.charAt(i++);
                if (c == ']') break;
                if (c != ',') throw err();
            }
            return l;
        }

        String str() {
            if (s.charAt(i) != '"') throw err();
            i++;
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n': b.append('\n'); break;
                        case 't': b.append('\t'); break;
                        case 'r': b.append('\r'); break;
                        case 'b': b.append('\b'); break;
                        case 'f': b.append('\f'); break;
                        case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                        default: b.append(e);
                    }
                } else b.append(c);
            }
            return b.toString();
        }
    }

    // ---------------- helpers for request bodies ----------------
    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObj(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    public static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v instanceof String ? (String) v : def;
    }

    public static double num(Map<String, Object> m, String k, double def) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    public static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v instanceof Boolean ? (Boolean) v : def;
    }
}
