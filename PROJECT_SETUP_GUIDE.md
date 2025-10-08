# Project Setup Guide: Calendar Java (JavaFX + CalendarFX)

This guide explains how this project was created from scratch in IntelliJ IDEA, how it integrates with CalendarFX and other libraries, the class structure, and how to build a runnable JAR file.

## Table of Contents
1. [Project Creation in IntelliJ IDEA](#1-project-creation-in-intellij-idea)
2. [Maven Configuration and Dependencies](#2-maven-configuration-and-dependencies)
3. [CalendarFX Integration](#3-calendarfx-integration)
4. [Project Architecture and Classes](#4-project-architecture-and-classes)
5. [Building and Running](#5-building-and-running)
6. [Creating a JAR File](#6-creating-a-jar-file)

---

## 1. Project Creation in IntelliJ IDEA

### Initial Setup

1. **Create New Maven Project**
   - Open IntelliJ IDEA
   - Click **File** → **New** → **Project**
   - Select **Maven Archetype** (or **Maven** for newer IntelliJ versions)
   - Configure project settings:
     - **Name**: `calendar-java`
     - **Location**: Choose your workspace directory
     - **JDK**: Select Java 21 or newer (this project uses Java 21)
     - **GroupId**: `org.example`
     - **ArtifactId**: `calendar-java`
     - **Version**: `1.0-SNAPSHOT`

2. **Project Structure Setup**
   - IntelliJ automatically creates the standard Maven directory structure:
     ```
     calendar-java/
     ├── src/
     │   ├── main/
     │   │   ├── java/          # Java source files
     │   │   └── resources/     # Configuration files, FXML, etc.
     │   └── test/
     │       └── java/          # Test files
     └── pom.xml                # Maven configuration
     ```

3. **IntelliJ IDEA Configuration**
   - The `.idea` folder contains IntelliJ-specific settings:
     - `misc.xml`: Project SDK and Maven configuration
     - `runConfigurations/`: Application run configurations
     - `vcs.xml`: Version control settings

### Run Configuration

The project includes a pre-configured run configuration (`.idea/runConfigurations/java_calendar.xml`):
- **Name**: `java-calendar`
- **Main Class**: `org.example.Main`
- **JRE**: Java 21+ (configured as `temurin-24` in the example)
- **Build**: Automatically builds before running

---

## 2. Maven Configuration and Dependencies

### pom.xml Overview

The `pom.xml` file is the heart of the Maven project, defining all dependencies and build configurations.

#### Project Properties
```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <javafx.version>22.0.1</javafx.version>
    <calendarfx.version>12.0.1</calendarfx.version>
</properties>
```

#### Core Dependencies

1. **CalendarFX** - Calendar UI Component
   ```xml
   <dependency>
       <groupId>com.calendarfx</groupId>
       <artifactId>view</artifactId>
       <version>12.0.1</version>
   </dependency>
   ```
   - Provides the calendar view and interaction components
   - Built on top of JavaFX for rich calendar visualization

2. **JavaFX Controls and FXML**
   ```xml
   <dependency>
       <groupId>org.openjfx</groupId>
       <artifactId>javafx-controls</artifactId>
       <version>22.0.1</version>
   </dependency>
   <dependency>
       <groupId>org.openjfx</groupId>
       <artifactId>javafx-fxml</artifactId>
       <version>22.0.1</version>
   </dependency>
   ```
   - JavaFX is the UI framework for building desktop applications
   - `javafx-controls`: UI components (buttons, dialogs, etc.)
   - `javafx-fxml`: XML-based UI layout support

3. **SQLite JDBC** - Database Storage
   ```xml
   <dependency>
       <groupId>org.xerial</groupId>
       <artifactId>sqlite-jdbc</artifactId>
       <version>3.42.0.0</version>
   </dependency>
   ```
   - Lightweight embedded database
   - No separate database server required

4. **ical4j** - iCalendar (ICS) Support
   ```xml
   <dependency>
       <groupId>org.mnode.ical4j</groupId>
       <artifactId>ical4j</artifactId>
       <version>3.2.7</version>
   </dependency>
   ```
   - Parse and generate .ics (iCalendar) files
   - Industry-standard calendar format

5. **JUnit Jupiter** - Testing Framework
   ```xml
   <dependency>
       <groupId>org.junit.jupiter</groupId>
       <artifactId>junit-jupiter</artifactId>
       <version>5.10.2</version>
       <scope>test</scope>
   </dependency>
   ```
   - Modern testing framework (JUnit 5)

#### Build Plugins

1. **Maven Surefire Plugin** - Test Runner
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-surefire-plugin</artifactId>
       <version>3.2.5</version>
   </plugin>
   ```
   - Executes unit tests during the build

2. **Maven Shade Plugin** - JAR Packaging
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-shade-plugin</artifactId>
       <version>3.5.1</version>
       <configuration>
           <transformers>
               <transformer implementation="...ManifestResourceTransformer">
                   <mainClass>org.example.Main</mainClass>
               </transformer>
           </transformers>
       </configuration>
   </plugin>
   ```
   - Creates a "fat JAR" (uber JAR) with all dependencies included
   - Sets the main class for easy execution

---

## 3. CalendarFX Integration

### How CalendarFX Works

CalendarFX is a comprehensive calendar UI library built on JavaFX. Here's how it's integrated:

#### Basic CalendarFX Setup

1. **Create a CalendarView**
   ```java
   CalendarView calendarView = new CalendarView();
   ```

2. **Create a Calendar Source**
   ```java
   CalendarSource source = new CalendarSource("Meine Kalender");
   ```

3. **Create and Add a Calendar**
   ```java
   Calendar fxCalendar = new Calendar("Termine");
   source.getCalendars().add(fxCalendar);
   ```

4. **Connect to the View**
   ```java
   calendarView.getCalendarSources().add(source);
   ```

#### Adding Events (Entries)

CalendarFX uses `Entry<T>` objects to represent calendar events:

```java
Entry<String> entry = new Entry<>("Meeting Title");
entry.setLocation("Conference Room");
entry.setInterval(startZonedDateTime, endZonedDateTime);
fxCalendar.addEntry(entry);
```

#### Localization (German UI)

The project includes German translations for CalendarFX's built-in controls:

```java
private void localizeNode(Node node) {
    if (node instanceof ButtonBase btn) {
        String text = btn.getText();
        switch (text) {
            case "Today" -> btn.setText("Heute");
            case "Day" -> btn.setText("Tag");
            case "Week" -> btn.setText("Woche");
            // ... more translations
        }
    }
    // Recursively localize child nodes
}
```

### Why CalendarFX?

- **Rich Calendar Views**: Day, week, month, and year views
- **Drag and Drop**: Users can move events easily
- **Built-in Search**: Find events quickly
- **Customizable**: Styling with CSS
- **Well-Maintained**: Active development and documentation

---

## 4. Project Architecture and Classes

### Package Structure

```
src/main/java/
├── org/example/
│   └── Main.java                    # Application launcher
├── CalendarProjektApp.java          # JavaFX Application (programmatic UI)
├── CalendarProjektController.java   # FXML Controller
├── CalendarEntry.java               # Domain model
├── CalendarEntryDAO.java            # Database access layer
├── DatabaseUtil.java                # Database connection utility
├── IcsUtil.java                     # ICS/VCS import/export
├── ConfigUtil.java                  # Configuration management
└── net/fortuna/ical4j/transform/recurrence/
    └── Frequency.java               # Compatibility shim for CalendarFX
```

### Core Classes Explained

#### 1. **org.example.Main** - Application Launcher

**Purpose**: Reliable JavaFX startup from a JAR file

```java
public final class Main {
    public static void main(String[] args) {
        Class<?> appClass = Class.forName("CalendarProjektApp");
        Class<?> fxApp = Class.forName("javafx.application.Application");
        fxApp.getMethod("launch", Class.class, String[].class)
            .invoke(null, appClass, args);
    }
}
```

**Why it exists**: When a JAR's main class directly extends `javafx.application.Application`, the Java launcher often reports "JavaFX runtime components are missing." This wrapper class avoids that issue by launching JavaFX programmatically.

#### 2. **CalendarProjektApp** - Main Application

**Purpose**: The main JavaFX application with programmatic UI

Key responsibilities:
- Initialize the CalendarFX view
- Load calendar entries from storage (ICS or database)
- Provide toolbar with buttons for:
  - Settings (storage mode, file paths)
  - Creating new events
  - Import/Export (ICS/VCS)
  - Subscribe to ICS feeds
- Localize the UI to German

Structure:
```java
public class CalendarProjektApp extends Application {
    private final CalendarEntryDAO dao = new CalendarEntryDAO();
    private final Calendar fxCalendar = new Calendar("Termine");
    
    @Override
    public void start(Stage primaryStage) {
        // Build UI with CalendarFX
        // Setup toolbar
        // Load data
        // Show window
    }
}
```

#### 3. **CalendarProjektController** - FXML Controller

**Purpose**: Alternative FXML-based UI controller

- Connects FXML UI elements to Java code
- Works with `src/main/resources/calendar_view.fxml`
- Implements the same functionality as CalendarProjektApp
- Follows MVC pattern (Model-View-Controller)

#### 4. **CalendarEntry** - Domain Model

**Purpose**: Represents a calendar event

```java
public class CalendarEntry {
    private Integer id;              // Database ID (optional)
    private String title;            // Event title
    private String description;      // Event details
    private LocalDateTime start;     // Start time
    private LocalDateTime end;       // End time
    private String recurrenceRule;   // RRULE for recurring events
    private Integer reminderMinutes; // Minutes before event to remind
    private String category;         // Event category/tag
    
    // Constructors, getters, setters...
}
```

#### 5. **CalendarEntryDAO** - Data Access Object

**Purpose**: Database operations for calendar entries

Methods:
- `save(CalendarEntry)` - Insert or update an entry
- `findAll()` - Retrieve all entries from database
- `update(CalendarEntry)` - Update existing entry
- `delete(int id)` - Remove an entry

Uses JDBC with SQLite:
```java
public void save(CalendarEntry entry) {
    String sql = "INSERT INTO entries (title, description, start, end) VALUES (?, ?, ?, ?)";
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, entry.getTitle());
        // ... set other parameters
        stmt.executeUpdate();
    }
}
```

#### 6. **DatabaseUtil** - Database Helper

**Purpose**: Manage database connection and schema

- Loads `db.url` from `config.properties`
- Creates the SQLite database if it doesn't exist
- Initializes the schema (creates tables):
  ```sql
  CREATE TABLE IF NOT EXISTS entries (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT NOT NULL,
      description TEXT,
      start TEXT NOT NULL,
      end TEXT NOT NULL
  )
  ```
- Provides `getConnection()` for database access

#### 7. **IcsUtil** - Calendar File Import/Export

**Purpose**: Handle ICS (iCalendar) and VCS (vCalendar) formats

Key methods:
- `importIcs(String path)` - Parse .ics file to CalendarEntry list
- `exportIcs(String path, List<CalendarEntry>)` - Write entries to .ics
- `importVcs(String path)` - Parse legacy .vcs format
- `exportVcs(String path, List<CalendarEntry>)` - Write to .vcs
- `importAuto(String path)` - Auto-detect format

Uses ical4j library for ICS:
```java
CalendarBuilder builder = new CalendarBuilder();
Calendar calendar = builder.build(new FileInputStream(icsPath));
// Parse components...
```

#### 8. **ConfigUtil** - Configuration Management

**Purpose**: Read/write application settings

Managed settings:
- `storage.mode` - "ICS" or "DB"
- `ics.path` - Path to ICS file
- `db.url` - SQLite JDBC URL

Preference order:
1. External `config.properties` in working directory (user-editable)
2. Classpath resource `config.properties` (defaults)

```java
public static String getStorageMode() {
    return getProperty("storage.mode", "ICS");
}

public static void save(Properties props) {
    // Write to external config.properties
}
```

### Data Flow

1. **Startup**
   - `Main.main()` launches `CalendarProjektApp`
   - App reads configuration from `ConfigUtil`
   - Determines storage mode (ICS or DB)

2. **Loading Data**
   - **ICS Mode**: `IcsUtil.importIcs()` reads file → `List<CalendarEntry>`
   - **DB Mode**: `CalendarEntryDAO.findAll()` queries database → `List<CalendarEntry>`
   - Entries converted to CalendarFX `Entry` objects and displayed

3. **Creating Events**
   - User fills dialog (title, description, dates)
   - Creates `CalendarEntry` object
   - **ICS Mode**: Added to in-memory list (export to save)
   - **DB Mode**: Saved via `CalendarEntryDAO.save()`

4. **Import/Export**
   - User selects file via `FileChooser`
   - `IcsUtil.importAuto()` detects format and imports
   - `IcsUtil.exportIcs()` or `.exportVcs()` writes to file

---

## 5. Building and Running

### Building with Maven

#### From Command Line
```bash
# Clean and build the project
mvn clean package

# Run tests only
mvn test

# Skip tests and just build
mvn clean package -DskipTests
```

#### From IntelliJ IDEA
1. Open **Maven** tool window (View → Tool Windows → Maven)
2. Expand **Lifecycle**
3. Double-click **package** to build
4. Or use the toolbar: **Run** → **Edit Configurations** → Select "java-calendar" → **Run**

### Running the Application

#### Option 1: From IntelliJ
- Click the **Run** button (green play icon)
- Or press **Shift + F10**
- Or right-click `Main.java` → **Run 'Main.main()'**

#### Option 2: From Maven (if exec plugin configured)
```bash
mvn javafx:run
# or
mvn exec:java
```
*Note: Currently not configured in this project*

#### Option 3: From Built JAR
```bash
java -jar target/calendar-java-1.0-SNAPSHOT.jar
```

### Testing

Run unit tests:
```bash
mvn test
```

Current test coverage:
- `IcsUtilTest`: Tests ICS/VCS import and export round-trip

---

## 6. Creating a JAR File

### Maven Shade Plugin

The project uses Maven Shade Plugin to create an executable "fat JAR" (uber JAR) that includes all dependencies.

#### Configuration in pom.xml

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <createDependencyReducedPom>false</createDependencyReducedPom>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>org.example.Main</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### What Happens During Build

1. **Compile Phase**
   - Java source files compiled to `.class` files
   - Resources copied to `target/classes/`

2. **Test Phase**
   - Test classes compiled
   - Unit tests executed
   - Build fails if tests fail

3. **Package Phase**
   - Regular JAR created: `calendar-java-1.0-SNAPSHOT.jar`
   - Shade plugin runs:
     - Unpacks all dependency JARs
     - Merges all classes into a single JAR
     - Creates manifest with `Main-Class: org.example.Main`
     - Produces: `calendar-java-1.0-SNAPSHOT.jar` (fat JAR)

#### Manifest File

The JAR includes a manifest at `META-INF/MANIFEST.MF`:
```
Manifest-Version: 1.0
Main-Class: org.example.Main
```

This allows running the JAR with just: `java -jar calendar-java-1.0-SNAPSHOT.jar`

### Building the JAR

```bash
# Build the JAR
mvn clean package

# Output location
ls -lh target/calendar-java-1.0-SNAPSHOT.jar
```

### Running the JAR

```bash
# Navigate to the target directory
cd target

# Run the JAR
java -jar calendar-java-1.0-SNAPSHOT.jar
```

### Distribution

The fat JAR is fully self-contained and can be distributed as a single file:
- All dependencies included (CalendarFX, JavaFX, SQLite, ical4j)
- No additional installation required (except Java 21+)
- Cross-platform (Windows, macOS, Linux)

#### Platform Considerations

JavaFX includes native libraries for different platforms. The shaded JAR includes these, but some platforms may require platform-specific classifiers:

```xml
<!-- Example: Windows-specific JavaFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>22.0.1</version>
    <classifier>win</classifier>
</dependency>
```

For truly cross-platform JARs, consider using jpackage (Java 14+) to create native installers.

---

## Summary

This project demonstrates:

1. **IntelliJ IDEA Maven Project**: Standard Java project setup with IDE integration
2. **Modern Java Stack**: Java 21, JavaFX 22, Maven build system
3. **Calendar UI**: CalendarFX integration for rich calendar functionality
4. **Multiple Storage Options**: SQLite database or ICS file storage
5. **Standard Formats**: ICS/VCS import/export using ical4j
6. **Clean Architecture**: Separation of UI, business logic, and data access
7. **Executable JAR**: Single-file distribution with Maven Shade Plugin

### Key Takeaways

- **Maven manages all dependencies** - no manual JAR downloads
- **CalendarFX provides the calendar UI** - built on JavaFX
- **Launcher pattern solves JavaFX JAR issues** - `org.example.Main` wrapper
- **DAO pattern separates database logic** - clean code organization
- **Configuration externalized** - `config.properties` for user settings
- **Fat JAR simplifies distribution** - all dependencies in one file

### Next Steps

- Explore `CODE_EXPLANATION.md` for detailed code walkthrough
- Check `README.md` for usage instructions
- Review `pom.xml` for complete dependency list
- Run the application and experiment with features
- Modify the code and rebuild to see changes

---

*For more detailed code explanations, see [CODE_EXPLANATION.md](CODE_EXPLANATION.md)*
