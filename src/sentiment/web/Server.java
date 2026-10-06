package sentiment.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import sentiment.service.AnalysisService;
import sentiment.util.Json;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Plain Java HTTP server (no framework). Wires URLs to ApiHandlers and serves the web/ folder. */
public final class Server {
    private final HttpServer http;
    private final AnalysisService service;

    public Server(AnalysisService service, int port) throws IOException {
        this.service = service;
        http = HttpServer.create(new InetSocketAddress(port), 0);
        ApiHandlers api = new ApiHandlers(service);

        route("/api/analyze", api::analyze);
        route("/api/batch", api::batch);
        route("/api/upload", api::upload);
        route("/api/feed/next", api::feedNext);
        route("/api/stats", api::stats);
        route("/api/status", api::status);
        route("/api/history", api::history);
        route("/api/history/delete", api::historyDelete);
        route("/api/history/clear", api::historyClear);
        route("/api/lexicon", api::lexiconList);
        route("/api/lexicon/set", api::lexiconSet);
        route("/api/lexicon/remove", api::lexiconRemove);
        route("/api/lexicon/reset", api::lexiconReset);
        route("/api/dataset", api::dataset);
        route("/api/train", api::train);
        route("/api/evaluate", api::evaluate);

        http.createContext("/", new StaticHandler());
        http.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
    }

    public interface ApiFn { Object handle(Map<String, Object> body, Map<String, String> query) throws Exception; }

    private void route(String path, ApiFn fn) {
        http.createContext(path, ex -> {
            try {
                Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
                Map<String, Object> body = new LinkedHashMap<>();
                if ("POST".equals(ex.getRequestMethod())) {
                    String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    if (!raw.isBlank()) body = Json.asObj(Json.parse(raw));
                }
                Object result = fn.handle(body, q);
                send(ex, 200, Json.write(Map.of("ok", true, "data", result == null ? Map.of() : result)));
            } catch (IllegalArgumentException e) {
                send(ex, 400, Json.write(Map.of("ok", false, "error", String.valueOf(e.getMessage()))));
            } catch (Exception e) {
                send(ex, 500, Json.write(Map.of("ok", false, "error", "Server error: " + e)));
            }
        });
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        if (raw == null) return m;
        for (String part : raw.split("&")) {
            int i = part.indexOf('=');
            try {
                if (i < 0) m.put(URLDecoder.decode(part, "UTF-8"), "");
                else m.put(URLDecoder.decode(part.substring(0, i), "UTF-8"), URLDecoder.decode(part.substring(i + 1), "UTF-8"));
            } catch (Exception ignored) { }
        }
        return m;
    }

    private static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    public void start() { http.start(); }

    public void stop() { http.stop(0); }
}
