import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;

/**
 * Dienstprogramm für die Datenbankanbindung (SQLite).
 * DEPRECATED: Database functionality has been moved to DbStorage.java
 * This class is kept for backward compatibility but is no longer actively used.
 */
@Deprecated
public class DatabaseUtil {
    private static String URL;

    static {
        try {
            URL = ConfigUtil.getDbUrl();
            // DO NOT automatically initialize schema - let DbStorage handle it if needed
        } catch (Exception e) {
            System.err.println("[INFO] Database configuration not loaded: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (URL == null || URL.isBlank()) {
            throw new SQLException("Database URL not configured");
        }
        return DriverManager.getConnection(URL);
    }
}
