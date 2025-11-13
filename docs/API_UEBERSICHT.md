# API-Übersicht (Klassen, Felder, Methoden)
Version: 1.0.3 — Stand: 2025-11-13

Diese Übersicht listet die wichtigsten Klassen, Felder (Variablen) und Methoden der Anwendung auf. Quelle: `src/main/java`.

Hinweis: Sichtbarkeiten werden angegeben, soweit aus dem Code ersichtlich. Private Hilfsmethoden sind der Vollständigkeit halber mit aufgeführt, wenn sie für das Verständnis nützlich sind.

---

## Paket: org.example

### Klasse: `org.example.Main`
- Methoden
  - `public static void main(String[] args)` – Startet wahlweise `CalendarFxmlApp` oder `CalendarProjektApp` via Reflection.

---

## Wurzelpaket (kein explizites package)

### Klasse: `CalendarEntry`
- Felder
  - `private Integer id`
  - `private String title`
  - `private String description`
  - `private java.time.LocalDateTime start`
  - `private java.time.LocalDateTime end`
  - `private Integer reminderMinutesBefore`
  - `private String category`
- Konstruktoren
  - `public CalendarEntry()`
  - `public CalendarEntry(Integer id, String title, String description, LocalDateTime start, LocalDateTime end)`
  - `public CalendarEntry(String title, String description, LocalDateTime start, LocalDateTime end)`
- Methoden
  - Getter/Setter: `getId/setId`, `getTitle/setTitle`, `getDescription/setDescription`, `getStart/setStart`, `getEnd/setEnd`, `getReminderMinutesBefore/setReminderMinutesBefore`, `getCategory/setCategory`
  - `public String toString()`
  - `public boolean equals(Object o)`
  - `public int hashCode()`

### Klasse: `ConfigUtil`
- Felder
  - `private static final String FILE_NAME` – "config.properties"
  - `private static Properties props`
  - `private static java.nio.file.Path externalConfigPath`
- Statischer Initialisierer
  - Lädt Konfiguration (`load()`).
- Methoden (statisch)
  - `public static synchronized void setExternalConfigPathForTest(Path path)`
  - `public static synchronized void load()` – lädt Werte aus externer Datei bzw. Ressourcen und setzt Defaults
  - `public static synchronized void save()` – schreibt nach externer Datei
  - `public static Path getIcsPath()`
  - `public static void setIcsPath(Path path)`
  - `public static boolean isDarkMode()`
  - `public static void setDarkMode(boolean dark)`

### Klasse: `IcsUtil`
- Öffentliche Methoden (statisch)
  - `public static java.util.List<CalendarEntry> importIcs(java.nio.file.Path path)`
  - `public static java.util.List<CalendarEntry> importIcsFromUrl(String url)`
  - `public static java.util.List<CalendarEntry> importAuto(java.nio.file.Path path)` – erkennt Format via Dateiendung
  - `public static void exportIcs(java.nio.file.Path path, java.util.List<CalendarEntry> entries)`
  - `public static java.util.List<CalendarEntry> importVcs(java.nio.file.Path path)`
  - `public static void exportVcs(java.nio.file.Path path, java.util.List<CalendarEntry> entries)`
- Private Hilfsmethoden
  - `private static java.util.List<CalendarEntry> importIcs(java.io.InputStream is)`
  - `private static java.util.List<String> unfoldLines(java.util.List<String> raw)`
  - `private static String getPropName(String line)`
  - `private static String getPropValue(String line)`
  - `private static java.time.LocalDateTime parseVCalDateTime(String value)`
  - `private static String formatVCalDateTime(java.time.LocalDateTime ldt)`
  - `private static String escapeText(String s)`
  - `private static String unescapeText(String s)`
  - `private static Integer parseDurationToMinutes(biweekly.util.Duration duration)`

### Klasse: `VersionUtil`
- Methoden (statisch)
  - `public static String getVersion()` – ermittelt Version via Manifest, `pom.properties` oder Fallback "1.0.3".

### Klasse: `CalendarFxmlApp` (JavaFX)
- Methoden
  - `public void start(javafx.stage.Stage stage)` – lädt FXML `calendar_view.fxml`
  - `public static void main(String[] args)`

### Klasse: `CalendarProjektApp` (JavaFX, ohne FXML)
- Felder
  - `private final com.calendarfx.model.Calendar<String> fxCalendar`
  - `private final java.util.List<CalendarEntry> currentEntries`
  - `private String lastEntriesSnapshot`
  - `private javafx.animation.Timeline autoSaveTimeline`
  - `private javafx.animation.PauseTransition debounceSave`
  - `private javafx.animation.Timeline periodicFullSave`
  - `private final java.util.Set<com.calendarfx.model.Entry<?>> trackedEntries`
  - `private javafx.scene.control.Label statusLabel`
  - `private static final boolean DIAG_VERBOSE`
  - `private static final boolean DIAG_SNAPSHOT`
  - `private volatile boolean dirty`
