// src/main/java/CalendarEntryDAO.java
import java.sql.Connection;
import java.sql.PreparedStatement;

public class CalendarEntryDAO {
    public void save(CalendarEntry entry) throws Exception {
        String sql = "INSERT INTO entries (title, description, start, end) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getDescription());
            stmt.setString(3, entry.getStart().toString());
            stmt.setString(4, entry.getEnd().toString());
            stmt.executeUpdate();
        }
    }
}