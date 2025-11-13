# Projekterstellung – Calendar Java
Version: 1.0.3 — Stand: 2025-11-13

## Überblick
Dieses Dokument beschreibt die schrittweise Erstellung des Calendar-Java-Projekts von Anfang an.

Siehe auch:
- README.md – Schnellstart und Übersicht
- CODE_EXPLANATION.md – Code-Erklärung für Einsteiger
- FIX_SUMMARY.md – Zusammenfassung des Auto-Save Bugfixes
- BIWEEKLY_MIGRATION.md – Migration von ical4j zu Biweekly
- MANUAL_TEST_PLAN.md – Manueller Testplan
- THIRD-PARTY-NOTICES.md – Drittanbieter-Lizenzen
- LICENSE – Projektlizenz
- CHANGELOG.md – Versionshistorie

## Projektinformationen
- **Name**: Calendar Java
- **Autoren**:
  - Jan Erdmann
  - Kerim Talha Morca
  - Florian Alexander Knittel
- **Sprache**: Java 21
- **Build-Tool**: Maven
- **Version**: 1.0.3

## Entwicklungsgeschichte

### Version 0.1.0 (2025-09-30) - Initiale Version
**Ziel**: Grundlegende Kalenderanwendung mit ICS/VCS-Support

**Schritte**:
1. Maven-Projekt erstellt mit `pom.xml`
2. Dependencies hinzugefügt:
   - JavaFX 22.0.1 (UI-Framework)
   - CalendarFX 12.0.1 (Kalender-Komponente)
   - ical4j 3.2.7 (ICS-Parsing)
   - JUnit Jupiter 5.10.2 (Tests)
3. Domain-Model erstellt: `CalendarEntry.java`
4. Utility-Klassen implementiert:
   - `IcsUtil.java` - ICS/VCS Import/Export
   - `ConfigUtil.java` - Konfigurationsverwaltung
5. UI-Komponenten entwickelt:
   - `CalendarProjektApp.java` - Programmatische UI
   - `CalendarFxmlApp.java` - FXML-basierte UI
   - `CalendarProjektController.java` - FXML Controller
6. Launcher-Klasse `org.example.Main` für zuverlässigen JAR-Start
7. Maven Shade Plugin konfiguriert für Fat-JAR-Erstellung

### Version 0.1.1 (2025-10-08) - Dokumentation
**Ziel**: Verbesserte Dokumentation für neue Entwickler

**Änderungen**:
- `CODE_EXPLANATION.md` erstellt - Beginner-freundliche Code-Übersicht
- Inline-Kommentare in Hauptklassen hinzugefügt
- README aktualisiert mit klareren Setup-Anweisungen
- Link zu CODE_EXPLANATION.md hinzugefügt

### Version 0.2.0 (2025-10-08) - Persistenz-Bugfixes
**Ziel**: Kritischen Bug bei UI-erstellten Einträgen beheben

**Problem**:
Einträge, die direkt im CalendarFX UI erstellt wurden (durch Klick auf Kalender), wurden nicht automatisch gespeichert.

**Lösung**:
- `rebuildCurrentEntriesFromUI()` Methode implementiert
- Auto-Save (globaler CalendarView-Handler + Diff-basierter Monitor)
- Verbesserte ICS-Pfad-Logik mit Fallback zum Home-Verzeichnis
- Schreibberechtigungsprüfungen vor ICS-Dateierstellung

**Dateien geändert**:
- `CalendarProjektApp.java`
- `CalendarProjektController.java`
- `ConfigUtil.java`

### Version 1.0.0 (2025-10-09) - UI-Verbesserungen und ICS-Library-Migration
**Ziel**: Optimierte Benutzeroberfläche und Workflow; Migration auf Biweekly vorbereiten

**Änderungen (UI)**:
- Toolbar-Layout neu organisiert:
  - Links: Aktions-Buttons (Einstellungen, Neuer Termin, Import, Export)
  - Rechts: Status-Anzeige und "Beenden und Speichern"
  - Flexibler Spacer zwischen linker und rechter Seite
- Bessere visuelle Trennung zwischen Aktionen und Kontrollelementen
- Status-Label zeigt immer aktuelle Speicher-Information

**Technische Details (UI)**:
- `Region spacer = new Region()` mit `HBox.setHgrow(spacer, Priority.ALWAYS)`
- Toolbar-Items-Reihenfolge: `settingsButton, createBtn, importBtn, exportBtn, spacer, statusLabel, separator, exitBtn`

**Änderungen (ICS)**:
- Migration von ical4j (Code) zu Biweekly vorbereitet und umgesetzt (Details siehe `BIWEEKLY_MIGRATION.md`)

### Version 1.0.1 (2025-10-16) - Biweekly & Dokumentation
**Ziel**: Migration dokumentieren und Dokumentation vereinheitlichen

**Änderungen**:
- `BIWEEKLY_MIGRATION.md` erstellt/aktualisiert (Biweekly 0.6.8, ical4j 4.0.2 beibehalten)
- `IcsUtil.java` vollständig auf Biweekly umgestellt (Import/Export)
- `CODE_EXPLANATION.md` auf Biweekly aktualisiert und verlinkt
- `README.md` ergänzt (Biweekly, Querverweise)
- `THIRD-PARTY-NOTICES.md` korrigiert (Biweekly hinzugefügt, ical4j-Version aktualisiert)
- `MANUAL_TEST_PLAN.md` bereinigt (ICS-only, Reminder-Test ergänzt)
- `FIX_SUMMARY.md` an tatsächliche Umsetzung angepasst (Auto-Save Pipeline)
- `PROJEKT_ERSTELLUNG.md` mit Verlinkungen zu allen relevanten Dokumenten
- `CHANGELOG.md` mit Dokumentationsupdates zu Biweekly ergänzt

