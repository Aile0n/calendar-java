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
 * 
 * @deprecated Database mode is deprecated. Use ICS file storage instead.
 *             This class is retained for potential future reuse but should not be used in new code.
 */
@Deprecated
public class DatabaseUtil {
    private static String URL;

    static {
        try {
            URL = ConfigUtil.getDbUrl();
            if (URL == null || URL.isBlank()) {
                // Set a default URL but don't initialize schema
                URL = "jdbc:sqlite:calendar.db";
            }
            // Schema initialization removed - database mode is deprecated
        } catch (Exception e) {
            // Silently fail - database mode is deprecated
            URL = "jdbc:sqlite:calendar.db";
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
