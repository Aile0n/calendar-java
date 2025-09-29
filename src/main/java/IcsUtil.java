import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.util.RandomUidGenerator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Hilfsklasse für den Import und Export von iCalendar-Dateien (ICS) mit ical4j.
 * Bietet Methoden zum Einlesen von VEVENTs in CalendarEntry-Objekte und zum Schreiben
 * einer Liste von CalendarEntry als .ics-Datei.
 */
public class IcsUtil {

    public static List<CalendarEntry> importIcs(Path path) throws Exception {
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(fis);
            List<CalendarEntry> entries = new ArrayList<>();
            for (var component : calendar.getComponents(Component.VEVENT)) {
                VEvent ev = (VEvent) component;
                var start = ev.getStartDate().getDate();
                var end = ev.getEndDate() != null ? ev.getEndDate().getDate() : null;
                LocalDateTime startLdt = LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault());
                LocalDateTime endLdt = end != null ? LocalDateTime.ofInstant(end.toInstant(), ZoneId.systemDefault()) : startLdt;
                String summary = ev.getSummary() != null ? ev.getSummary().getValue() : "(Ohne Titel)";
                String description = ev.getDescription() != null ? ev.getDescription().getValue() : "";
                entries.add(new CalendarEntry(summary, description, startLdt, endLdt));
            }
            return entries;
        }
    }

    public static void exportIcs(Path path, List<CalendarEntry> entries) throws Exception {
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//Calendar Java//iCal4j 3.x//EN"));
        calendar.getProperties().add(net.fortuna.ical4j.model.property.Version.VERSION_2_0);
        for (CalendarEntry entry : entries) {
            java.util.Date start = java.util.Date.from(entry.getStart().atZone(ZoneId.systemDefault()).toInstant());
            java.util.Date end = java.util.Date.from(entry.getEnd().atZone(ZoneId.systemDefault()).toInstant());
            VEvent ev = new VEvent(new DateTime(start), new DateTime(end), entry.getTitle());
            if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
                ev.getProperties().add(new net.fortuna.ical4j.model.property.Description(entry.getDescription()));
            }
            calendar.getComponents().add(ev);
        }
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, fos);
        }
    }

    /**
     * Importiert automatisch anhand der Dateiendung: .vcs -> vCalendar, ansonsten ICS.
     */
    public static List<CalendarEntry> importAuto(Path path) throws Exception {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".vcs")) {
            return importVcs(path);
        }
        return importIcs(path);
    }

    /**
     * Einfache vCalendar (VCS 1.0) Import-Implementierung für VEVENT.
     * Unterstützt DTSTART, DTEND, SUMMARY, DESCRIPTION.
     */
    public static List<CalendarEntry> importVcs(Path path) throws Exception {
        java.util.List<String> raw = java.nio.file.Files.readAllLines(path);
        java.util.List<String> lines = unfoldLines(raw);
        java.util.List<CalendarEntry> result = new java.util.ArrayList<>();

        String summary = null;
        String description = null;
        java.time.LocalDateTime dtStart = null;
        java.time.LocalDateTime dtEnd = null;
        boolean inEvent = false;

        for (String line : lines) {
            String upper = line.toUpperCase(java.util.Locale.ROOT);
            if (upper.equals("BEGIN:VEVENT")) {
                inEvent = true;
                summary = null;
                description = null;
                dtStart = null;
                dtEnd = null;
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
                    // ignore others
            }
        }
        return result;
    }

    /**
     * Schreibt eine einfache vCalendar (VCS 1.0) Datei mit VEVENTs.
     */
    public static void exportVcs(Path path, java.util.List<CalendarEntry> entries) throws Exception {
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
        java.nio.file.Files.writeString(path, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

    // --------- Hilfsmethoden für VCS ---------

    private static java.util.List<String> unfoldLines(java.util.List<String> raw) {
        java.util.List<String> out = new java.util.ArrayList<>();
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

    private static java.time.LocalDateTime parseVCalDateTime(String value) {
        String v = value.trim();
        try {
            if (v.endsWith("Z")) {
                String core = v.substring(0, v.length() - 1);
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(core, dtf);
                return ldt.atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
            }
            if (v.length() == 8) { // DATE
                java.time.LocalDate ld = java.time.LocalDate.parse(v, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                return ld.atStartOfDay();
            }
            if (v.length() == 15 && v.charAt(8) == 'T') { // DATE-TIME
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                return java.time.LocalDateTime.parse(v, dtf);
            }
            // Fallback: try ISO
            return java.time.LocalDateTime.parse(v);
        } catch (Exception e) {
            // As last resort return now
            return java.time.LocalDateTime.now();
        }
    }

    private static String formatVCalDateTime(java.time.LocalDateTime ldt) {
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        return ldt.format(dtf);
    }

    private static String escapeText(String s) {
        String r = s.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace(",", "\\,")
                .replace(";", "\\;");
        return r;
    }

    private static String unescapeText(String s) {
        String r = s.replace("\\n", System.lineSeparator())
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
        return r;
    }
}
