import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Zentrale Konfigurationsverwaltung für die Anwendung.
 *
 * Diese Klasse kümmert sich um alle Einstellungen des Programms,
 * z.B. wo die Kalenderdatei gespeichert wird und ob der Dunkelmodus aktiv ist.
 *
 * Die Einstellungen werden in einer "config.properties" Datei gespeichert,
 * die wie eine einfache Text-Datei mit Schlüssel=Wert Paaren aussieht.
 * Beispiel:
 *   ics.path=calendar.ics
 *   ui.darkMode=false
 */
public class ConfigUtil {
    /** Name der Konfigurationsdatei */
    private static final String FILE_NAME = "config.properties";

    /**
     * Hier werden alle Einstellungen gespeichert (Schlüssel-Wert-Paare).
     * Properties ist eine spezielle Java-Klasse zum Verwalten von Konfigurationsdaten.
     */
    private static Properties props;

    /** Der Pfad zur Konfigurationsdatei auf dem Computer */
    private static Path externalConfigPath = Paths.get(FILE_NAME);

    // Statischer Block - wird einmal beim ersten Laden der Klasse ausgeführt
    static {
        load(); // Lade die Konfiguration beim Programmstart
    }

    /**
     * Lädt die Konfiguration aus der Datei.
     *
     * Suchstrategie:
     * 1. Prüfe, ob es eine config.properties im aktuellen Ordner gibt
     * 2. Falls nicht, suche im JAR nach einer Standard-Konfiguration
     * 3. Falls auch das nicht klappt, nutze eingebaute Standardwerte
     */
    public static synchronized void load() {
        // Erstelle ein neues Properties-Objekt zum Speichern der Einstellungen
        Properties p = new Properties();
        boolean loaded = false;

        // Versuch 1: Externe Datei im aktuellen Verzeichnis
        if (Files.exists(externalConfigPath)) {
            try (FileInputStream fis = new FileInputStream(externalConfigPath.toFile())) {
                p.load(fis); // Lese alle Einstellungen aus der Datei
                loaded = true;
            } catch (Exception ignored) {
                // Falls ein Fehler auftritt, ignorieren wir ihn und versuchen die nächste Option
            }
        }

        // Versuch 2: Fallback zur eingebauten Konfiguration im JAR
        if (!loaded) {
            try (InputStream is = ConfigUtil.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                if (is != null) {
                    p.load(is);
                    loaded = true;
                }
            } catch (Exception ignored) {}
        }

        // --- Setze Standardwerte, falls bestimmte Einstellungen fehlen ---

        // Wenn kein ICS-Pfad definiert ist, lege einen Standard fest
        if (p.getProperty("ics.path") == null) {
            // Standardmäßig speichern wir die Kalenderdatei im aktuellen Ordner
            String defaultPath = "calendar.ics";
            Path testPath = Paths.get(defaultPath);

            try {
                // Prüfe, ob wir in den aktuellen Ordner schreiben können
                Path parent = testPath.getParent();
                if (parent == null) {
                    parent = Paths.get("."); // . bedeutet "aktueller Ordner"
                }
                if (!Files.isWritable(parent)) {
                    // Falls nicht beschreibbar, nutze das Home-Verzeichnis des Benutzers
                    defaultPath = Paths.get(System.getProperty("user.home"), "calendar.ics").toString();
                }
            } catch (Exception ignored) {
                // Sicherheits-Fallback: Im Zweifelsfall das Home-Verzeichnis nutzen
                defaultPath = Paths.get(System.getProperty("user.home"), "calendar.ics").toString();
            }

            p.setProperty("ics.path", defaultPath);
        }

        // Wenn keine Dunkelmodus-Einstellung vorhanden ist, setze false (heller Modus)
        if (p.getProperty("ui.darkMode") == null) {
            p.setProperty("ui.darkMode", "false");
        }

        // Speichere die Eigenschaften in der statischen Variable
        props = p;
    }

    /**
     * Speichert die aktuellen Einstellungen in die config.properties Datei.
     *
     * Diese Methode wird aufgerufen, wenn der Benutzer in den Einstellungen
     * etwas ändert und auf "OK" klickt.
     *
     * @throws Exception Falls beim Schreiben der Datei ein Fehler auftritt
     */
    public static synchronized void save() throws Exception {
        // Stelle sicher, dass der Ordner für die Konfigurationsdatei existiert
        Path parent = externalConfigPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent); // Erstelle alle nötigen Ordner
        }

        // Erstelle die Datei, falls sie noch nicht existiert
        if (!Files.exists(externalConfigPath)) {
            Files.createFile(externalConfigPath);
        }

        // Schreibe alle Einstellungen in die Datei
        try (FileOutputStream fos = new FileOutputStream(externalConfigPath.toFile())) {
            props.store(fos, "Application configuration"); // Der Kommentar erscheint in der Datei
        }
    }

    // --- Getter und Setter für spezifische Einstellungen ---

    /**
     * Gibt den Pfad zur ICS-Kalenderdatei zurück.
     *
     * @return Der Dateipfad als Path-Objekt (z.B. "C:\Users\...\calendar.ics")
     */
    public static Path getIcsPath() {
        return Paths.get(props.getProperty("ics.path", "calendar.ics"));
    }

    /**
     * Setzt einen neuen Pfad für die ICS-Kalenderdatei.
     *
     * @param path Der neue Dateipfad
     */
    public static void setIcsPath(Path path) {
        props.setProperty("ics.path", path.toString());
    }

    /**
     * Prüft, ob der Dunkelmodus aktiviert ist.
     *
     * @return true wenn Dunkelmodus an ist, false wenn heller Modus aktiv ist
     */
    public static boolean isDarkMode() {
        return Boolean.parseBoolean(props.getProperty("ui.darkMode", "false"));
    }

    /**
     * Aktiviert oder deaktiviert den Dunkelmodus.
     *
     * @param dark true für Dunkelmodus, false für hellen Modus
     */
    public static void setDarkMode(boolean dark) {
        props.setProperty("ui.darkMode", Boolean.toString(dark));
    }
}
