import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Zentrale Konfigurationsverwaltung für die Anwendung.
 * Liest und schreibt Einstellungen wie Pfade und Darstellung
 * aus/zu einer Properties-Datei (config.properties).
 */
public class ConfigUtil {
    private static final String FILE_NAME = "config.properties"; // prefer working directory

    private static Properties props;
    private static Path externalConfigPath = Paths.get(FILE_NAME);

    static {
        load();
    }

    public static synchronized void load() {
        Properties p = new Properties();
        boolean loaded = false;
        // Try external file first
        if (Files.exists(externalConfigPath)) {
            try (FileInputStream fis = new FileInputStream(externalConfigPath.toFile())) {
                p.load(fis);
                loaded = true;
            } catch (Exception ignored) {}
        }
        // Fallback to classpath resource
        if (!loaded) {
            try (InputStream is = ConfigUtil.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                if (is != null) {
                    p.load(is);
                    loaded = true;
                }
            } catch (Exception ignored) {}
        }
        // Defaults
        if (p.getProperty("ics.path") == null) {
            // Default to working directory, or user home if working directory is not writable
            String defaultPath = "calendar.ics";
            Path testPath = Paths.get(defaultPath);
            try {
                // Test if we can write to the working directory
                Path parent = testPath.getParent();
                if (parent == null) {
                    parent = Paths.get(".");
                }
                if (!Files.isWritable(parent)) {
                    // Fallback to user home directory
                    defaultPath = Paths.get(System.getProperty("user.home"), "calendar.ics").toString();
                }
            } catch (Exception ignored) {
                // If we can't determine writability, try user home as safe fallback
                defaultPath = Paths.get(System.getProperty("user.home"), "calendar.ics").toString();
            }
            p.setProperty("ics.path", defaultPath);
        }
        if (p.getProperty("ui.darkMode") == null) {
            p.setProperty("ui.darkMode", "false");
        }
        props = p;
    }

    public static synchronized void save() throws Exception {
        // Ensure parent directory exists
        Path parent = externalConfigPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        // Create config file if it doesn't exist
        if (!Files.exists(externalConfigPath)) {
            Files.createFile(externalConfigPath);
        }
        try (FileOutputStream fos = new FileOutputStream(externalConfigPath.toFile())) {
            props.store(fos, "Application configuration");
        }
    }

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
