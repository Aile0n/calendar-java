import java.sql.Connection;
import java.sql.SQLException;

/**
 * @deprecated Database access has been replaced by ICS-based persistence. This
 * utility remains as a thin wrapper for legacy code that still expects it.
 */
@Deprecated(forRemoval = false)
public final class DatabaseUtil {

    private static final DbStorage STORAGE = new DbStorage(ConfigUtil.getDbUrl());

    private DatabaseUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return STORAGE.openConnection();
    }
}