## Build-Anweisungen

### Voraussetzungen
- JDK 21 oder höher installiert
- Maven 3.9+ installiert
- JAVA_HOME Umgebungsvariable gesetzt

### Projekt kompilieren
```cmd
mvn clean compile
```

### Tests ausführen
```cmd
mvn test
```

### JAR erstellen
```cmd
mvn clean package
```

Das erzeugte Shaded JAR befindet sich in `target/` und enthält alle Abhängigkeiten.

### Anwendung starten
```cmd
java -jar target\calendar-java-1.0.3-shaded.jar
```

Oder in der IDE: `org.example.Main` ausführen

## Git und GitHub

Dieser Abschnitt beschreibt, wie Git eingerichtet und das Projekt auf GitHub veröffentlicht wird. Außerdem wird eine CI-Pipeline über GitHub Actions aktiviert.
# Code Explanation (Beginner Friendly)
### 1) Git initialisieren und erste Commits
```cmd
git init
git add .
git commit -m "Initial commit"
```

Empfehlung: `.gitignore` verwenden (Java/Maven/IDE). In diesem Repository ist eine passende `.gitignore` enthalten.

### 2) GitHub-Repository anlegen
1. Auf https://github.com ein neues Repository erstellen (Name: `calendar-java`).
2. Remote hinzufügen und auf den Hauptbranch `main` pushen:
```cmd
git branch -M main
git remote add origin https://github.com/<dein-user>/calendar-java.git
git push -u origin main
```
Hinweis: Bei HTTPS fragt Git nach Zugangsdaten/Token. Einrichten eines Personal Access Tokens (PAT) kann nötig sein.

### 3) Continuous Integration (GitHub Actions)
Dieses Projekt enthält eine Workflow-Datei unter `.github/workflows/ci.yml`. Sie baut und testet das Projekt bei jedem Push/PR.

- JDK: Temurin 21
- Betriebssysteme: Ubuntu, Windows
- Befehl: `mvn -B -ntp verify`

Ergebnisse sind im GitHub UI unter "Actions" einsehbar.

### 4) Optionale Schritte
- Branch-Schutzregeln aktivieren (Require passing checks)
- Release-Tagging und GitHub Releases
- Issue-Templates/PR-Templates hinzufügen

## Projektstruktur

```
calendar-java/
├── pom.xml                                 # Maven-Konfiguration
├── README.md                               # Projekt-Dokumentation
├── CHANGELOG.md                            # Versionshistorie
├── docs/
│   ├── CODE_EXPLANATION.md                 # Code-Erklärung für Anfänger
│   ├── FIX_SUMMARY.md                      # Bugfix-Dokumentation
│   ├── BIWEEKLY_MIGRATION.md               # Migration ical4j → Biweekly
│   ├── MANUAL_TEST_PLAN.md                 # Manueller Testplan
│   └── PROJEKT_ERSTELLUNG.md               # Diese Datei
├── THIRD-PARTY-NOTICES.md                  # Lizenzen von Dependencies
├── build-jar.cmd                           # Windows Build-Skript
├── calendar.ics                            # Beispiel-ICS-Datei
├── config.properties                       # Externe Konfiguration
├── .github/workflows/ci.yml                # GitHub Actions CI (Maven Build + Tests)
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── org/example/Main.java       # Launcher
    │   │   ├── CalendarFxmlApp.java        # FXML-basierte App
    │   │   ├── CalendarProjektApp.java     # Programmatische App
    │   │   ├── CalendarProjektController.java  # FXML Controller
    │   │   ├── CalendarEntry.java          # Domain-Model
    │   │   ├── ConfigUtil.java             # Konfigurations-Utility
    │   │   └── net/fortuna/ical4j/transform/recurrence/Frequency.java  # Shim
    │   └── resources/
    │       ├── calendar_view.fxml          # FXML-Layout
    │       ├── config.properties           # Standard-Konfiguration
    │       ├── dark.css                    # Dark-Mode Stylesheet
    │       └── META-INF/MANIFEST.MF        # JAR Manifest
    └── test/
        └── java/
            ├── IcsUtilTest.java            # ICS/VCS Tests
            └── CalendarUiPersistenceTest.java  # UI-Persistenz Tests
```

This document explains the main parts of the Calendar Java project and how data flows through the app. It's written for starters who want a quick mental model without diving into every implementation detail.

## Big Picture
- UI: JavaFX + CalendarFX provide the window, buttons, and calendar view.
- Data: Events (CalendarEntry) are stored in an ICS file (ICS-only design).
- Import/Export: ICS via Biweekly, plus basic VCS support. ical4j remains as a dependency for compatibility but ICS read/write in this app is implemented with Biweekly.
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
  - ICS: Uses Biweekly to parse and generate `.ics` files.
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

---
See also: BIWEEKLY_MIGRATION.md for details about the migration from ical4j to Biweekly.
