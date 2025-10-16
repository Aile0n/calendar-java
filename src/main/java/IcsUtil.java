import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.component.VAlarm;
import biweekly.parameter.Related;
import biweekly.property.*;
import biweekly.util.Duration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Hilfsklasse für den Import und Export von Kalenderdateien.
 *
 * Diese Klasse kann Kalenderdateien in zwei Formaten verarbeiten:
 * - ICS (iCalendar): Das moderne Standard-Format, das von den meisten Kalenderprogrammen verwendet wird
 * - VCS (vCalendar): Ein älteres Format, das manchmal noch verwendet wird
 *
 * Verwendet die "Biweekly"-Bibliothek für ICS-Dateien.
 */
public class IcsUtil {

    /**
     * Importiert Termine aus einer ICS-Datei auf dem Computer.
     *
     * @param path Der Pfad zur ICS-Datei (z.B. "C:\Dokumente\calendar.ics")
     * @return Eine Liste aller gefundenen Termine
     * @throws Exception Wenn die Datei nicht gelesen werden kann
     */
    public static List<CalendarEntry> importIcs(Path path) throws Exception {
        // Öffne die Datei und lese sie ein (wird automatisch geschlossen dank try-with-resources)
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            return importIcs(fis);
        }
    }

    /**
     * Importiert Termine aus einer ICS-Datei von einer URL (z.B. aus dem Internet).
     * Nützlich für Online-Kalender oder Kalender-Abonnements.
     *
     * @param url Die Webadresse der ICS-Datei (z.B. "https://example.com/calendar.ics")
     * @return Eine Liste aller gefundenen Termine
     * @throws Exception Wenn die URL nicht erreichbar ist oder die Datei fehlerhaft ist
     */
    public static List<CalendarEntry> importIcsFromUrl(String url) throws Exception {
        try (InputStream is = new URL(url).openStream()) {
            return importIcs(is);
        }
    }

    /**
     * Liest Termine aus einem Datenstrom (wird von den anderen Import-Methoden verwendet).
     *
     * @param is Der Eingabe-Datenstrom mit ICS-Daten
     * @return Eine Liste aller gefundenen Termine
     * @throws Exception Wenn die Daten nicht verarbeitet werden können
     */
    private static List<CalendarEntry> importIcs(InputStream is) throws Exception {
        // Hier sammeln wir alle Termine
        List<CalendarEntry> entries = new ArrayList<>();

        // Biweekly parst (analysiert) die ICS-Datei und findet alle Kalender darin
        List<ICalendar> calendars = Biweekly.parse(is).all();

        // Gehe durch jeden Kalender in der Datei
        for (ICalendar calendar : calendars) {
            // Gehe durch jeden Termin (VEvent = Event in iCalendar-Sprache)
            for (VEvent event : calendar.getEvents()) {
                // Prüfe, ob der Termin ein Startdatum hat (Pflichtfeld!)
                if (event.getDateStart() == null) {
                    continue; // Überspringe fehlerhafte Termine ohne Startzeit
                }

                // Hole Start- und Enddatum
                Date startDate = event.getDateStart().getValue();
                Date endDate = event.getDateEnd() != null ? event.getDateEnd().getValue() : startDate;

                // Konvertiere die alten Date-Objekte in moderne LocalDateTime-Objekte
                LocalDateTime startLdt = LocalDateTime.ofInstant(startDate.toInstant(), ZoneId.systemDefault());
                LocalDateTime endLdt = LocalDateTime.ofInstant(endDate.toInstant(), ZoneId.systemDefault());

                // Hole Titel und Beschreibung (mit Fallback-Werten, falls nicht vorhanden)
                String summary = event.getSummary() != null ? event.getSummary().getValue() : "(Ohne Titel)";
                String description = event.getDescription() != null ? event.getDescription().getValue() : "";

                // Erstelle ein CalendarEntry-Objekt aus den gelesenen Daten
                CalendarEntry ce = new CalendarEntry(summary, description, startLdt, endLdt);

                // --- Optionale Features ---

                // Kategorien einlesen (z.B. "Arbeit", "Privat")
                List<Categories> categoriesList = event.getCategories();
                if (categoriesList != null && !categoriesList.isEmpty()) {
                    Categories categories = categoriesList.get(0);
                    if (categories != null && !categories.getValues().isEmpty()) {
                        ce.setCategory(categories.getValues().get(0));
                    }
                }

                // VALARM = Erinnerungen/Alarme einlesen
                List<VAlarm> alarms = event.getAlarms();
                if (!alarms.isEmpty()) {
                    for (VAlarm alarm : alarms) {
                        Trigger trigger = alarm.getTrigger();
                        if (trigger != null && trigger.getDuration() != null) {
                            Duration duration = trigger.getDuration();
                            // Wandle die Dauer in Minuten um
                            Integer mins = parseDurationToMinutes(duration);
                            if (mins != null) {
                                ce.setReminderMinutesBefore(mins);
                                break; // Nur die erste Erinnerung verwenden
                            }
                        }
                    }
                }

                // Füge den Termin zur Liste hinzu
                entries.add(ce);
            }
        }

        return entries;
    }

    /**
     * Exportiert Termine in eine ICS-Datei.
     * Die erzeugte Datei kann in anderen Kalenderprogrammen (Outlook, Google Calendar, etc.) geöffnet werden.
     *
     * @param path Wo die Datei gespeichert werden soll
     * @param entries Die Liste der Termine, die gespeichert werden sollen
     * @throws Exception Falls beim Schreiben ein Fehler auftritt
     */
    public static void exportIcs(Path path, List<CalendarEntry> entries) throws Exception {
        // Sicherheitscheck: Falls keine Termine übergeben wurden, nimm eine leere Liste
        if (entries == null) {
            entries = new ArrayList<>();
        }
        
        // Erstelle ein neues ICalendar-Objekt (der Container für alle Termine)
        ICalendar calendar = new ICalendar();
        calendar.setProductId("-//Calendar Java//Biweekly//EN"); // Identifiziert unsere App

        // Gehe durch jeden Termin und wandle ihn in ein VEvent um
        for (CalendarEntry entry : entries) {
            // Überspringe ungültige Termine ohne Start oder Ende
            if (entry == null || entry.getStart() == null || entry.getEnd() == null) {
                continue;
            }
            
            // Erstelle ein neues Event für diesen Termin
            VEvent event = new VEvent();

            // Setze den Titel (mit Fallback, falls leer)
            String title = entry.getTitle() != null ? entry.getTitle() : "(Ohne Titel)";
            event.setSummary(title);

            // Wandle LocalDateTime zurück in Date-Objekte (für Biweekly)
            Date start = Date.from(entry.getStart().atZone(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(entry.getEnd().atZone(ZoneId.systemDefault()).toInstant());

            event.setDateStart(start);
            event.setDateEnd(end);

            // Erstelle eine eindeutige ID für den Termin (wichtig für Kalender-Synchronisation)
            event.setUid(java.util.UUID.randomUUID().toString());

            // Füge die Beschreibung hinzu, falls vorhanden
            if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
                event.setDescription(entry.getDescription());
            }

            // Füge die Kategorie hinzu, falls vorhanden
            if (entry.getCategory() != null && !entry.getCategory().isBlank()) {
                event.addCategories(entry.getCategory());
            }

            // Füge eine Erinnerung hinzu, falls gewünscht
            if (entry.getReminderMinutesBefore() != null && entry.getReminderMinutesBefore() > 0) {
                int minutes = entry.getReminderMinutesBefore();
                // Erstelle eine Dauer (z.B. "-15 Minuten" bedeutet 15 Minuten vor dem Termin)
                Duration duration = new Duration.Builder().prior(true).minutes(minutes).build();
                Trigger trigger = new Trigger(duration, (Related) null);
                VAlarm alarm = VAlarm.display(trigger, "Erinnerung");
                event.addAlarm(alarm);
            }

            // Füge das Event zum Kalender hinzu
            calendar.addEvent(event);
        }

        // Schreibe den Kalender in die Datei
        Biweekly.write(calendar).go(path.toFile());
    }

    /**
     * Importiert automatisch anhand der Dateiendung.
     * Erkennt, ob es eine .vcs oder .ics Datei ist und verwendet die passende Import-Methode.
     *
     * @param path Der Pfad zur Kalenderdatei
     * @return Eine Liste aller gefundenen Termine
     * @throws Exception Falls beim Lesen ein Fehler auftritt
     */
    public static List<CalendarEntry> importAuto(Path path) throws Exception {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".vcs")) {
            return importVcs(path); // Nutze VCS-Import für .vcs Dateien
        }
        return importIcs(path); // Ansonsten nutze ICS-Import
    }

    /**
     * Importiert Termine aus einer VCS-Datei (vCalendar 1.0 - ein älteres Format).
     * Unterstützt die wichtigsten Felder: DTSTART, DTEND, SUMMARY, DESCRIPTION.
     *
     * @param path Der Pfad zur VCS-Datei
     * @return Eine Liste aller gefundenen Termine
     * @throws Exception Falls beim Lesen ein Fehler auftritt
     */
    public static List<CalendarEntry> importVcs(Path path) throws Exception {
        // Lese alle Zeilen der Datei
        List<String> raw = Files.readAllLines(path);
        // "Entfalte" mehrzeilige Einträge (in VCS können Zeilen umgebrochen sein)
        List<String> lines = unfoldLines(raw);
        List<CalendarEntry> result = new ArrayList<>();

        // Variablen für den aktuell gelesenen Termin
        String summary = null;
        String description = null;
        LocalDateTime dtStart = null;
        LocalDateTime dtEnd = null;
        boolean inEvent = false; // Sind wir gerade innerhalb eines VEVENT-Blocks?

        // Gehe durch jede Zeile
        for (String line : lines) {
            String upper = line.toUpperCase(java.util.Locale.ROOT);

            // Beginn eines neuen Termins
            if (upper.equals("BEGIN:VEVENT")) {
                inEvent = true;
                // Setze alle Werte zurück für den neuen Termin
                summary = null;
                description = null;
                dtStart = null;
                dtEnd = null;
                continue;
            }

            // Ende des aktuellen Termins
            if (upper.equals("END:VEVENT")) {
                // Wenn wir einen gültigen Termin haben (mit Startzeit), speichere ihn
                if (inEvent && dtStart != null) {
                    if (dtEnd == null) dtEnd = dtStart; // Falls kein Ende angegeben, nutze Start
                    String s = summary != null ? summary : "(Ohne Titel)";
                    String d = description != null ? description : "";
                    result.add(new CalendarEntry(s, d, dtStart, dtEnd));
                }
                inEvent = false;
                continue;
            }

            // Ignoriere Zeilen außerhalb von VEVENT-Blöcken
            if (!inEvent) continue;

            // Extrahiere Feldname und Wert aus der Zeile (z.B. "SUMMARY:Meeting" -> name="SUMMARY", value="Meeting")
            String name = getPropName(line);
            String value = getPropValue(line);

            // Verarbeite die verschiedenen Felder
            switch (name) {
                case "SUMMARY":
                    summary = unescapeText(value);
                    break;
                case "DESCRIPTION":
                    description = unescapeText(value);
                    break;
                case "DTSTART":
                    dtStart = parseVCalDateTime(value);
                    break;
                case "DTEND":
                    dtEnd = parseVCalDateTime(value);
                    break;
                default:
                    // Andere Felder ignorieren wir
            }
        }
        return result;
    }

    /**
     * Schreibt eine einfache VCS-Datei (vCalendar 1.0).
     *
     * @param path Wo die Datei gespeichert werden soll
     * @param entries Die Termine, die gespeichert werden sollen
     * @throws Exception Falls beim Schreiben ein Fehler auftritt
     */
    public static void exportVcs(Path path, List<CalendarEntry> entries) throws Exception {
        StringBuilder sb = new StringBuilder();

        // VCS-Header
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:1.0\r\n");
        sb.append("PRODID:-//Calendar Java//VCS 1.0//DE\r\n");

        // Jeden Termin als VEVENT hinzufügen
        for (CalendarEntry e : entries) {
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("DTSTART:").append(formatVCalDateTime(e.getStart())).append("\r\n");
            sb.append("DTEND:").append(formatVCalDateTime(e.getEnd())).append("\r\n");
            if (e.getTitle() != null && !e.getTitle().isBlank()) {
                sb.append("SUMMARY:").append(escapeText(e.getTitle())).append("\r\n");
            }
            if (e.getDescription() != null && !e.getDescription().isBlank()) {
                sb.append("DESCRIPTION:").append(escapeText(e.getDescription())).append("\r\n");
            }
            sb.append("END:VEVENT\r\n");
        }

        // VCS-Footer
        sb.append("END:VCALENDAR\r\n");

        // Schreibe alles in die Datei
        Files.writeString(path, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

    // --------- Hilfsmethoden für VCS ---------
    // Diese Methoden helfen beim Parsen und Formatieren von VCS-Dateien

    /**
     * "Entfaltet" mehrzeilige Einträge in VCS-Dateien.
     * In VCS können lange Zeilen mit einem Leerzeichen am Anfang der nächsten Zeile fortgesetzt werden.
     */
    private static List<String> unfoldLines(List<String> raw) {
        List<String> out = new ArrayList<>();
        String prev = null;
        for (String line : raw) {
            // Zeilen, die mit Leerzeichen beginnen, sind Fortsetzungen
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (prev == null) prev = "";
                prev += line.substring(1); // Füge ohne das Leerzeichen hinzu
            } else {
                if (prev != null) out.add(prev);
                prev = line;
            }
        }
        if (prev != null) out.add(prev);
        return out;
    }

    /**
     * Extrahiert den Feldnamen aus einer VCS-Zeile.
     * Beispiel: "SUMMARY:Meeting" -> "SUMMARY"
     */
    private static String getPropName(String line) {
        int idx = line.indexOf(':');
        String left = idx >= 0 ? line.substring(0, idx) : line;
        int semi = left.indexOf(';');
        if (semi >= 0) left = left.substring(0, semi);
        return left.toUpperCase(java.util.Locale.ROOT).trim();
    }

    /**
     * Extrahiert den Wert aus einer VCS-Zeile.
     * Beispiel: "SUMMARY:Meeting" -> "Meeting"
     */
    private static String getPropValue(String line) {
        int idx = line.indexOf(':');
        return idx >= 0 ? line.substring(idx + 1).trim() : "";
    }

    /**
     * Wandelt einen VCS-Datums-String in ein LocalDateTime-Objekt um.
     * VCS unterstützt verschiedene Formate wie "20250110T140000" oder "20250110".
     */
    private static LocalDateTime parseVCalDateTime(String value) {
        String v = value.trim();
        try {
            // UTC-Zeit (endet mit Z)
            if (v.endsWith("Z")) {
                String core = v.substring(0, v.length() - 1);
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                LocalDateTime ldt = LocalDateTime.parse(core, dtf);
                return ldt.atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            }
            // Nur Datum (8 Zeichen: YYYYMMDD)
            if (v.length() == 8) {
                java.time.LocalDate ld = java.time.LocalDate.parse(v, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                return ld.atStartOfDay(); // Beginne um Mitternacht
            }
            // Datum + Zeit (15 Zeichen: YYYYMMDDTHHMMSS)
            if (v.length() == 15 && v.charAt(8) == 'T') {
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                return LocalDateTime.parse(v, dtf);
            }
            // Fallback: Versuche ISO-Format
            return LocalDateTime.parse(v);
        } catch (Exception e) {
            // Notfall-Fallback: Verwende die aktuelle Zeit
            return LocalDateTime.now();
        }
    }

    /**
     * Formatiert ein LocalDateTime für VCS im Format YYYYMMDDTHHMMSS.
     */
    private static String formatVCalDateTime(LocalDateTime ldt) {
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        return ldt.format(dtf);
    }

    /**
     * Maskiert Sonderzeichen für VCS-Text (z.B. Kommas, Zeilenumbrüche).
     */
    private static String escapeText(String s) {
        return s.replace("\\", "\\\\")    // Backslash zuerst!
                .replace("\n", "\\n")      // Zeilenumbruch
                .replace("\r", "")         // Carriage Return entfernen
                .replace(",", "\\,")       // Komma
                .replace(";", "\\;");      // Semikolon
    }

    /**
     * Entfernt Maskierungen aus VCS-Text.
     */
    private static String unescapeText(String s) {
        return s.replace("\\n", System.lineSeparator())
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }

    /**
     * Wandelt eine ICS-Duration (z.B. "P15M" für 15 Minuten) in Minuten um.
     * Wird für Erinnerungen verwendet.
     */
    private static Integer parseDurationToMinutes(Duration duration) {
        if (duration == null) return null;
        try {
            // Berechne die Gesamtzeit in Minuten
            int totalMinutes = 0;

            // Wochen in Minuten umrechnen (1 Woche = 7 Tage = 7*24*60 Minuten)
            if (duration.getWeeks() != null) {
                totalMinutes += duration.getWeeks() * 7 * 24 * 60;
            }
            // Tage in Minuten umrechnen (1 Tag = 24*60 Minuten)
            if (duration.getDays() != null) {
                totalMinutes += duration.getDays() * 24 * 60;
            }
            // Stunden in Minuten umrechnen (1 Stunde = 60 Minuten)
            if (duration.getHours() != null) {
                totalMinutes += duration.getHours() * 60;
            }
            // Minuten direkt addieren
            if (duration.getMinutes() != null) {
                totalMinutes += duration.getMinutes();
            }

            return Math.abs(totalMinutes); // Absoluter Wert (immer positiv)
        } catch (Exception e) {
            return null;
        }
    }
}
