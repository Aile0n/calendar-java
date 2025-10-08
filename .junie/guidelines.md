# Project Guidelines for Junie

## Project Overview
Calendar Java is a simple desktop calendar application built with JavaFX and CalendarFX. It lets users create, import, and export events in iCalendar (.ics) and vCalendar (.vcs) formats and supports two storage modes:
- ICS file storage (default)
- SQLite database storage

The UI is localized in German and provides a toolbar with actions:
- Einstellungen (gear): choose storage mode (ICS file vs. database) and ICS file path
- Neuer Termin: create a new event
- Importieren (ICS/VCS): import events from .ics or .vcs
- Exportieren (ICS/VCS): export current events to .ics or .vcs
- Abonnieren (ICS-Feed): subscribe to an online .ics URL with periodic refresh (where implemented)

For a beginner-friendly walkthrough of the codebase, see CODE_EXPLANATION.md.

### Tech stack
- Java 21, Maven
- JavaFX 22, CalendarFX 12
- SQLite (sqlite-jdbc)
- ical4j 3.x (ICS parsing) + minimal VCS support
- JUnit 5 for tests
- Maven Shade Plugin for a runnable fat JAR (Main-Class: org.example.Main)

### Key code and entry points
- org.example.Main — launcher to start JavaFX reliably
- CalendarProjektController — FXML-based controller integrating CalendarFX (UI defined in src/main/resources/calendar_view.fxml)
- CalendarProjektApp — alternative JavaFX Application with programmatic UI
- CalendarEntry / CalendarEntryDAO — domain model and SQLite DAO
- ConfigUtil — loads/saves storage mode and ICS path (external config.properties)
- DatabaseUtil — database connection and schema initialization (reads db.url)
- IcsUtil — import/export utilities for ICS and VCS
- Tests: src/test/java/IcsUtilTest.java (round-trip import/export)

### Build, run, and test
- Build (compiles, runs tests, creates shaded JAR):
  - mvn clean package
- Run tests:
  - mvn test
- Run the app (from shaded JAR in target/):
  - java -jar target/<shaded-jar>.jar

### Configuration
Config is handled via config.properties (external file in working directory, with defaults in src/main/resources/config.properties):
- storage.mode: ICS or DB (default: ICS)
- ics.path: path to ICS file when in ICS mode (default: calendar.ics)
- db.url: JDBC URL for the database (default: jdbc:sqlite:calendar.db)

For more details, see the project README.md and CODE_EXPLANATION.md.
