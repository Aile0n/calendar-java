# Code Explanation (Beginner Friendly)

This document explains the main parts of the Calendar Java project and how data flows through the app. It’s written for starters who want a quick mental model without diving into every implementation detail.

## Big Picture
- UI: JavaFX + CalendarFX provide the window, buttons, and calendar view.
- Data: Events (CalendarEntry) are stored in a single ICS file. Legacy database helpers are kept in a separate utility for future reuse.
- Import/Export: ICS (via ical4j) and basic VCS are supported.
- Config: A `config.properties` file controls the ICS file path and dark mode.

The app starts via a small launcher (org.example.Main) that ensures JavaFX can be started reliably from the shaded JAR.

## Key Classes and Files

- org.example.Main
  - A tiny launcher class that invokes JavaFX’s Application launch indirectly. This avoids common JavaFX startup errors when running a JAR. It launches `CalendarProjektApp`.

- CalendarProjektApp (JavaFX Application)
  - A programmatic JavaFX application that builds the UI in code (toolbar + CalendarFX view). It shows how to:
    - Load entries from the configured ICS file.
    - Auto-save on changes and offer a manual “Beenden & Speichern”.
    - Import/export ICS/VCS files.
    - Open a small settings dialog to select the ICS path and toggle dark mode.
  - Note: The project also contains an FXML-based UI (CalendarProjektController + calendar_view.fxml). You can use either approach; the launcher currently starts `CalendarProjektApp`.

- CalendarProjektController (FXML Controller)
  - An alternative UI that wires buttons defined in `src/main/resources/calendar_view.fxml`.
  - Connects UI actions (new, import, export, settings, exit & save) to the same logic used by `CalendarProjektApp`.

- CalendarEntry (domain model)
  - Represents a calendar event: id (optional), title, description, start/end times, optional recurrence rule, reminder minutes, and category.

- CalendarEntryDAO (database access)
  - Legacy CRUD wrapper kept for compatibility; now delegates to `DbStorage`.

- DatabaseUtil (database helper)
  - Deprecated thin wrapper around `DbStorage` for older code that still calls `DatabaseUtil.getConnection()`.

- DbStorage (database helper)
  - Encapsulates all SQL access logic. Not used by the UI anymore, but retained for potential future reuse.

- IcsUtil (import/export helper)
  - ICS: Uses ical4j to parse and generate `.ics` files with full RFC 5545 compliance (including UID generation).
  - VCS: Contains a minimal reader/writer for vCalendar 1.0 (`.vcs`).
  - Also maps recurrence, categories, and reminders (VALARM) to/from `CalendarEntry` when possible.
  - Includes comprehensive validation and error handling for malformed files.

- ConfigUtil (configuration)
  - Reads/writes user settings, preferring an external `config.properties` in the working directory.
  - Keys: `ics.path` and `ui.darkMode`. Automatically creates `config.properties` and an empty `calendar.ics` when missing.

## Data Flow
1. Startup
   - `org.example.Main` calls `Application.launch` for `CalendarProjektApp`.
   - `CalendarProjektApp` builds the UI and loads entries from `ConfigUtil.ensureCalendarFile()`.

2. Loading Data
   - `IcsUtil.importIcs` reads from the configured path, producing `List<CalendarEntry>`.
   - The resulting entries populate CalendarFX calendars (one per category, if present).

3. Creating/Editing
   - The UI shows a simple dialog to create a new event (title, start/end).
   - Saved entries are appended to the in-memory list and immediately written back to the ICS file.

4. Import/Export
   - Import: `IcsUtil.importAuto` detects `.vcs` vs `.ics`, merges into the in-memory list, and auto-saves.
   - Export: `IcsUtil.exportIcs` or `IcsUtil.exportVcs` writes the current entries to a user-selected file.

5. Configuration
   - `ConfigUtil` prefers an external `config.properties` in the working dir for user changes.
   - When the settings dialog is confirmed, `ConfigUtil.save()` writes the file and a new calendar path is created if necessary.

## Where to Start Reading the Code
- If you want to understand startup and UI flow: open `org/example/Main.java` then `CalendarProjektApp.java`.
- If you want to understand persistence: open `CalendarEntry.java`, `ConfigUtil.java`, and `IcsUtil.java` (for ICS). `DbStorage` contains the optional database helper.
- If you want to understand ICS/VCS import/export: open `IcsUtil.java` and the unit test `src/test/java/IcsUtilTest.java`.

## Tips for Beginners
- Search for TODO comments and method-level Javadoc to find extension points.
- Start by running `mvn clean package` and then `java -jar target/<shaded-jar>.jar`.
- Explore the settings dialog to see how the ICS path and dark mode are persisted.

## Glossary
- JavaFX: Java’s UI framework for desktop apps.
- CalendarFX: A UI library for calendar views and interactions.
- ICS (iCalendar): A standard calendar file format used by many tools.
- VCS (vCalendar): An older calendar file format similar to ICS.
- DAO: Data Access Object; a class that encapsulates database operations.
