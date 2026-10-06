package sentiment.storage;

import sentiment.analyzer.Result;
import sentiment.util.Csv;
import sentiment.util.Res;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

/** Every analysed text is remembered here (memory + data/history.csv), newest 5000 entries. */
public final class HistoryStore {
    public static final class Entry {
        public final long id, ts;
        public final String analyzer, label, text;
        public final double compound;

        Entry(long id, long ts, String analyzer, String label, double compound, String text) {
            this.id = id; this.ts = ts; this.analyzer = analyzer; this.label = label; this.compound = compound; this.text = text;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id); m.put("ts", ts); m.put("analyzer", analyzer); m.put("label", label);
            m.put("compound", compound); m.put("text", text);
            return m;
        }
    }

    private static final int CAP = 5000;
    private final List<Entry> list = new ArrayList<>();
    private final File file = Res.dataFile("history.csv");
    private long nextId = 1;

    public HistoryStore() throws IOException {
        if (!file.isFile()) return;
        for (String[] r : Csv.parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))) {
            try {
                long id = Long.parseLong(r[0]);
                list.add(new Entry(id, Long.parseLong(r[1]), r[2], r[3], Double.parseDouble(r[4]), r[5]));
                nextId = Math.max(nextId, id + 1);
            } catch (RuntimeException ignored) { }
        }
    }

    private static String line(Entry e) {
        return e.id + "," + e.ts + "," + e.analyzer + "," + e.label + "," + e.compound + "," + Csv.esc(e.text) + "\n";
    }

    public synchronized void addAll(List<Result> results) throws IOException {
        StringBuilder b = new StringBuilder();
        long now = System.currentTimeMillis();
        for (Result r : results) {
            Entry e = new Entry(nextId++, now, r.analyzer, r.label, r.compound, r.text);
            list.add(e); b.append(line(e));
        }
        if (list.size() > CAP) { list.subList(0, list.size() - CAP).clear(); rewrite(); }
        else Files.write(file.toPath(), b.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public synchronized boolean delete(long id) throws IOException {
        boolean removed = list.removeIf(e -> e.id == id);
        if (removed) rewrite();
        return removed;
    }

    public synchronized void clear() throws IOException { list.clear(); rewrite(); }

    private void rewrite() throws IOException {
        StringBuilder b = new StringBuilder();
        for (Entry e : list) b.append(line(e));
        Files.write(file.toPath(), b.toString().getBytes(StandardCharsets.UTF_8));
    }

    public synchronized List<Entry> snapshot() { return new ArrayList<>(list); }

    /** Newest first, filtered by text and/or label. */
    public synchronized List<Entry> search(String q, String label, int limit) {
        String needle = q == null ? "" : q.toLowerCase();
        List<Entry> out = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0 && out.size() < limit; i--) {
            Entry e = list.get(i);
            if (!needle.isEmpty() && !e.text.toLowerCase().contains(needle)) continue;
            if (label != null && !label.isEmpty() && !label.equals("All") && !e.label.equals(label)) continue;
            out.add(e);
        }
        return out;
    }

    public synchronized int size() { return list.size(); }
}
