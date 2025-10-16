import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Datenmodell-Klasse für einen Kalendereintrag.
 *
 * Diese Klasse repräsentiert einen einzelnen Termin im Kalender.
 * Sie speichert alle wichtigen Informationen wie Titel, Beschreibung,
 * Start- und Endzeit sowie optionale Features wie Erinnerungen und Kategorien.
 *
 * Stell dir das wie eine "Schablone" oder "Bauplan" für Termine vor:
 * Jeder Termin, den du im Kalender anlegst, wird als CalendarEntry-Objekt gespeichert.
 */
public class CalendarEntry {
    // --- Eigenschaften (Felder) eines Kalendereintrags ---

    /** Die eindeutige ID des Eintrags (kann null sein, wird automatisch vergeben) */
    private Integer id;

    /** Der Titel/Name des Termins (z.B. "Zahnarzttermin" oder "Meeting") */
    private String title;

    /** Eine ausführlichere Beschreibung des Termins (optional) */
    private String description;

    /** Wann der Termin beginnt (Datum + Uhrzeit) */
    private LocalDateTime start;

    /** Wann der Termin endet (Datum + Uhrzeit) */
    private LocalDateTime end;

    // --- Optionale Features ---

    /** Wie viele Minuten vor dem Termin soll eine Erinnerung erscheinen? (z.B. 15 = 15 Minuten vorher) */
    private Integer reminderMinutesBefore;

    /** In welche Kategorie gehört der Termin? (z.B. "Arbeit", "Privat", "Sport") */
    private String category;

    // --- Konstruktoren (verschiedene Wege, einen CalendarEntry zu erstellen) ---

    /**
     * Leerer Konstruktor - erstellt einen leeren Termin.
     * Wird z.B. verwendet, wenn die Daten später gefüllt werden.
     */
    public CalendarEntry() {}

    /**
     * Vollständiger Konstruktor mit allen Hauptfeldern.
     *
     * @param id Die eindeutige ID (kann null sein)
     * @param title Der Titel des Termins
     * @param description Die Beschreibung des Termins
     * @param start Startzeitpunkt
     * @param end Endzeitpunkt
     */
    public CalendarEntry(Integer id, String title, String description, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
    }

    /**
     * Vereinfachter Konstruktor ohne ID (wird häufiger verwendet).
     * Die ID wird später automatisch vergeben.
     *
     * @param title Der Titel des Termins
     * @param description Die Beschreibung des Termins
     * @param start Startzeitpunkt
     * @param end Endzeitpunkt
     */
    public CalendarEntry(String title, String description, LocalDateTime start, LocalDateTime end) {
        this(null, title, description, start, end); // Ruft den anderen Konstruktor auf, mit id=null
    }

    // --- Getter und Setter (Methoden zum Lesen und Setzen der Eigenschaften) ---
    // In Java ist es üblich, private Felder über diese Methoden zu lesen/ändern

    /** Gibt die ID zurück */
    public Integer getId() { return id; }
    /** Setzt eine neue ID */
    public void setId(Integer id) { this.id = id; }

    /** Gibt den Titel zurück */
    public String getTitle() { return title; }
    /** Setzt einen neuen Titel */
    public void setTitle(String title) { this.title = title; }

    /** Gibt die Beschreibung zurück */
    public String getDescription() { return description; }
    /** Setzt eine neue Beschreibung */
    public void setDescription(String description) { this.description = description; }

    /** Gibt den Startzeitpunkt zurück */
    public LocalDateTime getStart() { return start; }
    /** Setzt einen neuen Startzeitpunkt */
    public void setStart(LocalDateTime start) { this.start = start; }

    /** Gibt den Endzeitpunkt zurück */
    public LocalDateTime getEnd() { return end; }
    /** Setzt einen neuen Endzeitpunkt */
    public void setEnd(LocalDateTime end) { this.end = end; }

    /** Gibt die Erinnerungszeit (in Minuten vor dem Termin) zurück */
    public Integer getReminderMinutesBefore() { return reminderMinutesBefore; }
    /** Setzt eine neue Erinnerungszeit */
    public void setReminderMinutesBefore(Integer reminderMinutesBefore) { this.reminderMinutesBefore = reminderMinutesBefore; }

    /** Gibt die Kategorie zurück */
    public String getCategory() { return category; }
    /** Setzt eine neue Kategorie */
    public void setCategory(String category) { this.category = category; }

    /**
     * Erstellt eine lesbare Text-Darstellung des Termins.
     * Wird z.B. verwendet, wenn man System.out.println(termin) aufruft.
     *
     * @return Text wie "CalendarEntry{id=1, title='Meeting', start=2025-10-10T14:00, end=2025-10-10T15:00}"
     */
    @Override
    public String toString() {
        return "CalendarEntry{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }

    /**
     * Vergleicht zwei CalendarEntry-Objekte auf Gleichheit.
     * Zwei Termine sind gleich, wenn sie die gleiche ID haben.
     *
     * @param o Das Objekt zum Vergleichen
     * @return true wenn beide die gleiche ID haben, sonst false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Sind es genau das gleiche Objekt?
        if (o == null || getClass() != o.getClass()) return false; // Ist o null oder ein anderer Typ?
        CalendarEntry that = (CalendarEntry) o;
        return Objects.equals(id, that.id); // Vergleiche die IDs
    }

    /**
     * Berechnet einen Hash-Wert für diesen Termin.
     * Wird von Java-Collections (z.B. HashSet, HashMap) verwendet.
     *
     * @return Ein eindeutiger Zahlenwert basierend auf der ID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
