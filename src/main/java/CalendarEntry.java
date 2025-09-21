import java.time.LocalDateTime;

public class CalendarEntry {
    private int id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;

    // Constructors

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    // Setters and other methods if needed
}
