import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;

public class DatabaseUtil {
    private static String URL;

    static {
        try (InputStream input = DatabaseUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            URL = prop.getProperty("db.url");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DB config", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
