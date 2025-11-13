import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Zentrale Konfigurations-Hilfsklasse für Anwendungseinstellungen.
 * Werte werden aus einer externen config.properties gelesen und geschrieben;
 * Klassenpfad-Defaults dienen als Fallback.
 */
public class ConfigUtil {
    private static final String FILE_NAME = "config.properties";

    /** Interner Speicher für Konfigurationswerte. */
    private static Properties props;

    /** Pfad zur externen Konfigurationsdatei. */
    private static Path externalConfigPath = Paths.get(FILE_NAME);

    static {
        load();
    }

    /** Test-Hook: externen Konfigurationspfad für Tests überschreiben und neu laden. */
    public static synchronized void setExternalConfigPathForTest(Path path) {
        if (path != null) {
            externalConfigPath = path;
            load();
        }
    }

    /** Konfiguration aus externer Datei oder Klassenpfad laden und sinnvolle Defaults setzen. */
    public static synchronized void load() {
        Properties p = new Properties();
        boolean loaded = false;

        if (Files.exists(externalConfigPath)) {
            try (FileInputStream fis = new FileInputStream(externalConfigPath.toFile())) {
                p.load(fis);
                loaded = true;
            } catch (Exception ignored) {}
        }
        if (!loaded) {
            try (InputStream is = ConfigUtil.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                if (is != null) {
                    p.load(is);
                    loaded = true;
                }
            } catch (Exception ignored) {}
        }

        if (p.getProperty("ics.path") == null) {
            String defaultPath = "calendar.ics";
            Path testPath = Paths.get(defaultPath);
            try {
                Path parent = testPath.getParent();
                if (parent == null) parent = Paths.get(".");
                if (!Files.isWritable(parent)) {
                    defaultPath = Paths.get(System.getProperty("user.home"), "calendar.ics").toString();
                }
            } catch (Exception ignored) {
                defaultPath = Paths.get(System.getProperty("user.home"), "calendar.ics").toString();
            }
            p.setProperty("ics.path", defaultPath);
        }
        if (p.getProperty("ui.darkMode") == null) {
            p.setProperty("ui.darkMode", "false");
        }
        props = p;
    }

    /** Aktuelle Konfiguration in die externe Datei schreiben. */
    public static synchronized void save() throws Exception {
        Path parent = externalConfigPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(externalConfigPath)) {
            Files.createFile(externalConfigPath);
        }
        try (FileOutputStream fos = new FileOutputStream(externalConfigPath.toFile())) {
            props.store(fos, "Application configuration");
        }
    }

    // Zugriff
    public static Path getIcsPath() {
        return Paths.get(props.getProperty("ics.path", "calendar.ics"));
    }
    public static void setIcsPath(Path path) {
        props.setProperty("ics.path", path.toString());
    }
    public static boolean isDarkMode() {
        return Boolean.parseBoolean(props.getProperty("ui.darkMode", "false"));
    }
    public static void setDarkMode(boolean dark) {
        props.setProperty("ui.darkMode", Boolean.toString(dark));
    }
}
