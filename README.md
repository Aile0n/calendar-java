# Calendar Java Application

A JavaFX-based calendar application with database integration for managing calendar events.

## Features

- **Modern UI**: Built with JavaFX and CalendarFX for a rich calendar interface
- **Database Integration**: SQLite database for storing calendar entries
- **Settings Dialog**: Configurable application settings
- **German Interface**: User interface in German language

## Prerequisites

- Java 17 or higher
- Maven 3.6+

## Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd calendar-java
   ```

2. **Build the project**
   ```bash
   mvn clean compile
   ```

3. **Run the application**
   ```bash
   mvn javafx:run
   ```

## Project Structure

```
src/main/java/
├── SimpleCalendarApp.java     # Main JavaFX application
├── CalendarEntry.java         # Calendar event data model
├── CalendarEntryDAO.java      # Database access object
└── DatabaseUtil.java          # Database connection utility

src/main/resources/
└── config.properties          # Database configuration
```

## Database Configuration

The application uses SQLite by default. Configuration is in `src/main/resources/config.properties`:

```properties
db.url=jdbc:sqlite:calendar.db
```

For other databases (MySQL, PostgreSQL), update the URL accordingly and add the appropriate JDBC driver dependency.

## Dependencies

- **CalendarFX 11.12.7**: Calendar UI components
- **JavaFX Controls 17.0.10**: Core JavaFX framework
- **JDBC**: Database connectivity (SQLite driver included in JDK)

## Code Documentation

For a detailed explanation of the code structure and architecture, see [CODE_EXPLANATION.md](CODE_EXPLANATION.md).

## License

This project is open source and available under the [MIT License](LICENSE).