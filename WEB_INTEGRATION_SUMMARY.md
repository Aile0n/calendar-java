# Web-Integration Zusammenfassung

## Aufgabe
Die JavaFX-Kalenderanwendung in eine Webseite einbauen.

## Lösung
Eine vollständige Web-Version der Kalenderanwendung wurde erfolgreich implementiert mit:

### Backend (Spring Boot)
- REST API Controller (`CalendarRestController.java`)
- Alle CRUD-Operationen für Kalendereinträge
- Import/Export von ICS/VCS-Dateien
- Konfigurationsverwaltung
- Wiederverwendung der bestehenden Utility-Klassen (`IcsUtil`, `ConfigUtil`, `CalendarEntry`)

### Frontend (HTML/CSS/JavaScript)
- `index.html` - Hauptseite mit Kalenderansicht
- `calendar.css` - Modernes, responsives Design mit Dark Mode
- `calendar.js` - Interaktive Kalenderfunktionalität

### Features
✅ Monatliche Kalenderansicht mit Navigation
✅ Termine erstellen, bearbeiten und löschen
✅ Import/Export von ICS/VCS-Dateien über Web-UI
✅ Dark Mode Unterstützung
✅ Responsive Design
✅ REST API für externe Integration
✅ Shared Persistence mit Desktop-Version

### Technische Entscheidungen

1. **Package-Struktur**: Alle Web-Klassen in `com.calendar.web` package
   - Vermeidet Spring Boot Probleme mit Default Package
   - Klare Trennung zwischen Web und Desktop Code

2. **Maven Konfiguration**:
   - JavaFX/CalendarFX-Abhängigkeiten als optional markiert
   - Compiler-Exclusions für JavaFX-Klassen (benötigen Java 21)
   - Web-Version läuft mit Java 17

3. **Minimale Änderungen**:
   - Desktop-Version bleibt intakt
   - Utility-Klassen wurden in das Web-Package kopiert
   - ICS-Persistenz funktioniert für beide Versionen
   
   **Hinweis**: Die Utility-Klassen (CalendarEntry, ConfigUtil, IcsUtil) wurden kopiert statt geteilt,
   um minimale Änderungen am bestehenden Code zu gewährleisten. Für Produktionsumgebungen sollte 
   ein gemeinsames Package erstellt werden, auf das beide Versionen zugreifen.

## Verwendung

### Web-Version starten:
```bash
mvn spring-boot:run
```
Oder:
```bash
mvn clean package -DskipTests
java -jar target/calendar-java-1.0-SNAPSHOT.jar
```

### Zugriff:
http://localhost:8080

## Dateien

### Neue Dateien:
- `src/main/java/com/calendar/web/CalendarWebApp.java` - Spring Boot Hauptklasse
- `src/main/java/com/calendar/web/CalendarRestController.java` - REST API Controller
- `src/main/java/com/calendar/web/CalendarEntry.java` - Kopie für Web-Package
- `src/main/java/com/calendar/web/ConfigUtil.java` - Kopie für Web-Package
- `src/main/java/com/calendar/web/IcsUtil.java` - Kopie für Web-Package
- `src/main/resources/static/index.html` - Web-Frontend
- `src/main/resources/static/calendar.css` - Styles
- `src/main/resources/static/calendar.js` - JavaScript Logik
- `src/main/resources/application.properties` - Spring Boot Konfiguration
- `WEB_README.md` - Web-Version Dokumentation

### Geänderte Dateien:
- `pom.xml` - Spring Boot Dependencies, Compiler-Exclusions
- `README.md` - Dokumentation für beide Versionen

## Tests
- Bestehende IcsUtil-Tests laufen weiterhin (außer ein pre-existierender Fehler)
- Web-API funktioniert korrekt (manuell getestet)
- Frontend funktioniert korrekt (Screenshot vorhanden)

## Screenshot
![Calendar Web Interface](https://github.com/user-attachments/assets/d95a2108-4278-4cd1-b996-d8286149544f)
