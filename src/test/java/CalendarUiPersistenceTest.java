import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI-Persistenztests: Stellen sicher, dass Erstellen/Bearbeiten/Löschen von Terminen
 * in der CalendarFX-UI unmittelbar in der ICS-Datei gespeichert wird.
 */
public class CalendarUiPersistenceTest {

    private Path icsPath;

    @BeforeAll
    static void initJavaFx() {
        // Initialisiert JavaFX-Toolkit (headless, es wird kein Fenster angezeigt)
        new JFXPanel();
    }

    @BeforeEach
    void setupIcsPath() throws Exception {
        icsPath = Files.createTempFile("ui-cal-", ".ics");
        // Setze die Applikations-Config dynamisch auf die Testdatei
        ConfigUtil.setIcsPath(icsPath);
        // Schreibe eine minimale gültige ICS-Struktur, damit der erste Import nicht fehlschlägt
        String minimal = "BEGIN:VCALENDAR\r\n" +
                "VERSION:2.0\r\n" +
                "PRODID:-//Test//Calendar//EN\r\n" +
                "END:VCALENDAR\r\n";
        Files.writeString(icsPath, minimal);
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(icsPath);
    }

    private CalendarProjektController loadController() throws Exception {
        AtomicReference<CalendarProjektController> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/calendar_view.fxml"));
                Parent root = loader.load();
                // Szene zuweisen (kein Stage nötig)
                new Scene(root, 800, 600);
                CalendarProjektController controller = loader.getController();
                ref.set(controller);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latchAwait(latch), "JavaFX Init/Load Timeout");
        assertNotNull(ref.get(), "Controller sollte geladen sein");
        // Kleine Wartezeit, damit Initialisierung/Auto-Load abgeschlossen ist
        sleep(150);
        return ref.get();
    }

    private boolean latchAwait(CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private Calendar<?> getDefaultCalendar(CalendarView view) {
        for (CalendarSource src : view.getCalendarSources()) {
            for (Calendar<?> cal : src.getCalendars()) {
                if ("Allgemein".equalsIgnoreCase(cal.getName())) {
                    return cal;
                }
            }
        }
        // Fallback: erster Kalender
        return view.getCalendarSources().get(0).getCalendars().get(0);
    }

    private List<CalendarEntry> readIcsSafely() throws Exception {
        // Mehrfach versuchen, da Autosave asynchron erfolgt
        Exception last = null;
        for (int i = 0; i < 20; i++) {
            try {
                if (Files.size(icsPath) > 0) {
                    return IcsUtil.importIcs(icsPath);
                }
            } catch (Exception e) {
                last = e;
            }
            sleep(50);
        }
        if (last != null) throw last;
        return IcsUtil.importIcs(icsPath);
    }

    @Test
    void testAutoSaveOnAddEntry() throws Exception {
        CalendarProjektController controller = loadController();
        CalendarView view = controller.getCalendarViewForTest();
        Calendar<?> cal = getDefaultCalendar(view);

        LocalDateTime start = LocalDateTime.now().withSecond(0).withNano(0).plusMinutes(5);
        LocalDateTime end = start.plusHours(1);
        Entry<String> e = new Entry<>("UI Add Event");
        e.setLocation("desc");
        e.setInterval(start.atZone(ZoneId.systemDefault()), end.atZone(ZoneId.systemDefault()));

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            cal.addEntry(e);
            latch.countDown();
        });
        assertTrue(latchAwait(latch), "addEntry Timeout");

        // ICS sollte den Eintrag schnell enthalten
        List<CalendarEntry> back = readIcsSafely();
        assertTrue(back.stream().anyMatch(c -> "UI Add Event".equals(c.getTitle())), "Eintrag wurde nicht in ICS gespeichert");
    }

    @Test
    void testAutoSaveOnEditEntry() throws Exception {
        CalendarProjektController controller = loadController();
        CalendarView view = controller.getCalendarViewForTest();
        Calendar<?> cal = getDefaultCalendar(view);

        LocalDateTime start = LocalDateTime.now().withSecond(0).withNano(0).plusMinutes(10);
        LocalDateTime end = start.plusHours(2);
        Entry<String> e = new Entry<>("To Edit");
        e.setInterval(start.atZone(ZoneId.systemDefault()), end.atZone(ZoneId.systemDefault()));

        CountDownLatch addLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            cal.addEntry(e);
            addLatch.countDown();
        });
        assertTrue(latchAwait(addLatch), "addEntry Timeout");
        // Warten bis erster Save erfolgt
        readIcsSafely();

        // Ändern des Titels und der Endzeit
        CountDownLatch editLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            e.setTitle("Edited Title");
            e.changeEndDate(end.plusMinutes(30).toLocalDate());
            e.changeEndTime(end.plusMinutes(30).toLocalTime());
            editLatch.countDown();
        });
        assertTrue(latchAwait(editLatch), "edit Timeout");

        List<CalendarEntry> back = readIcsSafely();
        assertTrue(back.stream().anyMatch(c -> "Edited Title".equals(c.getTitle())), "Bearbeitung wurde nicht gespeichert");
    }

    @Test
    void testAutoSaveOnDeleteEntry() throws Exception {
        CalendarProjektController controller = loadController();
        CalendarView view = controller.getCalendarViewForTest();
        Calendar<?> cal = getDefaultCalendar(view);

        LocalDateTime start = LocalDateTime.now().withSecond(0).withNano(0).plusMinutes(15);
        LocalDateTime end = start.plusMinutes(30);
        Entry<String> e = new Entry<>("To Delete");
        e.setInterval(start.atZone(ZoneId.systemDefault()), end.atZone(ZoneId.systemDefault()));

        CountDownLatch addLatch = new CountDownLatch(1);
        Platform.runLater(() -> { cal.addEntry(e); addLatch.countDown(); });
        assertTrue(latchAwait(addLatch), "addEntry Timeout");
        // Warten bis gespeichert
        readIcsSafely();

        CountDownLatch delLatch = new CountDownLatch(1);
        Platform.runLater(() -> { cal.removeEntry(e); delLatch.countDown(); });
        assertTrue(latchAwait(delLatch), "removeEntry Timeout");

        // Prüfen, dass Eintrag nicht mehr existiert
        List<CalendarEntry> back = readIcsSafely();
        assertTrue(back.stream().noneMatch(c -> "To Delete".equals(c.getTitle())), "Löschen wurde nicht gespeichert");
    }

    @Test
    void testAutoSaveAcrossViews() throws Exception {
        CalendarProjektController controller = loadController();
        CalendarView view = controller.getCalendarViewForTest();
        Calendar<?> cal = getDefaultCalendar(view);

        // Day
        Platform.runLater(view::showDayPage);
        sleep(50);
        addAndAssert(cal, "DayView Event");

        // Week
        Platform.runLater(view::showWeekPage);
        sleep(50);
        addAndAssert(cal, "WeekView Event");

        // Month
        Platform.runLater(view::showMonthPage);
        sleep(50);
        addAndAssert(cal, "MonthView Event");

        // Year
        Platform.runLater(view::showYearPage);
        sleep(50);
        addAndAssert(cal, "YearView Event");

        List<CalendarEntry> back = readIcsSafely();
        assertTrue(back.stream().anyMatch(c -> "DayView Event".equals(c.getTitle())));
        assertTrue(back.stream().anyMatch(c -> "WeekView Event".equals(c.getTitle())));
        assertTrue(back.stream().anyMatch(c -> "MonthView Event".equals(c.getTitle())));
        assertTrue(back.stream().anyMatch(c -> "YearView Event".equals(c.getTitle())));
    }

    private void addAndAssert(Calendar<?> cal, String title) throws Exception {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        Entry<String> e = new Entry<>(title);
        e.setInterval(now.plusMinutes(1).atZone(ZoneId.systemDefault()), now.plusMinutes(31).atZone(ZoneId.systemDefault()));
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> { cal.addEntry(e); latch.countDown(); });
        assertTrue(latchAwait(latch), "addEntry Timeout");
        // Warten und prüfen
        List<CalendarEntry> back = readIcsSafely();
        assertTrue(back.stream().anyMatch(c -> title.equals(c.getTitle())), "Eintrag fehlt in ICS: " + title);
    }
}
