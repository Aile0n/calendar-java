import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

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
}
