package sentiment.web;

import sentiment.analyzer.Result;
import sentiment.service.AnalysisService;
import sentiment.storage.HistoryStore;
import sentiment.util.Json;

import java.util.*;

/** Turns each parsed HTTP request into one call on AnalysisService, and results into plain JSON-able objects. */
final class ApiHandlers {
    private final AnalysisService s;

    ApiHandlers(AnalysisService s) { this.s = s; }

    private static List<Object> results(List<Result> list) {
        List<Object> out = new ArrayList<>();
        for (Result r : list) out.add(r.toMap());
        return out;
    }

    Object analyze(Map<String, Object> b, Map<String, String> q) throws Exception {
        String text = Json.str(b, "text", "");
        if (text.trim().isEmpty()) throw new IllegalArgumentException("Type some text first.");
        return s.analyze(text, Json.str(b, "analyzer", "lexicon"), true).toMap();
    }

    @SuppressWarnings("unchecked")
    Object batch(Map<String, Object> b, Map<String, String> q) throws Exception {
        List<String> lines = new ArrayList<>();
        for (Object o : (List<Object>) b.getOrDefault("texts", List.of())) if (o != null) lines.add(o.toString());
        List<String> clean = new ArrayList<>();
        for (String l : lines) if (!l.trim().isEmpty()) clean.add(l.trim());
        if (clean.isEmpty()) throw new IllegalArgumentException("Give at least one non-empty line.");
        if (clean.size() > 2000) throw new IllegalArgumentException("Please keep batches to 2000 lines or fewer.");
        List<Result> out = s.batch(clean, Json.str(b, "analyzer", "lexicon"), true);
        return Map.of("results", results(out));
    }

    Object upload(Map<String, Object> b, Map<String, String> q) throws Exception {
        String csv = Json.str(b, "csv", "");
        List<String> texts = AnalysisService.textsFromCsv(csv);
        if (texts.isEmpty()) throw new IllegalArgumentException("No text column found. Use a header like 'text' or 'review'.");
        if (texts.size() > 5000) texts = texts.subList(0, 5000);
        List<Result> out = s.batch(texts, Json.str(b, "analyzer", "lexicon"), true);
        return Map.of("results", results(out), "count", texts.size());
    }

    Object feedNext(Map<String, Object> b, Map<String, String> q) throws Exception {
        return s.feedNext(q.getOrDefault("analyzer", "lexicon"));
    }

    Object stats(Map<String, Object> b, Map<String, String> q) { return s.stats(); }

    Object status(Map<String, Object> b, Map<String, String> q) { return s.status(); }

    Object history(Map<String, Object> b, Map<String, String> q) {
        int limit = Integer.parseInt(q.getOrDefault("limit", "100"));
        List<Object> out = new ArrayList<>();
        for (HistoryStore.Entry e : s.history.search(q.get("q"), q.get("label"), Math.max(1, Math.min(500, limit)))) out.add(e.toMap());
        return Map.of("entries", out, "total", s.history.size());
    }

    Object historyDelete(Map<String, Object> b, Map<String, String> q) throws Exception {
        long id = (long) Json.num(b, "id", -1);
        if (id < 0 || !s.history.delete(id)) throw new IllegalArgumentException("Entry not found.");
        return Map.of("deleted", id);
    }

    Object historyClear(Map<String, Object> b, Map<String, String> q) throws Exception { s.history.clear(); return Map.of(); }

    Object lexiconList(Map<String, Object> b, Map<String, String> q) {
        int limit = Integer.parseInt(q.getOrDefault("limit", "200"));
        return Map.of("words", s.lexicon.list(q.get("q"), Math.max(1, Math.min(2000, limit))), "size", s.lexicon.size());
    }

    Object lexiconSet(Map<String, Object> b, Map<String, String> q) throws Exception {
        s.lexicon.set(Json.str(b, "word", ""), Json.num(b, "score", 0));
        return Map.of();
    }

    Object lexiconRemove(Map<String, Object> b, Map<String, String> q) throws Exception {
        s.lexicon.remove(Json.str(b, "word", "")); return Map.of();
    }

    Object lexiconReset(Map<String, Object> b, Map<String, String> q) throws Exception { s.lexicon.resetCustom(); return Map.of(); }

    Object dataset(Map<String, Object> b, Map<String, String> q) { return s.datasetInfo(); }

    Object train(Map<String, Object> b, Map<String, String> q) throws Exception { return s.train(Json.str(b, "csv", null)); }

    Object evaluate(Map<String, Object> b, Map<String, String> q) {
        return s.evaluate(Json.num(b, "testFraction", 0.25), (long) Json.num(b, "seed", 1));
    }
}
