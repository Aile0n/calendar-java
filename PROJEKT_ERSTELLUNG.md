# Projekterstellung – Calendar Java

## Überblick
Dieses Dokument beschreibt die schrittweise Erstellung des Calendar-Java-Projekts von Anfang an.

## Projektinformationen
- **Name**: Calendar Java
- **Autor**: Florian Knittel
- **Sprache**: Java 21
- **Build-Tool**: Maven
- **Version**: 1.0.0

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
- Auto-Save-Listener für CalendarFX Einträge hinzugefügt
- Verbesserte ICS-Pfad-Logik mit Fallback zum Home-Verzeichnis
- Schreibberechtigungsprüfungen vor ICS-Dateierstellung

**Dateien geändert**:
- `CalendarProjektApp.java`
- `CalendarProjektController.java`
- `ConfigUtil.java`

### Version 1.0.0 (2025-10-09) - UI-Verbesserungen
**Ziel**: Optimierte Benutzeroberfläche und Workflow

**Änderungen**:
- Toolbar-Layout neu organisiert:
  - Links: Aktions-Buttons (Einstellungen, Neuer Termin, Import, Export)
  - Rechts: Status-Anzeige und "Beenden und Speichern"
  - Flexibler Spacer zwischen linker und rechter Seite
- Bessere visuelle Trennung zwischen Aktionen und Kontrollelementen
- Status-Label zeigt immer aktuelle Speicher-Information

**Technische Details**:
- `Region spacer = new Region()` mit `HBox.setHgrow(spacer, Priority.ALWAYS)`
- Toolbar-Items-Reihenfolge: `settingsButton, createBtn, importBtn, exportBtn, spacer, statusLabel, separator, exitBtn`

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
java -jar target\calendar-java-1.0-SNAPSHOT-shaded.jar
```

Oder in der IDE: `org.example.Main` ausführen

## Projektstruktur

```
calendar-java/
├── pom.xml                                 # Maven-Konfiguration
├── README.md                               # Projekt-Dokumentation
├── CHANGELOG.md                            # Versionshistorie
├── CODE_EXPLANATION.md                     # Code-Erklärung für Anfänger
├── FIX_SUMMARY.md                          # Bugfix-Dokumentation
├── PROJEKT_ERSTELLUNG.md                   # Diese Datei
├── THIRD-PARTY-NOTICES.md                  # Lizenzen von Dependencies
├── build-jar.cmd                           # Windows Build-Skript
├── calendar.ics                            # Beispiel-ICS-Datei
├── config.properties                       # Externe Konfiguration
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── org/example/Main.java       # Launcher
    │   │   ├── CalendarFxmlApp.java        # FXML-basierte App
    │   │   ├── CalendarProjektApp.java     # Programmatische App
    │   │   ├── CalendarProjektController.java  # FXML Controller
    │   │   ├── CalendarEntry.java          # Domain-Model
    │   │   ├── ConfigUtil.java             # Konfigurations-Utility
    │   │   ├── IcsUtil.java                # ICS/VCS Import/Export
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

## Verwendete Technologien

### Core
- **Java 21**: Moderne Java-Features (Records, Pattern Matching, etc.)
- **Maven**: Dependency Management und Build-Automatisierung

### UI
- **JavaFX 22**: Desktop-UI-Framework
- **CalendarFX 12**: Professionelle Kalender-Komponente
- **FXML**: Deklarative UI-Definition

### Daten
- **ical4j 3.2.7**: RFC 5545 (iCalendar) Implementierung
- **Custom VCS Parser**: Minimale vCalendar 1.0 Unterstützung

### Testing
- **JUnit Jupiter 5.10.2**: Unit-Testing-Framework

### Packaging
- **Maven Shade Plugin**: Fat-JAR mit allen Dependencies

## Konfiguration

Die Anwendung verwendet `config.properties`:

```properties
# Pfad zur ICS-Datei
ics.path=calendar.ics

# Dark Mode aktivieren
ui.darkMode=false
```

Die Datei wird in folgender Reihenfolge gesucht:
1. Aktuelles Arbeitsverzeichnis
2. Classpath (Standard-Fallback)

## Features

### Implementiert
- ✅ ICS-Import/Export (ical4j)
- ✅ VCS-Import/Export (custom)
- ✅ CalendarFX UI Integration
- ✅ Deutsche Lokalisierung
- ✅ Dark Mode
- ✅ Automatisches Speichern
- ✅ Status-Anzeige
- ✅ Einstellungs-Dialog
- ✅ Termin-Erstellung per Dialog
- ✅ Termin-Erstellung per UI-Klick
- ✅ Drag & Drop Termine
- ✅ Info-Dialog mit Bibliotheks-Credits

### Zukünftige Erweiterungen
- ⏳ Wiederkehrende Termine (RRULE)
- ⏳ Erinnerungen (VALARM)
- ⏳ Kategorien/Tags
- ⏳ Mehrere Kalender-Quellen
- ⏳ Suche/Filter
- ⏳ Kalender-Freigabe

## Bekannte Einschränkungen
- Keine Datenbank-Persistenz (nur ICS)
- Begrenzte RRULE-Unterstützung
- VCS-Import: nur grundlegende Felder
- CalendarFX ist kommerziell lizenziert (Lizenz erforderlich)

## Support und Kontakt
**Autor**: Florian Knittel

Bei Fragen oder Problemen:
1. README.md und CODE_EXPLANATION.md lesen
2. CHANGELOG.md für bekannte Issues prüfen
3. FIX_SUMMARY.md für Bugfix-Details ansehen

