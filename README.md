# Calendar Java (JavaFX + CalendarFX)

A simple desktop calendar application built with JavaFX and CalendarFX. It can import and export events in iCalendar (.ics) and vCalendar (.vcs) formats and supports two storage modes:

- ICS file storage (default)
- SQLite database storage

The UI is localized in German and provides buttons for creating events, importing/exporting, and switching storage mode in-app.

For a beginner-friendly walkthrough of the codebase, see CODE_EXPLANATION.md.


## Tech Stack
- Language: Java 21
- Build tool: Apache Maven
- UI: JavaFX 22 (controls, FXML)
- Calendar UI: CalendarFX 12
- Storage: SQLite (via sqlite-jdbc)
- Calendar parsing: ical4j 3.x (ICS) + custom minimal VCS support
- Testing: JUnit Jupiter (JUnit 5)
- Packaging: Maven Shade Plugin (fat JAR with `Main-Class: org.example.Main`)

See `pom.xml` for exact versions and plugins.


## Requirements
- JDK 21+
- Maven 3.9+
- OS with JavaFX support (Windows/macOS/Linux)

Notes about JavaFX:
- This project depends on JavaFX via Maven. The provided launcher class `org.example.Main` avoids the common "JavaFX runtime components are missing" message when launching.
- The shaded JAR is configured to set `Main-Class: org.example.Main`. If running the JAR fails on your platform due to native JavaFX modules, prefer starting the app from Maven or your IDE. See Troubleshooting below.


## Getting Started

### Clone
```
git clone <this-repo-url>
cd calendar-java
```

### Build
```
mvn clean package
```
This compiles sources, runs unit tests, and builds a fat JAR in `target/` using the Maven Shade Plugin.

### Run
Choose one of the following:

- From the built shaded JAR (preferred if it works on your platform):
  - Check `target/` for a shaded JAR (often named like `calendar-java-1.0-SNAPSHOT-shaded.jar`).
  - Run it:
    ```
    java -jar target/<the-shaded-jar>.jar
    ```

- From your IDE: Run the class `org.example.Main`.

- From Maven (exec plugin is not configured; use the JAR method above). TODO: Add exec/run plugin if desired.

The application window will open with a CalendarFX view and a toolbar with:
- Einstellungen (gear): choose storage mode (ICS file vs. database) and ICS file path
- Neuer Termin: create a new event
- Importieren (ICS/VCS): import events from .ics or .vcs
- Exportieren (ICS/VCS): export current events to .ics or .vcs
- Abonnieren (ICS-Feed): subscribe to an online calendar feed (URL to .ics) with periodic refresh


## Configuration
Configuration is managed via a `config.properties` file:

- Classpath defaults are provided in `src/main/resources/config.properties`:
  ```properties
  db.url=jdbc:sqlite:calendar.db
  storage.mode=ICS
  ics.path=calendar.ics
  ```
- At runtime, most user-facing settings are read and written by `ConfigUtil` from an external `config.properties` located in the current working directory. If absent, defaults from the classpath resource are used and a new external file may be created when saving settings via the UI.

Keys:
- `storage.mode`: `ICS` or `DB` (default: `ICS`)
- `ics.path`: path to ICS file when in ICS mode (default: `calendar.ics` in working dir)
- `db.url`: JDBC URL for the database. Default is a local SQLite file.

Important notes:
- DatabaseUtil currently reads `db.url` from the classpath `config.properties` and initializes the schema accordingly. Changing the DB URL via the UI is not supported; to change `db.url`, adjust `src/main/resources/config.properties` and rebuild, or ensure your runtime classpath contains an overriding `config.properties`. TODO: Unify DB URL handling with `ConfigUtil` so the UI can edit it.
- The SQLite database file defaults to `calendar.db` in the working directory. The schema is auto-created on first run.


## Tests
Unit tests are located under `src/test/java`. To run them:
```
mvn test
```
Current tests cover ICS and VCS round-trip import/export (`IcsUtilTest`).


## Project Structure
```
calendar-java/
├─ pom.xml                         # Maven build config (Java 21, JavaFX, CalendarFX, Shade, Surefire)
├─ README.md
├─ calendar.db                     # Example SQLite DB file (optional, created at runtime as needed)
├─ calendar.ics                    # Example ICS file
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  ├─ org/example/Main.java               # Launcher (non-Application) to start JavaFX reliably
│  │  │  ├─ CalendarProjektApp.java             # Alternative JavaFX Application (programmatic UI)
│  │  │  ├─ CalendarProjektController.java      # FXML-based controller (CalendarFX integration)
│  │  │  ├─ CalendarEntry.java                  # Domain model
│  │  │  ├─ CalendarEntryDAO.java               # SQLite DAO
│  │  │  ├─ ConfigUtil.java                     # Loads/saves storage mode & ICS path
│  │  │  ├─ DatabaseUtil.java                   # DB connection + schema init (uses db.url)
│  │  │  └─ net/fortuna/.../Frequency.java      # Shim for CalendarFX legacy API compatibility
│  │  └─ resources/
│  │     ├─ calendar_view.fxml                  # FXML UI (toolbar + embedded CalendarFX view)
│  │     ├─ META-INF/MANIFEST.MF                # Manifest setting Main-Class: org.example.Main
│  │     └─ config.properties                   # Default config (db.url, storage.mode, ics.path)
│  └─ test/
│     └─ java/
│        └─ IcsUtilTest.java                    # Unit tests for ICS/VCS import/export
├─ target/                         # Maven build output (classes, test-classes, shaded JAR)
└─ out/artifacts/...               # IDE build output (IntelliJ), may contain a JAR
```


## Useful Maven Commands (Scripts)
- Build + test:
  - `mvn clean package`
- Run tests only:
  - `mvn test`
- Create fat JAR (run as part of `package` via Shade):
  - `mvn package`

There is no dedicated Maven JavaFX run plugin configured. TODO: Add `javafx-maven-plugin` or `exec-maven-plugin` for `mvn javafx:run`/`mvn exec:java` if desired.


## Troubleshooting
- If launching the shaded JAR shows a JavaFX error on your platform:
  - Prefer running from your IDE or ensure your JavaFX native libraries are present.
  - Consider adding platform-specific JavaFX classifiers (e.g., `org.openjfx:javafx-controls:22.0.1:win`) or a JavaFX Maven run plugin. TODO in build.
- If database mode is selected but no data appears:
  - Ensure `db.url` points to a writable SQLite file path (default `jdbc:sqlite:calendar.db`). The schema is created automatically.
- ICS mode shows no events:
  - Check that the `ics.path` file exists or add events via the UI and export.


## License
This project is licensed under the MIT License. See the LICENSE file for details.

For the licenses of third‑party dependencies used by this project, see THIRD-PARTY-NOTICES.md.

## Acknowledgements
- CalendarFX (https://dlsc.com/products/calendarfx/)
- OpenJFX (https://openjfx.io/)
- ical4j (https://github.com/ical4j/ical4j)
- SQLite JDBC (https://github.com/xerial/sqlite-jdbc)
