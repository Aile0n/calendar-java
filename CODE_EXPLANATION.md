# Code Explanation (Beginner Friendly)

This document explains the main parts of the Calendar Java project and how data flows through the app. It's written for starters who want a quick mental model without diving into every implementation detail.

## Big Picture
- UI: JavaFX + CalendarFX provide the window, buttons, and calendar view.
- Data: Events (CalendarEntry) are stored in an ICS file (ICS-only design).
- Import/Export: ICS (via ical4j) and basic VCS are supported.
- Config: A `config.properties` file controls the ICS file path and UI options.

The app starts via a small launcher (org.example.Main) that ensures JavaFX can be started reliably from the shaded JAR.

## Key Classes and Files

- org.example.Main
  - A tiny launcher class that invokes JavaFX's Application launch indirectly. This avoids common JavaFX startup errors when running a JAR. It launches the JavaFX app (`CalendarFxmlApp` or `CalendarProjektApp`).

- CalendarFxmlApp (JavaFX Application)
  - Loads the FXML layout (`calendar_view.fxml`) and thus activates the controller `CalendarProjektController`.

- CalendarProjektApp (JavaFX Application)
  - A programmatic JavaFX application that builds the UI in code (toolbar + CalendarFX view). It shows how to:
    - Load entries from the configured ICS file.
    - Create new events.
    - Import/export ICS/VCS files.
    - Open a small settings dialog to choose the ICS path and toggle dark mode.
  - The toolbar is organized with action buttons on the left (Settings, New Event, Import, Export) and control buttons on the right (Status indicator, Exit and Save).
  - A flexible spacer ensures the status label and exit button always appear on the right edge.
  - The project also contains the FXML-based UI (CalendarProjektController + calendar_view.fxml). You can use either approach.

- CalendarProjektController (FXML Controller)
  - Wires the buttons defined in `src/main/resources/calendar_view.fxml` to the application logic.
  - Connects UI actions (new, import, export, settings) to logic and updates the CalendarFX view.

- CalendarEntry (domain model)
  - Represents a calendar event: id (optional), title, description, start/end times, optional reminder minutes, and category.

- IcsUtil (import/export helper)
  - ICS: Uses ical4j to parse and generate `.ics` files.
  - VCS: Contains a minimal reader/writer for vCalendar 1.0 (`.vcs`).
  - Maps categories and reminders (VALARM) to/from `CalendarEntry` when possible.

- ConfigUtil (configuration)
  - Reads/writes user settings, with preference for an external `config.properties` in the working directory (falls back to the classpath default if missing).
  - Keys: `ics.path=calendar.ics`, `ui.darkMode=false`.

## Data Flow
1. Startup
   - `org.example.Main` calls `Application.launch` for either `CalendarFxmlApp` (FXML) or `CalendarProjektApp` (programmatic UI).

2. Loading Data (ICS-only)
   - `IcsUtil.importIcs` reads from the configured path (`ConfigUtil.getIcsPath()`), producing `List<CalendarEntry>`.
   - The resulting entries are shown in the CalendarFX view.

3. Creating/Editing
   - The UI shows a dialog to create a new event (title, dates).
   - Changes done in the CalendarFX UI are auto-saved to the ICS file.
   - A status indicator on the right side of the toolbar shows the save state and entry count.

4. Import/Export
   - Import: `IcsUtil.importAuto` detects `.vcs` vs `.ics` and returns entries.
   - Export: `IcsUtil.exportIcs` or `IcsUtil.exportVcs` writes a list of entries to a file of your choice.

5. Configuration
   - `ConfigUtil` prefers an external `config.properties` in the working dir for user changes.
   - When the settings dialog is confirmed, `ConfigUtil.save()` writes the file.

## Where to Start Reading the Code
- To understand startup and UI flow: open `org/example/Main.java` then `CalendarFxmlApp.java` and `CalendarProjektController.java`.
- To understand the alternative programmatic UI: open `CalendarProjektApp.java`.
- To understand ICS/VCS import/export: open `IcsUtil.java` and the unit test `src/test/java/IcsUtilTest.java`.

## Tips for Beginners
- Search for TODO comments and method-level Javadoc to find extension points.
- Start by running `mvn clean package` and then `java -jar target/<shaded-jar>.jar`.
- Try importing a small `.ics` file to see entries appear.

## Glossary
- JavaFX: Java's UI framework for desktop apps.
- CalendarFX: A UI library for calendar views and interactions.
- ICS (iCalendar): A standard calendar file format used by many tools.
- VCS (vCalendar): An older calendar file format similar to ICS.
