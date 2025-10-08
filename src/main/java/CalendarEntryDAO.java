import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Datenzugriffsobjekt (DAO) für Kalendereinträge in der SQLite-Datenbank.
 * Bietet CRUD-Operationen: speichern, laden (alle), aktualisieren und löschen.
 */
public class CalendarEntryDAO {
    public CalendarEntry save(CalendarEntry entry) throws SQLException {
        String sql = "INSERT INTO entries (title, description, start, end, recurrence_rule, reminder_minutes, category) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getDescription());
            stmt.setString(3, entry.getStart().toString());
            stmt.setString(4, entry.getEnd().toString());
            stmt.setString(5, entry.getRecurrenceRule());
            if (entry.getReminderMinutesBefore() == null) stmt.setNull(6, java.sql.Types.INTEGER); else stmt.setInt(6, entry.getReminderMinutesBefore());
            stmt.setString(7, entry.getCategory());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    entry.setId(rs.getInt(1));
                }
            }
        }
        return entry;
    }

    public List<CalendarEntry> findAll() throws SQLException {
        String sql = "SELECT id, title, description, start, end, recurrence_rule, reminder_minutes, category FROM entries ORDER BY start";
        List<CalendarEntry> result = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public void update(CalendarEntry entry) throws SQLException {
        String sql = "UPDATE entries SET title=?, description=?, start=?, end=?, recurrence_rule=?, reminder_minutes=?, category=? WHERE id=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getDescription());
            stmt.setString(3, entry.getStart().toString());
            stmt.setString(4, entry.getEnd().toString());
            stmt.setString(5, entry.getRecurrenceRule());
            if (entry.getReminderMinutesBefore() == null) stmt.setNull(6, java.sql.Types.INTEGER); else stmt.setInt(6, entry.getReminderMinutesBefore());
            stmt.setString(7, entry.getCategory());
            stmt.setInt(8, entry.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM entries WHERE id=?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private CalendarEntry mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        LocalDateTime start = LocalDateTime.parse(rs.getString("start"));
        LocalDateTime end = LocalDateTime.parse(rs.getString("end"));
        CalendarEntry ce = new CalendarEntry(id, title, description, start, end);
        try { ce.setRecurrenceRule(rs.getString("recurrence_rule")); } catch (SQLException ignored) {}
        try { int m = rs.getInt("reminder_minutes"); if (!rs.wasNull()) ce.setReminderMinutesBefore(m); } catch (SQLException ignored) {}
        try { ce.setCategory(rs.getString("category")); } catch (SQLException ignored) {}
        return ce;
    }
}