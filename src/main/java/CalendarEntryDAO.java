import java.sql.SQLException;
import java.util.List;

/**
 * Legacy DAO wrapper delegating to {@link DbStorage}. The application no longer
 * uses direct database persistence but keeps this class for compatibility.
 */
@Deprecated(forRemoval = false)
public class CalendarEntryDAO {

    private final DbStorage storage;

    public CalendarEntryDAO() {
        this.storage = new DbStorage(ConfigUtil.getDbUrl());
    }

    public CalendarEntry save(CalendarEntry entry) throws SQLException {
        return storage.insert(entry);
    }

    public List<CalendarEntry> findAll() throws SQLException {
        return storage.loadAll();
    }

    public void update(CalendarEntry entry) throws SQLException {
        storage.update(entry);
    }

    public void delete(int id) throws SQLException {
        storage.delete(id);
    }
}