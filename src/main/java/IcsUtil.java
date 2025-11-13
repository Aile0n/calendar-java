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
 * Hilfsklasse für den Import und Export von Kalenderdaten in den Formaten
 * ICS (iCalendar) und VCS (vCalendar). ICS wird über die Bibliothek Biweekly
 * verarbeitet. Die VCS-Unterstützung ist minimal und individuell implementiert.
 */
public class IcsUtil {

    /**
     * Importiert Termine aus einer ICS-Datei (Pfad).
     */
    public static List<CalendarEntry> importIcs(Path path) throws Exception {
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            return importIcs(fis);
        }
    }

    /**
     * Importiert Termine aus einer ICS-URL.
     */
    public static List<CalendarEntry> importIcsFromUrl(String url) throws Exception {
        try (InputStream is = new URL(url).openStream()) {
            return importIcs(is);
        }
    }

    /**
     * Parst Termine aus einem ICS-Eingabestrom.
     */
    private static List<CalendarEntry> importIcs(InputStream is) throws Exception {
        List<CalendarEntry> entries = new ArrayList<>();
        List<ICalendar> calendars = Biweekly.parse(is).all();

        for (ICalendar calendar : calendars) {
            for (VEvent event : calendar.getEvents()) {
                if (event.getDateStart() == null) {
                    continue;
                }
                Date startDate = event.getDateStart().getValue();
                Date endDate = event.getDateEnd() != null ? event.getDateEnd().getValue() : startDate;

                LocalDateTime startLdt = LocalDateTime.ofInstant(startDate.toInstant(), ZoneId.systemDefault());
                LocalDateTime endLdt = LocalDateTime.ofInstant(endDate.toInstant(), ZoneId.systemDefault());

                String summary = event.getSummary() != null ? event.getSummary().getValue() : "(Ohne Titel)";
                String description = event.getDescription() != null ? event.getDescription().getValue() : "";

                CalendarEntry ce = new CalendarEntry(summary, description, startLdt, endLdt);

                List<Categories> categoriesList = event.getCategories();
                if (categoriesList != null && !categoriesList.isEmpty()) {
                    Categories categories = categoriesList.get(0);
                    if (categories != null && !categories.getValues().isEmpty()) {
                        ce.setCategory(categories.getValues().get(0));
                    }
                }

                List<VAlarm> alarms = event.getAlarms();
                if (!alarms.isEmpty()) {
                    for (VAlarm alarm : alarms) {
                        Trigger trigger = alarm.getTrigger();
                        if (trigger != null && trigger.getDuration() != null) {
                            Duration duration = trigger.getDuration();
                            Integer mins = parseDurationToMinutes(duration);
                            if (mins != null) {
                                ce.setReminderMinutesBefore(mins);
                                break;
                            }
                        }
                    }
                }

                entries.add(ce);
            }
        }
        return entries;
    }

    /**
     * Exportiert Termine in eine ICS-Datei.
     */
    public static void exportIcs(Path path, List<CalendarEntry> entries) throws Exception {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        ICalendar calendar = new ICalendar();
        calendar.setProductId("-//Calendar Java//Biweekly//DE");

        for (CalendarEntry entry : entries) {
            if (entry == null || entry.getStart() == null || entry.getEnd() == null) {
                continue;
            }
            VEvent event = new VEvent();

            String title = entry.getTitle() != null ? entry.getTitle() : "(Ohne Titel)";
            event.setSummary(title);

            Date start = Date.from(entry.getStart().atZone(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(entry.getEnd().atZone(ZoneId.systemDefault()).toInstant());
            event.setDateStart(start);
            event.setDateEnd(end);
            event.setUid(java.util.UUID.randomUUID().toString());

            if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
                event.setDescription(entry.getDescription());
            }
            if (entry.getCategory() != null && !entry.getCategory().isBlank()) {
                event.addCategories(entry.getCategory());
            }
            if (entry.getReminderMinutesBefore() != null && entry.getReminderMinutesBefore() > 0) {
                int minutes = entry.getReminderMinutesBefore();
                Duration duration = new Duration.Builder().prior(true).minutes(minutes).build();
                Trigger trigger = new Trigger(duration, (Related) null);
                VAlarm alarm = VAlarm.display(trigger, "Erinnerung");
                event.addAlarm(alarm);
            }
            calendar.addEvent(event);
        }
        Biweekly.write(calendar).go(path.toFile());
    }

    /**
     * Ermittelt das Format anhand der Dateiendung und importiert entsprechend.
     */
    public static List<CalendarEntry> importAuto(Path path) throws Exception {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".vcs")) {
            return importVcs(path);
        }
        return importIcs(path);
    }

    /**
     * Importiert Termine aus einer VCS-Datei (vCalendar 1.0).
     */
    public static List<CalendarEntry> importVcs(Path path) throws Exception {
        List<String> raw = Files.readAllLines(path);
        List<String> lines = unfoldLines(raw);
        List<CalendarEntry> result = new ArrayList<>();

        String summary = null;
        String description = null;
        LocalDateTime dtStart = null;
        LocalDateTime dtEnd = null;
        boolean inEvent = false;

        for (String line : lines) {
            String upper = line.toUpperCase(java.util.Locale.ROOT);
            if (upper.equals("BEGIN:VEVENT")) {
                inEvent = true;
                summary = null; description = null; dtStart = null; dtEnd = null;
                continue;
            }
            if (upper.equals("END:VEVENT")) {
                if (inEvent && dtStart != null) {
                    if (dtEnd == null) dtEnd = dtStart;
                    String s = summary != null ? summary : "(Ohne Titel)";
                    String d = description != null ? description : "";
                    result.add(new CalendarEntry(s, d, dtStart, dtEnd));
                }
                inEvent = false;
                continue;
            }
            if (!inEvent) continue;

            String name = getPropName(line);
            String value = getPropValue(line);

            switch (name) {
                case "SUMMARY": summary = unescapeText(value); break;
                case "DESCRIPTION": description = unescapeText(value); break;
                case "DTSTART": dtStart = parseVCalDateTime(value); break;
                case "DTEND": dtEnd = parseVCalDateTime(value); break;
                default: // andere ignorieren
            }
        }
        return result;
    }

    /**
     * Exportiert Termine in eine VCS-Datei (vCalendar 1.0).
     */
    public static void exportVcs(Path path, List<CalendarEntry> entries) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:1.0\r\n");
        sb.append("PRODID:-//Calendar Java//VCS 1.0//DE\r\n");

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
        sb.append("END:VCALENDAR\r\n");
        Files.writeString(path, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

    // ----- Hilfsfunktionen für VCS-Parsing/Formatierung -----

    private static List<String> unfoldLines(List<String> raw) {
        List<String> out = new ArrayList<>();
        String prev = null;
        for (String line : raw) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (prev == null) prev = "";
                prev += line.substring(1);
            } else {
                if (prev != null) out.add(prev);
                prev = line;
            }
        }
        if (prev != null) out.add(prev);
        return out;
    }

    private static String getPropName(String line) {
        int idx = line.indexOf(':');
        String left = idx >= 0 ? line.substring(0, idx) : line;
        int semi = left.indexOf(';');
        if (semi >= 0) left = left.substring(0, semi);
        return left.toUpperCase(java.util.Locale.ROOT).trim();
    }

    private static String getPropValue(String line) {
        int idx = line.indexOf(':');
        return idx >= 0 ? line.substring(idx + 1).trim() : "";
    }

    private static LocalDateTime parseVCalDateTime(String value) {
        String v = value.trim();
        try {
            if (v.endsWith("Z")) {
                String core = v.substring(0, v.length() - 1);
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                LocalDateTime ldt = LocalDateTime.parse(core, dtf);
                return ldt.atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            }
            if (v.length() == 8) {
                java.time.LocalDate ld = java.time.LocalDate.parse(v, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                return ld.atStartOfDay();
            }
            if (v.length() == 15 && v.charAt(8) == 'T') {
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                return LocalDateTime.parse(v, dtf);
            }
            return LocalDateTime.parse(v);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private static String formatVCalDateTime(LocalDateTime ldt) {
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        return ldt.format(dtf);
    }

    private static String escapeText(String s) {
        return s.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }

    private static String unescapeText(String s) {
        return s.replace("\\n", System.lineSeparator())
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }

    /**
     * Konvertiert eine ICS-Dauer in Minuten (absoluter Wert).
     */
    private static Integer parseDurationToMinutes(Duration duration) {
        if (duration == null) return null;
        try {
            int totalMinutes = 0;
            if (duration.getWeeks() != null) totalMinutes += duration.getWeeks() * 7 * 24 * 60;
            if (duration.getDays() != null) totalMinutes += duration.getDays() * 24 * 60;
            if (duration.getHours() != null) totalMinutes += duration.getHours() * 60;
            if (duration.getMinutes() != null) totalMinutes += duration.getMinutes();
            return Math.abs(totalMinutes);
        } catch (Exception e) {
            return null;
        }
    }
}
