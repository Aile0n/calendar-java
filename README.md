# Calendar Java (JavaFX + CalendarFX)

A simple desktop calendar application built with JavaFX and CalendarFX. It can import and export events in iCalendar (.ics) and vCalendar (.vcs) formats.

- **Storage:** ICS file format (calendar.ics in working directory)
- **UI:** Localized in German
- **Features:** Create events, import/export calendars, persistent storage

For a beginner-friendly walkthrough of the codebase, see CODE_EXPLANATION.md.
For an end-to-end creation story and build steps in German, see PROJEKT_ERSTELLUNG.md.


## Tech Stack
- Language: Java 21
- Build tool: Apache Maven
- UI: JavaFX 22 (controls, FXML)
- Calendar UI: CalendarFX 12
- Storage: ICS file format (persistent across application restarts)
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
On Windows (with Maven installed): double-click `build-jar.cmd`, or run:
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

## Configuration
Configuration is managed via a `config.properties` file:

- Classpath defaults are provided in `src/main/resources/config.properties`:
  ```properties
  storage.mode=ICS
  ics.path=calendar.ics
  ui.darkMode=false
  ```
- At runtime, settings are read and written by `ConfigUtil` from an external `config.properties` located in the current working directory. If absent, defaults from the classpath resource are used and a new external file is created automatically.

Keys:
- `storage.mode`: Always `ICS` (database support has been removed)
- `ics.path`: path to ICS file (default: `calendar.ics` in working directory)
- `ui.darkMode`: Enable/disable dark mode (default: `false`)

**Data Persistence:**
- All calendar entries are automatically saved to the ICS file specified in the configuration
- The ICS file persists across application restarts
- You can manually edit the `ics.path` in settings to use a different location


## Tests
Unit tests are located under `src/test/java`. To run them:
```
mvn test
```
Current tests cover comprehensive ICS and VCS import/export functionality (`IcsUtilTest`):
- Round-trip import/export for both ICS and VCS formats
- Multiple entries in a single file
- Special characters and text escaping
- Edge cases (empty descriptions, null values, same start/end times)
- Error handling for malformed files
- Auto-detection of file formats
- UID generation compliance with RFC 5545


## Project Structure
```
calendar-java/
├─ pom.xml                         # Maven build config (Java 21, JavaFX, CalendarFX, Shade, Surefire)
├─ README.md
├─ calendar.ics                    # ICS file for storing calendar entries (created automatically)
├─ config.properties               # User config file (created automatically in working directory)
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  ├─ org/example/Main.java               # Launcher (non-Application) to start JavaFX reliably
│  │  │  ├─ CalendarProjektApp.java             # Alternative JavaFX Application (programmatic UI)
│  │  │  ├─ CalendarProjektController.java      # FXML-based controller (CalendarFX integration)
│  │  │  ├─ CalendarEntry.java                  # Domain model
│  │  │  ├─ IcsUtil.java                        # ICS/VCS import/export utilities
│  │  │  ├─ ConfigUtil.java                     # Loads/saves configuration (ICS path, dark mode)
│  │  │  ├─ DbStorage.java                      # Database code (unused, kept for future)
│  │  │  ├─ DatabaseUtil.java                   # Deprecated DB utilities
│  │  │  ├─ CalendarEntryDAO.java               # Deprecated DAO (uses DatabaseUtil)
│  │  │  └─ net/fortuna/.../Frequency.java      # Shim for CalendarFX legacy API compatibility
│  │  └─ resources/
│  │     ├─ calendar_view.fxml                  # FXML UI (toolbar + embedded CalendarFX view)
│  │     ├─ dark.css                            # Dark mode stylesheet
│  │     ├─ META-INF/MANIFEST.MF                # Manifest setting Main-Class: org.example.Main
│  │     └─ config.properties                   # Default config (storage.mode, ics.path, ui.darkMode)
│  └─ test/
│     └─ java/
│        ├─ IcsUtilTest.java                    # Unit tests for ICS/VCS import/export
│        ├─ IcsUtilEdgeCasesTest.java           # Edge case tests
│        └─ ConfigUtilTest.java                 # Configuration tests
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
- If no events appear after restart:
  - Check that the `calendar.ics` file exists in the working directory
  - Verify the `ics.path` setting in `config.properties`
  - The application automatically creates and saves to `calendar.ics` when you add events


## License
This project is licensed under the MIT License. See the LICENSE file for details.

For the licenses of third‑party dependencies used by this project, see THIRD-PARTY-NOTICES.md.

## Acknowledgements
- CalendarFX (https://dlsc.com/products/calendarfx/)
- OpenJFX (https://openjfx.io/)
- ical4j (https://github.com/ical4j/ical4j)
- SQLite JDBC (https://github.com/xerial/sqlite-jdbc)
