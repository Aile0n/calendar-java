# Calendar Java Application - Code Explanation

## Overview
This is a JavaFX-based calendar application that provides a graphical user interface for viewing and managing calendar events. The application is built using Java 17 and leverages the CalendarFX library for the calendar functionality.

## Project Structure

### Maven Configuration (`pom.xml`)
```xml
<!-- Core configuration -->
<groupId>org.example</groupId>
<artifactId>calendar-java</artifactId>
<version>1.0-SNAPSHOT</version>

<!-- Java 17 configuration -->
<maven.compiler.source>17</maven.compiler.source>
<maven.compiler.target>17</maven.compiler.target>

<!-- Key dependencies -->
- CalendarFX (11.12.7): Provides calendar UI components
- JavaFX Controls (17.0.10): Core JavaFX UI framework
```

## Core Components

### 1. SimpleCalendarApp.java - Main Application Class

**Purpose**: Main JavaFX application entry point that creates and displays the calendar interface.

**Key Features**:
- **JavaFX Application**: Extends `Application` class for JavaFX applications
- **Calendar View**: Uses CalendarFX's `CalendarView` component for calendar functionality
- **Custom Toolbar**: Creates a toolbar with settings button
- **German Interface**: Uses German text ("Mein kleiner Kalender", "Einstellungen")

**Code Breakdown**:

```java
public class SimpleCalendarApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Create the main calendar component
        CalendarView calendarView = new CalendarView();
        
        // Create toolbar with settings button
        ToolBar toolBar = new ToolBar();
        Button settingsButton = new Button();
        
        // Icon loading with fallback
        var iconUrl = getClass().getResource("/icons/settings.png");
        if (iconUrl != null) {
            // Load icon from resources
            ImageView iv = new ImageView(new Image(iconUrl.toExternalForm()));
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            settingsButton.setGraphic(iv);
        } else {
            // Fallback to Unicode gear symbol
            settingsButton.setText("⚙");
        }
        
        // Settings dialog functionality
        settingsButton.setOnAction(e -> {
            Stage dialog = new Stage();
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            // ... dialog setup
        });
        
        // Layout setup
        VBox root = new VBox(toolBar, calendarView);
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Mein kleiner Kalender");
        primaryStage.show();
    }
}
```

### 2. CalendarEntry.java - Data Model

**Purpose**: Represents a calendar event/entry with basic properties.

**Properties**:
- `id`: Unique identifier for the entry
- `title`: Event title/name
- `description`: Detailed description of the event
- `start`: Start date and time (`LocalDateTime`)
- `end`: End date and time (`LocalDateTime`)

**Features**:
- Uses modern Java time API (`LocalDateTime`)
- Provides getter methods for data access
- Follows JavaBean convention (getters/setters pattern)

```java
public class CalendarEntry {
    private int id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    
    // Getter methods for accessing properties
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
}
```

### 3. DatabaseUtil.java - Database Connection Management

**Purpose**: Handles database connections and configuration management.

**Key Features**:
- **Configuration Loading**: Reads database settings from `config.properties`
- **Connection Factory**: Provides database connections via `getConnection()`
- **Static Initialization**: Loads configuration when class is first loaded
- **Error Handling**: Throws `RuntimeException` if configuration fails

**Code Analysis**:
```java
public class DatabaseUtil {
    private static String URL;
    
    static {
        // Load configuration at class initialization
        try (InputStream input = DatabaseUtil.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            URL = prop.getProperty("db.url");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DB config", e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
```

### 4. CalendarEntryDAO.java - Data Access Object

**Purpose**: Handles database operations for calendar entries (Data Access Object pattern).

**Functionality**:
- **Save Operation**: Inserts calendar entries into database
- **Prepared Statements**: Uses parameterized queries for security
- **Resource Management**: Implements try-with-resources for automatic cleanup

**Code Structure**:
```java
public class CalendarEntryDAO {
    public void save(CalendarEntry entry) throws Exception {
        String sql = "INSERT INTO entries (title, description, start, end) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters safely
            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getDescription());
            stmt.setString(3, entry.getStart().toString());
            stmt.setString(4, entry.getEnd().toString());
            
            stmt.executeUpdate();
        }
    }
}
```

### 5. Configuration (`config.properties`)

**Purpose**: External configuration for database connection.

```properties
db.url=jdbc:sqlite:calendar.db
# For a server DB, use e.g.:
# db.url=jdbc:mysql://server-address:3306/calendar
```

**Features**:
- SQLite database by default (file-based, no server required)
- Comments show how to configure for MySQL server
- Easy to modify without recompiling code

## Application Architecture

### Design Patterns Used:

1. **MVC (Model-View-Controller)**:
   - **Model**: `CalendarEntry` (data structure)
   - **View**: `SimpleCalendarApp` (JavaFX UI)
   - **Controller**: Implied in event handlers and DAOs

2. **DAO (Data Access Object)**:
   - `CalendarEntryDAO` abstracts database operations
   - Separates data access logic from business logic

3. **Factory Pattern**:
   - `DatabaseUtil.getConnection()` creates database connections
   - Centralizes connection creation logic

4. **Singleton-like Pattern**:
   - `DatabaseUtil` uses static methods and initialization
   - Single point of configuration management

### Technology Stack:

- **JavaFX**: Modern UI framework for desktop applications
- **CalendarFX**: Specialized calendar components and functionality
- **JDBC**: Database connectivity (supports SQLite, MySQL, etc.)
- **Maven**: Build automation and dependency management

### Key Dependencies:

```xml
<!-- CalendarFX for calendar UI components -->
<dependency>
    <groupId>com.calendarfx</groupId>
    <artifactId>view</artifactId>
    <version>11.12.7</version>
</dependency>

<!-- JavaFX for general UI components -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.10</version>
</dependency>
```

## Application Flow

1. **Startup**: `main()` method launches JavaFX application
2. **UI Creation**: `start()` method creates the main window with:
   - Calendar view component
   - Toolbar with settings button
   - Window layout and styling
3. **User Interaction**: Settings button opens modal dialog
4. **Data Operations**: DAO classes handle database interactions
5. **Configuration**: Properties file manages database settings

## Code Quality Features

### Error Handling:
- Try-with-resources for automatic resource cleanup
- Exception propagation with meaningful error messages
- Graceful fallbacks (e.g., icon loading)

### Resource Management:
- Automatic connection closing via try-with-resources
- Static initialization for configuration loading
- Proper resource path handling

### Security:
- Prepared statements prevent SQL injection
- Parameterized queries for database operations

### Maintainability:
- Separation of concerns (UI, data, configuration)
- External configuration files
- Clean code structure with meaningful names

## Running the Application

To run the application locally:

```bash
# Build the project
mvn clean compile

# Run with JavaFX runtime (requires GUI environment)
mvn javafx:run

# Or run directly with main class
java -cp target/classes:target/dependency/* SimpleCalendarApp
```

**Note**: This is a GUI application that requires a display server. It cannot run in headless environments.

## Future Enhancement Opportunities

1. **Additional CRUD Operations**: Add read, update, delete methods to DAO
2. **Event Management**: Connect CalendarFX events to database storage
3. **Enhanced UI**: Add more calendar customization options
4. **Error Handling**: Implement comprehensive error handling and user feedback
5. **Testing**: Add unit tests for DAO and utility classes
6. **Logging**: Implement proper logging framework
7. **Validation**: Add input validation for calendar entries
8. **Internationalization**: Support multiple languages beyond German
9. **Themes**: Add customizable themes and styling options
10. **Import/Export**: Add calendar data import/export functionality