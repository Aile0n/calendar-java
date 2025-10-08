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
        if (rawPath == null || rawPath.isBlank()) {
            Path calendarPath = locateInitialCalendar();
            props.setProperty(KEY_ICS_PATH, calendarPath.toString());
        } else {
            Path calendarPath = Paths.get(rawPath.trim()).toAbsolutePath().normalize();
            props.setProperty(KEY_ICS_PATH, calendarPath.toString());
        }

        if (props.getProperty(KEY_DARK_MODE) == null) {
            props.setProperty(KEY_DARK_MODE, "false");
        }
    }

    private static Path locateInitialCalendar() {
        Path workingDirCandidate = Paths.get("calendar.ics").toAbsolutePath().normalize();
        if (Files.exists(workingDirCandidate) || isWritableLocation(workingDirCandidate)) {
            return workingDirCandidate;
        }

        Path homeCandidate = determineUserHomeCalendar();
        if (Files.exists(homeCandidate) || isWritableLocation(homeCandidate)) {
            return homeCandidate;
        }

        Path appCandidate = determineApplicationDirectory().resolve("calendar.ics").toAbsolutePath().normalize();
        if (Files.exists(appCandidate)) {
            return appCandidate;
        }

        // Fall back to working directory even if we cannot determine writability.
        return workingDirCandidate;
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

    private static Path determineUserHomeCalendar() {
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            return Paths.get(userHome).resolve("calendar.ics").toAbsolutePath().normalize();
        }
        return Paths.get("calendar.ics").toAbsolutePath().normalize();
    }

    private static boolean isWritableLocation(Path target) {
        try {
            Path parent = target.getParent();
            if (parent == null) {
                parent = Paths.get("").toAbsolutePath();
            }
            Path probe = parent;
            while (probe != null && !Files.exists(probe)) {
                probe = probe.getParent();
            }
            if (probe == null) {
                probe = Paths.get("").toAbsolutePath();
            }
            if (Files.exists(target)) {
                return Files.isWritable(target);
            }
            return Files.isWritable(probe);
        } catch (Exception ignored) {
            return false;
        }
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
        try {
            ensureCalendarFileExists(path);
            return path;
        } catch (RuntimeException ex) {
            Path fallback = findWritableFallback(path);
            if (fallback.equals(path)) {
                throw ex;
            }
            ensureCalendarFileExists(fallback);
            props.setProperty(KEY_ICS_PATH, fallback.toString());
            try {
                save();
            } catch (Exception ignored) {
                // Best effort – failure to save shouldn't hide the original path problem.
            }
            return fallback;
        }
    }

    private static Path findWritableFallback(Path current) {
        Path working = Paths.get("calendar.ics").toAbsolutePath().normalize();
        if (!working.equals(current) && isWritableLocation(working)) {
            return working;
        }
        Path home = determineUserHomeCalendar();
        if (!home.equals(current) && isWritableLocation(home)) {
            return home;
        }
        Path app = determineApplicationDirectory().resolve("calendar.ics").toAbsolutePath().normalize();
        if (!app.equals(current) && isWritableLocation(app)) {
            return app;
        }
        return current;
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
