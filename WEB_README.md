# Calendar Java - Web Version

## Web-Interface Nutzung

Die Kalender-Anwendung kann jetzt als Web-Anwendung genutzt werden!

### Web-Version starten

```bash
# Mit Maven
mvn spring-boot:run

# Oder mit dem gepackten JAR
mvn clean package -DskipTests
java -jar target/calendar-java-1.0-SNAPSHOT.jar
```

Die Web-Anwendung läuft dann unter: **http://localhost:8080**

### Features der Web-Version

- ✅ **Kalenderansicht**: Monatliche Kalenderansicht mit Navigation
- ✅ **Termine erstellen**: Neue Termine über das Web-Interface hinzufügen
- ✅ **Termine bearbeiten**: Bestehende Termine bearbeiten
- ✅ **Termine löschen**: Termine entfernen
- ✅ **Import/Export**: ICS/VCS-Dateien importieren und exportieren
- ✅ **Dark Mode**: Dunkelmodus-Unterstützung
- ✅ **Responsive**: Funktioniert auf Desktop und Mobilgeräten

### REST API Endpunkte

Die Anwendung stellt folgende REST-APIs bereit:

- `GET /api/calendar/entries` - Alle Termine abrufen
- `POST /api/calendar/entries` - Neuen Termin erstellen
- `PUT /api/calendar/entries/{index}` - Termin aktualisieren
- `DELETE /api/calendar/entries/{index}` - Termin löschen
- `POST /api/calendar/import` - ICS/VCS-Datei importieren
- `GET /api/calendar/export` - Kalender als ICS exportieren
- `GET /api/calendar/config` - Konfiguration abrufen
- `POST /api/calendar/config` - Konfiguration aktualisieren

### Screenshot

![Calendar Web Interface](https://github.com/user-attachments/assets/d95a2108-4278-4cd1-b996-d8286149544f)

### Technische Details

- **Backend**: Spring Boot 3.1.5 (Java 17)
- **Frontend**: HTML5, CSS3, Vanilla JavaScript
- **API**: RESTful JSON API
- **Persistenz**: ICS-Dateien (kompatibel mit Desktop-Version)

### Desktop-Version

Die ursprüngliche JavaFX-Desktop-Anwendung ist weiterhin verfügbar, erfordert aber Java 21 und wird separat gebuildet.
