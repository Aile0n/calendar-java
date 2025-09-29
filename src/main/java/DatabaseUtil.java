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
        try (InputStream input = DatabaseUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                throw new RuntimeException("config.properties not found on classpath");
            }
            prop.load(input);
            URL = prop.getProperty("db.url");
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
        String sql = "CREATE TABLE IF NOT EXISTS entries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "start TEXT NOT NULL, " +
                "end TEXT NOT NULL" +
                ")";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize schema", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
