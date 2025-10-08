# Code Explanation (Beginner Friendly)

This document explains the main parts of the Calendar Java project and how data flows through the app. It’s written for starters who want a quick mental model without diving into every implementation detail.

## Big Picture
- UI: JavaFX + CalendarFX provide the window, buttons, and calendar view.
- Data: Events (CalendarEntry) are either stored in an ICS file or in an SQLite database.
- Import/Export: ICS (via ical4j) and basic VCS are supported.
- Config: A `config.properties` file controls storage mode and paths.

The app starts via a small launcher (org.example.Main) that ensures JavaFX can be started reliably from the shaded JAR.

## Key Classes and Files

- org.example.Main
  - A tiny launcher class that invokes JavaFX’s Application launch indirectly. This avoids common JavaFX startup errors when running a JAR. It launches `CalendarProjektApp`.

- CalendarProjektApp (JavaFX Application)
  - A programmatic JavaFX application that builds the UI in code (toolbar + CalendarFX view). It shows how to:
    - Load entries, depending on the storage mode.
    - Create new events.
    - Import/export ICS/VCS files.
    - Open a small settings dialog to switch between ICS and DB storage and choose the ICS path.
  - Note: The project also contains an FXML-based UI (CalendarProjektController + calendar_view.fxml). You can use either approach; the launcher currently starts `CalendarProjektApp`.

- CalendarProjektController (FXML Controller)
  - An alternative UI that wires buttons defined in `src/main/resources/calendar_view.fxml`.
  - Connects UI actions (new, import, export, settings, subscribe) to logic.
  - Uses the same data model and utilities as `CalendarProjektApp`.

- CalendarEntry (domain model)
  - Represents a calendar event: id (optional), title, description, start/end times, optional recurrence rule, reminder minutes, and category.

- CalendarEntryDAO (database access)
  - CRUD operations for SQLite: save, findAll, update, delete.
  - Uses `DatabaseUtil` for the connection.

- DatabaseUtil (database helper)
  - Reads the DB URL, initializes the database schema if needed, and provides a `getConnection()` method used by the DAO.

- IcsUtil (import/export helper)
  - ICS: Uses ical4j to parse and generate `.ics` files with full RFC 5545 compliance (including UID generation).
  - VCS: Contains a minimal reader/writer for vCalendar 1.0 (`.vcs`).
  - Also maps recurrence, categories, and reminders (VALARM) to/from `CalendarEntry` when possible.
  - Includes comprehensive validation and error handling for malformed files.

- ConfigUtil (configuration)
  - Reads/writes user settings, with preference for an external `config.properties` in the working directory (falls back to the classpath default if missing).
  - Keys: `storage.mode=ICS|DB`, `ics.path=calendar.ics`, `db.url=jdbc:sqlite:calendar.db`, and a few optional UI/feed settings.

## Data Flow
1. Startup
   - `org.example.Main` calls `Application.launch` for `CalendarProjektApp`.
   - `CalendarProjektApp` builds the UI and loads entries depending on `ConfigUtil.getStorageMode()`.

2. Loading Data
   - ICS mode: `IcsUtil.importIcs` reads from the configured path (`ConfigUtil.getIcsPath()`), producing `List<CalendarEntry>`.
   - DB mode: `CalendarEntryDAO.findAll()` queries SQLite and maps rows to `CalendarEntry` objects.
   - The resulting entries are shown in the CalendarFX view.

3. Creating/Editing
   - The UI shows a simple dialog to create a new event (title, dates).
   - In DB mode it saves via `CalendarEntryDAO.save`.
   - In ICS mode, entries live in memory, and you can export them back to an ICS file.

4. Import/Export
   - Import: `IcsUtil.importAuto` detects `.vcs` vs `.ics` and returns entries.
   - Export: `IcsUtil.exportIcs` or `IcsUtil.exportVcs` writes a list of entries to a file.

5. Configuration
   - `ConfigUtil` prefers an external `config.properties` in the working dir for user changes.
   - When the settings dialog is confirmed, `ConfigUtil.save()` writes the file.

## Where to Start Reading the Code
- If you want to understand startup and UI flow: open `org/example/Main.java` then `CalendarProjektApp.java`.
- If you want to understand persistence: open `CalendarEntry.java`, `DatabaseUtil.java`, and `CalendarEntryDAO.java`.
- If you want to understand ICS/VCS import/export: open `IcsUtil.java` and the unit test `src/test/java/IcsUtilTest.java`.

## Tips for Beginners
- Search for TODO comments and method-level Javadoc to find extension points.
- Start by running `mvn clean package` and then `java -jar target/<shaded-jar>.jar`.
- Change storage mode in the app and observe how the source of truth switches between DB and ICS.

## Glossary
- JavaFX: Java’s UI framework for desktop apps.
- CalendarFX: A UI library for calendar views and interactions.
- ICS (iCalendar): A standard calendar file format used by many tools.
- VCS (vCalendar): An older calendar file format similar to ICS.
- DAO: Data Access Object; a class that encapsulates database operations.
