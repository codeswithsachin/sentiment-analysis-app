package sentiment.analyzer;

/** Common interface: any sentiment engine can be plugged into the app. */
public interface Analyzer {
    String id();
    String name();
    Result analyze(String text);
}
