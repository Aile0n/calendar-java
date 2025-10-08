import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Zentrale Konfigurationsverwaltung für die Anwendung.
 * Liest und schreibt Einstellungen wie Speicher-Modus (ICS/DB) und Pfade
 * aus/zu einer Properties-Datei (config.properties).
 */
public class ConfigUtil {
    public enum StorageMode { ICS, DB }

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
        if (p.getProperty("storage.mode") == null) {
            p.setProperty("storage.mode", "ICS");
        }
        if (p.getProperty("ics.path") == null) {
            p.setProperty("ics.path", "calendar.ics");
        }
        if (p.getProperty("ui.darkMode") == null) {
            p.setProperty("ui.darkMode", "false");
        }
        if (p.getProperty("feeds.urls") == null) {
            p.setProperty("feeds.urls", "");
        }
        if (p.getProperty("feeds.refreshMinutes") == null) {
            p.setProperty("feeds.refreshMinutes", "60");
        }
        // keep backward compatibility: ignore legacy calendar.style if present
        props = p;
    }

    public static synchronized void save() throws Exception {
        if (!Files.exists(externalConfigPath)) {
            Files.createFile(externalConfigPath);
        }
        try (FileOutputStream fos = new FileOutputStream(externalConfigPath.toFile())) {
            props.store(fos, "Application configuration");
        }
    }

    public static StorageMode getStorageMode() {
        String mode = props.getProperty("storage.mode", "ICS").trim().toUpperCase();
        try {
            return StorageMode.valueOf(mode);
        } catch (Exception e) {
            return StorageMode.ICS;
        }
    }

    public static void setStorageMode(StorageMode mode) {
        props.setProperty("storage.mode", mode.name());
    }

    public static Path getIcsPath() {
        return Paths.get(props.getProperty("ics.path", "calendar.ics"));
    }

    public static void setIcsPath(Path path) {
        props.setProperty("ics.path", path.toString());
    }

    public static String getDbUrl() {
        return props.getProperty("db.url");
    }

    public static boolean isDarkMode() {
        return Boolean.parseBoolean(props.getProperty("ui.darkMode", "false"));
    }

    public static void setDarkMode(boolean dark) {
        props.setProperty("ui.darkMode", Boolean.toString(dark));
    }

    // ---- Feed subscription settings ----
    public static List<String> getFeedUrls() {
        String raw = props.getProperty("feeds.urls", "");
        if (raw.isBlank()) return new ArrayList<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public static void setFeedUrls(List<String> urls) {
        String joined = urls == null ? "" : urls.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
        props.setProperty("feeds.urls", joined);
    }

    public static int getFeedRefreshMinutes() {
        try {
            return Integer.parseInt(props.getProperty("feeds.refreshMinutes", "60").trim());
        } catch (Exception e) {
            return 60;
        }
    }

    public static void setFeedRefreshMinutes(int minutes) {
        if (minutes <= 0) minutes = 60;
        props.setProperty("feeds.refreshMinutes", Integer.toString(minutes));
    }
}
