package sentiment.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** File and resource helpers. Defaults are read from disk (if present) or from inside the jar. */
public final class Res {
    private Res() { }

    /** Folder where the app saves history, custom lexicon and custom training data. */
    public static final String DATA_DIR = System.getProperty("sentiment.data", "data");

    /** Reads a bundled file such as "data/lexicon.csv" or "web/app.js" (disk first, then classpath). */
    public static String readDefault(String rel) throws IOException {
        File f = new File(rel);
        if (f.isFile()) return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        try (InputStream in = Res.class.getResourceAsStream("/" + rel)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static File dataFile(String name) {
        File d = new File(DATA_DIR);
        d.mkdirs();
        return new File(d, name);
    }
}