- Öffentliche Methoden
  - `public void start(javafx.stage.Stage primaryStage)`
  - `public static void main(String[] args)`
- Private Methoden (Auswahl)
  - UI/Lokalisierung/Diagnose: `localizeNode(Parent)`, `tryDumpMiniCalendar(Parent)`, `scanHeaderLabels(Parent)`
  - Dialoge/Theme: `showCreateDialog(Stage)`, `showInfoDialog(Stage)`, `showError(String, Exception)`, `applyThemeToDialog(DialogPane)`, `applyTheme(Scene)`
  - Persistenz/Autosave: `startPeriodicFullSave()`, `attachAutoPersistence()`, `autoSaveIfChanged()`, `updateSnapshot()`, `refreshTrackingFromCalendar()`, `computeSnapshot()`
  - Export/Import: `doImport(Stage)`, `doExport(Stage)`
  - Datenaufbereitung: `loadEntries()`, `rebuildCurrentEntriesFromUI()`
  - Hilfen: `buildEntryDiagnostic(String, List<CalendarEntry>)`, `getFileSizeSafe(Path)`, `countVevents(Path)`, `attachEntryListeners(Entry<?>)`, `scheduleDebouncedSave()`, `updateStatus(String, String)`, `logDiag(String)`, `logVerbose(String)`

### Klasse: `CalendarProjektController` (JavaFX-Controller für FXML)
- FXML-Felder (UI)
  - `@FXML AnchorPane calendarContainer`
  - `@FXML Button newButton, importButton, exportButton, settingsButton, exitButton, infoButton, manualSaveButton`
  - `@FXML Label statusLabel, saveStatusLabel`
- Interne Felder
  - `private com.calendarfx.view.CalendarView calendarView`
  - `private final com.calendarfx.model.Calendar<String> fxCalendar`
  - `private final java.util.Map<String, com.calendarfx.model.Calendar<String>> categoryCalendars`
  - `private final java.time.ZoneId zone`
  - `private final java.util.List<CalendarEntry> currentEntries`
  - Flags/Zähler: `private boolean suppressAutoSave`, `private boolean initialLoadCompleted`, `private int lastSavedCount`, `private boolean calendarEventsHooked`
  - Reminder/Autosave: `private javafx.animation.Timeline reminderTimeline`, `private final java.util.Set<String> notified`, `private javafx.animation.Timeline autosaveTimeline`, `private String lastUiSignature`
- Öffentliche Methoden
  - `public void initialize(URL location, ResourceBundle resources)`
  - `public com.calendarfx.view.CalendarView getCalendarViewForTest()`
- Private Methoden (Auswahl)
  - UI/Status: `setStatus(String)`, `setSaveStatus(String)`, `applyTheme()`, `applyThemeToDialog(DialogPane)`
  - Buttons/Flows: `ensureManualSaveButton()`, `manualSaveAction()`
  - Laden/Aktualisieren: `reloadData()`, `populateCalendar(List<CalendarEntry>)`, `rebuildCurrentEntriesFromUI()`
  - Kategorien: `getOrCreateCalendar(String)`, `setupCalendarListeners()`, `addCalendarListener(Calendar<?>)`
  - Persistenz: `saveCurrentEntriesToIcs()`
  - Dialoge/Interaktionen: `onNewEntry(ActionEvent)`, `onImport(ActionEvent)`, `onExport(ActionEvent)`, `onSettings(ActionEvent)`, `onExit(ActionEvent)`, `onInfo(ActionEvent)`
  - Validierung/Parsing: `validateInputs(...)`, `parseTime(String)`
  - Reminder: `scheduleReminders(List<CalendarEntry>)`, `checkReminders(List<CalendarEntry>)`
  - Autosave: `computeUiSignature()`, `startAutosaveMonitor()`
  - Logging: `log(String, String)`

---

## Paket: net.fortuna.ical4j.transform.recurrence

### Enum: `Frequency`
- Konstanten: `SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY`
- Zweck: Kompatibilität für CalendarFX (Stub für entfernte ical4j-Klasse)

---

## Bemerkungen
- Öffentliche API für externe Nutzung sind vor allem:
  - `IcsUtil` (Import/Export), `ConfigUtil` (Konfiguration), `CalendarEntry` (Datenmodell), `VersionUtil` (Version), die JavaFX-Apps (`CalendarFxmlApp`, `CalendarProjektApp`) und der Launcher `org.example.Main`.
- Der JavaFX-Controller `CalendarProjektController` ist primär intern mit FXML verdrahtet und nicht für direkte externe Verwendung gedacht.
