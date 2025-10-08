import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Database storage implementation for calendar entries.
 * This class is currently unused but kept for potential future use.
 * Contains both connection management and DAO operations.
 */
public class DbStorage {
    
    // Database connection management
    private static String URL;
    
    static {
        try {
            URL = ConfigUtil.getDbUrl();
            if (URL != null && !URL.isBlank()) {
                // Initialize schema only if DB URL is configured
                initSchema();
            }
        } catch (Exception e) {
            // Silently ignore DB initialization errors since DB is not used
            System.err.println("[INFO] Database not configured: " + e.getMessage());
        }
    }
    
    private static void initSchema() {
        String createSql = "CREATE TABLE IF NOT EXISTS entries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "start TEXT NOT NULL, " +
                "end TEXT NOT NULL, " +
                "recurrence_rule TEXT, " +
                "reminder_minutes INTEGER, " +
                "category TEXT" +
                ")";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
            // Try to add missing columns in case of existing DB
            try { stmt.execute("ALTER TABLE entries ADD COLUMN recurrence_rule TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE entries ADD COLUMN reminder_minutes INTEGER"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE entries ADD COLUMN category TEXT"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            System.err.println("[WARNING] Failed to initialize database schema: " + e.getMessage());
        }
    }
    
    public static Connection getConnection() throws SQLException {
        if (URL == null || URL.isBlank()) {
            throw new SQLException("Database URL not configured");
        }
        return DriverManager.getConnection(URL);
    }
    
    // DAO operations
    public CalendarEntry save(CalendarEntry entry) throws SQLException {
        String sql = "INSERT INTO entries (title, description, start, end, recurrence_rule, reminder_minutes, category) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getDescription());
            stmt.setString(3, entry.getStart().toString());
            stmt.setString(4, entry.getEnd().toString());
            stmt.setString(5, entry.getRecurrenceRule());
            if (entry.getReminderMinutesBefore() == null) stmt.setNull(6, java.sql.Types.INTEGER); 
            else stmt.setInt(6, entry.getReminderMinutesBefore());
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
        try (Connection conn = getConnection();
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
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getDescription());
            stmt.setString(3, entry.getStart().toString());
            stmt.setString(4, entry.getEnd().toString());
            stmt.setString(5, entry.getRecurrenceRule());
            if (entry.getReminderMinutesBefore() == null) stmt.setNull(6, java.sql.Types.INTEGER); 
            else stmt.setInt(6, entry.getReminderMinutesBefore());
            stmt.setString(7, entry.getCategory());
            stmt.setInt(8, entry.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM entries WHERE id=?";
        try (Connection conn = getConnection();
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
