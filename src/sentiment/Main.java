package sentiment;

import sentiment.service.AnalysisService;
import sentiment.web.Server;

/**
 * Sentiment Analysis Web Application - entry point.
 * Run:  java Main.java [port]      (default port 8080)
 */
public final class Main {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        AnalysisService service = new AnalysisService();
        Server server = new Server(service, port);
        server.start();
        String url = "http://localhost:" + port;
        System.out.println("Sentiment Analysis app running at " + url + "  (Ctrl+C to stop)");
        try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); }
        catch (Throwable ignored) { System.out.println("Open " + url + " in your browser."); }
    }
}
