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
}
