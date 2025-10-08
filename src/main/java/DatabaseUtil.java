import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.io.InputStream;

/**
 * Dienstprogramm für die Datenbankanbindung (SQLite).
 * Lädt die Verbindungskonfiguration aus config.properties, initialisiert das Schema
 * und stellt Verbindungen für DAO-Operationen bereit.
 */
public class DatabaseUtil {
    private static String URL;

    static {
        try {
            URL = ConfigUtil.getDbUrl();
            if (URL == null || URL.isBlank()) {
                throw new RuntimeException("db.url is missing in config.properties");
            }
            // Initialize schema
            initSchema();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DB config", e);
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
            throw new RuntimeException("Failed to initialize schema", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
