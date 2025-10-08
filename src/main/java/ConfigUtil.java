import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Central configuration helper.
 *
 * <p>The application stores its settings in {@code config.properties} next to the
 * working directory. If the file is missing, a default one will be created
 * automatically. The most important setting is the path to the calendar ICS
 * file which will also be created automatically when necessary.</p>
 */
public final class ConfigUtil {
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String KEY_ICS_PATH = "ics.path";
    private static final String KEY_DARK_MODE = "ui.darkMode";

    private static final Properties props = new Properties();
    private static final Path configPath = Paths.get(CONFIG_FILE_NAME).toAbsolutePath();

    static {
        load();
    }

    private ConfigUtil() {}

    public static synchronized void load() {
        props.clear();
        boolean loadedFromFile = false;
        if (Files.exists(configPath)) {
            try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
                props.load(fis);
                loadedFromFile = true;
            } catch (Exception ignored) {
                // Fallback to defaults below.
            }
        } else {
            try (InputStream is = ConfigUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
                if (is != null) {
                    props.load(is);
                }
            } catch (Exception ignored) {
                // Fallback to defaults below.
            }
        }

        ensureDefaults();

        if (!loadedFromFile) {
            try {
                save();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize configuration", e);
            }
        }
    }

    private static void ensureDefaults() {
        String rawPath = props.getProperty(KEY_ICS_PATH);
        Path calendarPath;
        if (rawPath == null || rawPath.isBlank()) {
            calendarPath = locateInitialCalendar();
            props.setProperty(KEY_ICS_PATH, calendarPath.toString());
        } else {
            calendarPath = Paths.get(rawPath.trim()).toAbsolutePath().normalize();
            props.setProperty(KEY_ICS_PATH, calendarPath.toString());
        }

        if (props.getProperty(KEY_DARK_MODE) == null) {
            props.setProperty(KEY_DARK_MODE, "false");
        }

        ensureCalendarFileExists(calendarPath);
    }

    private static Path locateInitialCalendar() {
        Path base = determineApplicationDirectory();
        Path candidate = base.resolve("calendar.ics");
        if (Files.exists(candidate)) {
            return candidate.toAbsolutePath().normalize();
        }
        Path workingDirCandidate = Paths.get("calendar.ics").toAbsolutePath().normalize();
        if (Files.exists(workingDirCandidate)) {
            return workingDirCandidate;
        }
        return candidate.toAbsolutePath().normalize();
    }

    private static Path determineApplicationDirectory() {
        try {
            var codeSource = ConfigUtil.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                Path location = Paths.get(codeSource.getLocation().toURI());
                if (Files.isDirectory(location)) {
                    return location.toAbsolutePath();
                }
                Path parent = location.getParent();
                if (parent != null) {
                    return parent.toAbsolutePath();
                }
            }
        } catch (Exception ignored) {
        }
        return Paths.get("").toAbsolutePath();
    }

    private static void ensureCalendarFileExists(Path calendarPath) {
        try {
            if (calendarPath.getParent() != null) {
                Files.createDirectories(calendarPath.getParent());
            }
            if (!Files.exists(calendarPath)) {
                IcsUtil.exportIcs(calendarPath, java.util.List.of());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create calendar file at " + calendarPath, e);
        }
    }

    public static synchronized void save() throws Exception {
        if (configPath.getParent() != null) {
            Files.createDirectories(configPath.getParent());
        }
        try (FileOutputStream fos = new FileOutputStream(configPath.toFile())) {
            props.store(fos, "Application configuration");
        }
    }

    public static synchronized Path getIcsPath() {
        return Paths.get(props.getProperty(KEY_ICS_PATH)).toAbsolutePath().normalize();
    }

    public static synchronized void setIcsPath(Path path) throws Exception {
        Path normalized = path.toAbsolutePath().normalize();
        ensureCalendarFileExists(normalized);
        props.setProperty(KEY_ICS_PATH, normalized.toString());
    }

    public static synchronized Path ensureCalendarFile() {
        Path path = getIcsPath();
        ensureCalendarFileExists(path);
        return path;
    }

    public static synchronized boolean isDarkMode() {
        return Boolean.parseBoolean(props.getProperty(KEY_DARK_MODE, "false"));
    }

    public static synchronized void setDarkMode(boolean darkMode) {
        props.setProperty(KEY_DARK_MODE, Boolean.toString(darkMode));
    }

    public static synchronized String getDbUrl() {
        return props.getProperty("db.url");
    }
}
