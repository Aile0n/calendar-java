# Calendar Java (JavaFX + CalendarFX + Web)

A calendar application available in two versions:
1. **Desktop Version**: Built with JavaFX and CalendarFX (requires Java 21)
2. **Web Version**: Modern web interface with Spring Boot (requires Java 17)

Both versions import and export events in iCalendar (.ics) and vCalendar (.vcs) formats. Persistence is ICS-only and shared between versions.

## 🌐 Web Version (NEW!)

Start the web application:
```bash
mvn spring-boot:run
# or
mvn clean package -DskipTests
java -jar target/calendar-java-1.0-SNAPSHOT.jar
```

Access the web interface at: **http://localhost:8080**

See [WEB_README.md](WEB_README.md) for complete web version documentation.

![Calendar Web Interface](https://github.com/user-attachments/assets/d95a2108-4278-4cd1-b996-d8286149544f)

## 🖥️ Desktop Version

The UI is localized in German and provides buttons for creating events, importing/exporting, choosing the ICS file path, and toggling dark mode.

For a beginner-friendly walkthrough of the codebase, see CODE_EXPLANATION.md.
For an end-to-end creation story and build steps in German, see PROJEKT_ERSTELLUNG.md.


## Tech Stack

### Web Version
- Language: Java 17
- Build tool: Apache Maven
- Framework: Spring Boot 3.1.5
- Frontend: HTML5, CSS3, Vanilla JavaScript
- Calendar parsing: ical4j 4.x (ICS) + Biweekly (VCS)
- Testing: JUnit Jupiter (JUnit 5)

### Desktop Version
- Language: Java 21
- Build tool: Apache Maven
- UI: JavaFX 22 (controls, FXML)
- Calendar UI: CalendarFX 12
- Calendar parsing: ical4j 4.x (ICS) + Biweekly (VCS)
- Testing: JUnit Jupiter (JUnit 5)
- Packaging: Maven Shade Plugin (fat JAR)

See `pom.xml` for exact versions and plugins.


## Requirements

### Web Version
- JDK 17+
- Maven 3.9+
- Modern web browser

### Desktop Version
- JDK 21+
- Maven 3.9+
- OS with JavaFX support (Windows/macOS/Linux)

Notes about JavaFX:
- This project depends on JavaFX via Maven. The provided launcher class `org.example.Main` avoids the common "JavaFX runtime components are missing" message when launching.
- The shaded JAR is configured to set `Main-Class: org.example.Main`. If running the JAR fails on your platform due to native JavaFX modules, prefer starting the app from Maven or your IDE. See Troubleshooting below.


## Getting Started

### Web Version (Recommended)
```bash
# Clone
git clone <this-repo-url>
cd calendar-java

# Build and run
mvn spring-boot:run

# Or build JAR and run
mvn clean package -DskipTests
java -jar target/calendar-java-1.0-SNAPSHOT.jar
```

Open browser at: http://localhost:8080

### Desktop Version

**Note:** The desktop version requires Java 21 due to CalendarFX dependency. If you have Java 17, use the web version instead.

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

The application window will open with a CalendarFX view and a toolbar with:
- Einstellungen (gear): choose ICS file path and toggle dark mode
- Neuer Termin: create a new event
- Importieren (ICS/VCS): import events from .ics or .vcs
- Exportieren (ICS/VCS): export current events to .ics or .vcs
- Status indicator (right side): shows save status and current entry count
- Beenden und Speichern (right side): write ICS + quit


## Configuration (ICS-only)
Configuration is managed via a `config.properties` file.

Defaults are provided in `src/main/resources/config.properties`:
```
ics.path=calendar.ics
ui.darkMode=false
```
At runtime, user-facing settings are read and written by `ConfigUtil` from an external `config.properties` located in the current working directory. If absent, defaults from the classpath resource are used and a new external file may be created when saving settings via the UI.

Keys:
- `ics.path`: path to ICS file (default: `calendar.ics` in working dir)
- `ui.darkMode`: `true`/`false`


## Tests
Unit tests are located under `src/test/java`. To run them:
```
mvn test
```
Current tests cover ICS and VCS import/export functionality (`IcsUtilTest`).


## Project Structure
```
calendar-java/
├─ pom.xml                         # Maven build config (Java 21, JavaFX, CalendarFX, Shade, Surefire)
├─ README.md
├─ calendar.ics                    # Example ICS file
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  ├─ org/example/Main.java               # Launcher (non-Application) to start JavaFX reliably
│  │  │  ├─ CalendarFxmlApp.java                # FXML-based Application loader
│  │  │  ├─ CalendarProjektApp.java             # Alternative JavaFX Application (programmatic UI)
│  │  │  ├─ CalendarProjektController.java      # FXML-based controller (CalendarFX integration)
│  │  │  ├─ CalendarEntry.java                  # Domain model
│  │  │  ├─ ConfigUtil.java                     # Loads/saves ICS path + UI options
│  │  │  └─ net/fortuna/.../Frequency.java      # Shim for CalendarFX legacy API compatibility
│  │  └─ resources/
│  │     ├─ calendar_view.fxml                  # FXML UI (toolbar + embedded CalendarFX view)
│  │     ├─ META-INF/MANIFEST.MF                # Manifest setting Main-Class: org.example.Main
│  │     └─ config.properties                   # Default config (ics.path, ui.darkMode)
│  └─ test/
│     └─ java/
│        └─ IcsUtilTest.java                    # Unit tests for ICS/VCS import/export
└─ target/                         # Maven build output (classes, test-classes, shaded JAR)
```


## Useful Maven Commands
- Build + test:
  - `mvn clean package`
- Run tests only:
  - `mvn test`

There is no dedicated Maven JavaFX run plugin configured. Consider adding `javafx-maven-plugin` or `exec-maven-plugin` for `mvn javafx:run`/`mvn exec:java` if desired.


## Troubleshooting
- If launching the shaded JAR shows a JavaFX error on your platform:
  - Prefer running from your IDE or ensure your JavaFX native libraries are present.
  - Consider adding platform-specific JavaFX classifiers (e.g., `org.openjfx:javafx-controls:22.0.1:win`) or a JavaFX Maven run plugin.
- ICS shows no events:
  - Check that the `ics.path` file exists or add events via the UI (the app will create the file on first save if possible).


## License
This project is licensed under the MIT License. See the LICENSE file for details.

For the licenses of third‑party dependencies used by this project, see THIRD-PARTY-NOTICES.md.

## Acknowledgements
- CalendarFX (https://dlsc.com/products/calendarfx/)
- OpenJFX (https://openjfx.io/)
- ical4j (https://github.com/ical4j/ical4j)
