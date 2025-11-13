# Konfiguration und Variablenübersicht
Version: 1.0.3 — Stand: 2025-11-13

Diese Datei beschreibt alle relevanten Konfigurationsschlüssel (config.properties) und die wichtigsten öffentlichen Datenstrukturen der Anwendung. Alle Angaben sind in deutscher Sprache.

## Konfigurationsdatei (config.properties)

Die Anwendung liest Einstellungen aus einer externen Datei `config.properties` im Arbeitsverzeichnis. Falls diese nicht vorhanden ist, werden Klassenpfad-Defaults aus `src/main/resources/config.properties` verwendet. Beim Speichern schreibt die Anwendung in die externe Datei.

Lade-/Speicher-Reihenfolge:
- 1) Externe Datei (typischerweise: `./config.properties`)
- 2) Fallback: Klassenpfad-Defaults (`src/main/resources/config.properties`)
- Fehlen Werte, werden sinnvolle Defaults gesetzt.

Speicherort extern: Standardmäßig `./config.properties`. Tests können temporär einen anderen Pfad setzen.

### Unterstützte Schlüssel

- ics.path
  - Typ: Pfad (String)
  - Standard: `calendar.ics` (bzw. im Home-Verzeichnis, falls das Arbeitsverzeichnis nicht beschreibbar ist)
  - Verwendung: Pfad zur ICS-Datei, in der Termine gelesen/gespeichert werden.

- ui.darkMode
  - Typ: Boolean (`true`/`false`)
  - Standard: `false`
  - Verwendung: Aktiviert ein dunkles Stylesheet (`dark.css`) für UI-Dialoge und die Oberfläche.

### Nicht verwendete/ignorierte Schlüssel (Stand dieser Version)

In einer externen `config.properties` im Projekt-Stamm wurden zusätzlich folgende Schlüssel gefunden. Diese werden von der aktuellen Version nicht ausgewertet und daher ignoriert:
- db.url
- feeds.refreshMinutes
- feeds.urls
- storage.mode

Hinweis: Unbekannte Schlüssel beeinträchtigen die Anwendung nicht, sie werden lediglich ignoriert.

### Beispiel: config.properties

```properties
# Pfad zur ICS-Datei
ics.path=calendar.ics

# Dunkelmodus für die UI
ui.darkMode=false
```

## Wichtige Datenstruktur: CalendarEntry

Interne Domänenklasse zur Repräsentation eines Termins.
- id: Integer (optional) – Eindeutige Kennung
- title: String – Titel des Termins
- description: String – Beschreibung (optional)
- start: LocalDateTime – Startzeitpunkt
- end: LocalDateTime – Endzeitpunkt
- reminderMinutesBefore: Integer (optional) – Minuten vor Start als Erinnerung
- category: String (optional) – Kategorie/Label

## Import/Export-Utilities (IcsUtil)

Öffentliche Methoden:
- importIcs(Path pfad): List<CalendarEntry>
  - Liest Termine aus einer ICS-Datei ein.
- importIcsFromUrl(String url): List<CalendarEntry>
  - Liest Termine aus einer ICS-Quelle per URL ein.
- importAuto(Path pfad): List<CalendarEntry>
  - Ermittelt anhand der Dateiendung automatisch das Format (ICS oder VCS) und importiert entsprechend.
- exportIcs(Path pfad, List<CalendarEntry> einträge): void
  - Schreibt Termine in eine ICS-Datei. Fügt optional Kategorien und Erinnerungen hinzu.
- importVcs(Path pfad): List<CalendarEntry>
  - Liest Termine aus einer VCS-Datei (vCalendar 1.0).
- exportVcs(Path pfad, List<CalendarEntry> einträge): void
  - Schreibt Termine in eine VCS-Datei (vCalendar 1.0).

Weitere Hinweise:
- ICS wird über die Bibliothek "Biweekly" verarbeitet.
- VCS-Unterstützung ist minimal selbst implementiert (grundlegende Felder: DTSTART, DTEND, SUMMARY, DESCRIPTION).

## Verhalten und Defaults

- Fehlt `ics.path`, wird standardmäßig `calendar.ics` genutzt. Ist das Arbeitsverzeichnis nicht beschreibbar, wird auf das Benutzerverzeichnis ausgewichen.
- Fehlt `ui.darkMode`, wird `false` gesetzt (helles Thema).
- Unbekannte Schlüssel werden ignoriert.

## Troubleshooting

- Änderungen an den Einstellungen lassen sich über die UI speichern (Einstellungen-Dialog). Die Anwendung schreibt dann in die externe `config.properties`.
- Falls die ICS-Datei nicht existiert, erzeugt die Anwendung bei Bedarf eine leere ICS-Datei.
- Bei Problemen mit dem Dateizugriff (z. B. Berechtigungen) bitte Schreibrechte für das Zielverzeichnis prüfen oder einen anderen Pfad in `ics.path` setzen.
