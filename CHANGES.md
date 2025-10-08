# Changes Made - Persistent ICS Storage Implementation

## Summary
This update simplifies the calendar application to use only ICS file storage, making entries persistent across application restarts. Database functionality has been removed from the UI and moved to a separate class for potential future use.

## Changes Made

### 1. Persistent Storage
- **ICS-only storage**: Removed database selection from the UI
- **Automatic persistence**: All entries are now automatically saved to `calendar.ics` in the working directory
- **Config persistence**: Configuration file (`config.properties`) is automatically created and persisted in the working directory
- **Works on Windows**: ICS file storage works reliably across all platforms including Windows

### 2. UI Simplification

#### Settings Dialog
- **Before**: Had radio buttons to select between "ICS" and "Database" storage modes
- **After**: Only shows ICS file path and dark mode settings
- Removed storage mode selection completely
- Simplified grid layout from 3 rows to 2 rows

#### New Entry Dialog
- **Removed fields**:
  - Category selection (Allgemein, Arbeit, Privat, Familie, Sonstiges)
  - Recurrence/repeat options (Täglich, Wöchentlich, Monatlich)
  - Reminder minutes before event
- **Kept fields**:
  - Title (required)
  - Description (optional)
  - Start date and time
  - End date and time

### 3. Code Cleanup

#### CalendarProjektApp.java
- Removed all database mode checks
- Simplified `loadFromDatabase()` to only load from ICS
- Simplified `doImport()` to only save to ICS
- Simplified `doExport()` to only read from current entries
- Removed settings dialog complexity (removed ToggleGroup, RadioButtons for storage mode)

#### CalendarProjektController.java
- Removed all database mode checks
- Removed category calendar management
- Removed recurrence rule support
- Removed reminder scheduling
- Removed feed subscription functionality
- Simplified `reloadData()` to only use ICS
- Simplified `populateCalendar()` to basic entry display
- Removed unused methods:
  - `buildRRule()`
  - `scheduleReminders()`
  - `checkReminders()`
  - `onSubscribe()`
  - `restoreFeedsFromConfig()`
  - `subscribeToFeed()`
  - `refreshFeed()`
  - `extractCalendarName()`
  - `getOrCreateCalendar()`

#### DatabaseUtil.java
- Marked as `@Deprecated`
- Removed automatic schema initialization
- No longer throws exceptions if DB is not configured
- Kept minimal functionality for backward compatibility

#### DbStorage.java (NEW)
- Created new class to contain all database-related code
- Includes both connection management and DAO operations
- Silently handles missing DB configuration
- Kept for potential future use

#### ConfigUtil.java
- Removed feed-related configuration methods
- Simplified to focus on ICS path and UI preferences

### 4. Configuration Updates

#### src/main/resources/config.properties
- **Before**:
  ```properties
  db.url=jdbc:sqlite:calendar.db
  storage.mode=ICS
  ics.path=calendar.ics
  ui.darkMode=false
  feeds.urls=
  feeds.refreshMinutes=60
  ```
- **After**:
  ```properties
  # Calendar Java Configuration
  # Storage mode is always ICS (database support has been removed)
  storage.mode=ICS
  ics.path=calendar.ics
  ui.darkMode=false
  ```

### 5. Documentation Updates

#### README.md
- Updated description to reflect ICS-only storage
- Removed references to database storage and switching between modes
- Added "Data Persistence" section explaining how entries are saved
- Updated Configuration section to remove database-related settings
- Updated Project Structure to reflect new/changed files
- Updated Troubleshooting to remove database-related issues
- Removed mention of advanced features (recurrence, categories, reminders) from test coverage

### 6. Files Modified
- `src/main/java/CalendarProjektApp.java` - Simplified to ICS-only
- `src/main/java/CalendarProjektController.java` - Simplified to ICS-only
- `src/main/java/DatabaseUtil.java` - Deprecated, minimal functionality
- `src/main/java/DbStorage.java` - NEW - Contains unused DB code
- `src/main/resources/config.properties` - Simplified configuration
- `README.md` - Updated documentation

## Testing
- All existing unit tests pass (30 tests, 0 failures)
- Build succeeds: `mvn clean package`
- JAR file created successfully (32MB)
- ICS import/export functionality intact
- Configuration persistence working

## How Persistence Works
1. **On Startup**: 
   - Application loads `config.properties` from working directory (or creates it from defaults)
   - Reads ICS file path from config (default: `calendar.ics`)
   - Loads all entries from ICS file if it exists

2. **When Creating/Importing Entries**:
   - New entry is added to in-memory list
   - List is immediately saved to ICS file via `IcsUtil.exportIcs()`
   - ICS file is persisted in working directory

3. **On Restart**:
   - Application automatically loads entries from ICS file
   - All previously created entries are restored

## Benefits
1. **Simplicity**: Single storage mechanism, easier to understand and maintain
2. **Reliability**: File-based storage works consistently across all platforms
3. **Portability**: ICS files can be easily backed up, shared, or moved
4. **Windows Compatibility**: No SQLite database issues on Windows
5. **Clean Code**: Removed ~400 lines of unused/complex code
6. **Future Ready**: Database code preserved in DbStorage.java for potential future use

## What Was Removed
- Database storage mode selection
- Automatic database creation
- Category support
- Recurrence/repeat functionality
- Reminder notifications
- ICS feed subscriptions
- Feed refresh timers

## What Was Kept
- ICS import/export (both .ics and .vcs formats)
- Event creation (title, description, start/end date/time)
- Dark mode support
- All existing tests
- CalendarFX integration
- German localization
