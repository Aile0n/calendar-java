import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domänenmodell, das einen Kalendereintrag mit Titel, Beschreibung,
 * Start/Ende und optionalen Metadaten repräsentiert.
 */
public class CalendarEntry {
    // Kernfelder
    private Integer id;                 // Optionale eindeutige Kennung
    private String title;               // Termin-Titel
    private String description;         // Optionale Beschreibung
    private LocalDateTime start;        // Startdatum/-zeit
    private LocalDateTime end;          // Enddatum/-zeit

    // Optionale Eigenschaften
    private Integer reminderMinutesBefore; // Minuten vor Beginn für Erinnerung
    private String category;               // Logische Kategorie/Label

    /** Standardkonstruktor. */
    public CalendarEntry() {}

    /**
     * Vollständiger Konstruktor.
     */
    public CalendarEntry(Integer id, String title, String description, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
    }

    /**
     * Bequemer Konstruktor ohne ID.
     */
    public CalendarEntry(String title, String description, LocalDateTime start, LocalDateTime end) {
        this(null, title, description, start, end);
    }

    // Zugriffsmethoden
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }

    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }

    public Integer getReminderMinutesBefore() { return reminderMinutesBefore; }
    public void setReminderMinutesBefore(Integer reminderMinutesBefore) { this.reminderMinutesBefore = reminderMinutesBefore; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return "CalendarEntry{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarEntry that = (CalendarEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
