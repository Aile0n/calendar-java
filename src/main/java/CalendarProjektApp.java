import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.CalendarEvent;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Hauptanwendung des Projekts. Zeigt einen CalendarFX-Kalender, lädt und speichert
 * Termine automatisch in eine ICS-Datei und bietet Import/Export sowie
 * Einstellungen und einen manuellen "Beenden & Speichern"-Knopf.
 */
public class CalendarProjektApp extends Application {

    private final Calendar fxCalendar = new Calendar("Termine");
    private final Map<String, Calendar> categoryCalendars = new HashMap<>();
    private final List<CalendarEntry> currentEntries = new ArrayList<>();
    private final Set<Calendar> observedCalendars = new HashSet<>();
    private CalendarSource mainSource;
    private Path calendarFile;
    private boolean updatingFromStorage;
    private boolean exiting;

    @Override
    public void start(Stage primaryStage) {
        Locale.setDefault(Locale.GERMANY);
        Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);

        calendarFile = ConfigUtil.ensureCalendarFile();

        CalendarView calendarView = new CalendarView();
        CalendarSource source = new CalendarSource("Meine Kalender");
        source.getCalendars().add(fxCalendar);
        calendarView.getCalendarSources().add(source);
        mainSource = source;
        attachCalendarListeners(fxCalendar);

        reloadFromStorage();

        ToolBar toolBar = new ToolBar();
        toolBar.getItems().add(buildSettingsButton(primaryStage));

        Button createButton = new Button("Neuer Termin");
        createButton.setOnAction(e -> showCreateDialog(primaryStage));
        toolBar.getItems().add(createButton);

        Button importButton = new Button("Importieren (ICS/VCS)");
        importButton.setOnAction(e -> doImport(primaryStage));
        toolBar.getItems().add(importButton);

        Button exportButton = new Button("Exportieren (ICS/VCS)");
        exportButton.setOnAction(e -> doExport(primaryStage));
        toolBar.getItems().add(exportButton);

        Button exitButton = new Button("Beenden & Speichern");
        exitButton.setOnAction(e -> exitAndSave());
        toolBar.getItems().add(exitButton);

        Button infoButton = new Button("Info");
        infoButton.setTooltip(new Tooltip("Informationen zu Bibliotheken und Autor"));
        infoButton.setOnAction(e -> showInfoDialog(primaryStage));
        ToolBar footer = new ToolBar(infoButton);

        VBox root = new VBox(toolBar, calendarView, footer);
        VBox.setVgrow(calendarView, Priority.ALWAYS);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CalendarProjekt");
        applyTheme(scene);
        primaryStage.setOnCloseRequest(evt -> {
            if (exiting) {
                return;
            }
            evt.consume();
            exitAndSave();
        });
        primaryStage.show();

        Platform.runLater(() -> localizeNode(root));
    }

    private Button buildSettingsButton(Stage owner) {
        Button settingsButton = new Button();
        settingsButton.setTooltip(new Tooltip("Einstellungen"));
        var iconUrl = getClass().getResource("/icons/settings.png");
        if (iconUrl != null) {
            ImageView iv = new ImageView(new Image(iconUrl.toExternalForm()));
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            settingsButton.setGraphic(iv);
        } else {
            settingsButton.setText("⚙");
        }

        settingsButton.setOnAction(e -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Einstellungen");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            applyThemeToDialog(dialog.getDialogPane());

            TextField icsPathField = new TextField(ConfigUtil.getIcsPath().toString());
            Button browse = new Button("…");
            browse.setOnAction(ev -> {
                FileChooser chooser = new FileChooser();
                chooser.setInitialFileName("calendar.ics");
                File f = chooser.showSaveDialog(dialog.getDialogPane().getScene().getWindow());
                if (f != null) {
                    icsPathField.setText(f.getAbsolutePath());
                }
            });

            CheckBox darkMode = new CheckBox("Dunkelmodus");
            darkMode.setSelected(ConfigUtil.isDarkMode());

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setStyle("-fx-padding: 10;");
            grid.add(new Label("ICS-Datei:"), 0, 0);
            grid.add(icsPathField, 1, 0);
            grid.add(browse, 2, 0);
            grid.add(new Label("Darstellung:"), 0, 1);
            grid.add(darkMode, 1, 1);

            dialog.getDialogPane().setContent(grid);

            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        ConfigUtil.setIcsPath(Path.of(icsPathField.getText().trim()));
                        ConfigUtil.setDarkMode(darkMode.isSelected());
                        ConfigUtil.save();
                        calendarFile = ConfigUtil.ensureCalendarFile();
                        applyTheme(owner.getScene());
                        reloadFromStorage();
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Einstellungen gespeichert.", ButtonType.OK);
                        alert.setHeaderText(null);
                        applyThemeToDialog(alert.getDialogPane());
                        alert.showAndWait();
                    } catch (Exception ex) {
                        showError("Konnte Einstellungen nicht speichern", ex);
                    }
                }
            });
        });

        return settingsButton;
    }

    private void reloadFromStorage() {
        updatingFromStorage = true;
        fxCalendar.clear();
        categoryCalendars.values().forEach(Calendar::clear);
        try {
            calendarFile = ConfigUtil.ensureCalendarFile();
            currentEntries.clear();
            if (Files.exists(calendarFile)) {
                currentEntries.addAll(IcsUtil.importIcs(calendarFile));
            }
            populateCalendar(currentEntries);
        } catch (Exception ex) {
            showError("Fehler beim Laden der ICS-Datei", ex);
        } finally {
            updatingFromStorage = false;
        }
    }

    private void populateCalendar(List<CalendarEntry> items) {
        for (CalendarEntry ce : items) {
            addEntryToView(ce);
        }
    }

    private Calendar getOrCreateCalendar(String category) {
        if ("Allgemein".equalsIgnoreCase(category)) {
            return fxCalendar;
        }
        return categoryCalendars.computeIfAbsent(category, key -> {
            Calendar cal = new Calendar(key);
            Calendar.Style[] styles = Calendar.Style.values();
            Calendar.Style style = styles[Math.abs(key.hashCode()) % styles.length];
            cal.setStyle(style);
            if (mainSource != null && !mainSource.getCalendars().contains(cal)) {
                mainSource.getCalendars().add(cal);
            }
            attachCalendarListeners(cal);
            return cal;
        });
    }

    private void persistEntries() {
        try {
            if (updatingFromStorage) {
                return;
            }
            ConfigUtil.ensureCalendarFile();
            currentEntries.clear();
            currentEntries.addAll(snapshotEntriesFromView());
            IcsUtil.exportIcs(calendarFile, new ArrayList<>(currentEntries));
        } catch (Exception ex) {
            showError("Kalender konnte nicht gespeichert werden", ex);
        }
    }

    private void doImport(Stage owner) {
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(all);
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        try {
            List<CalendarEntry> imported = IcsUtil.importAuto(file.toPath());
            updatingFromStorage = true;
            try {
                for (CalendarEntry ce : imported) {
                    addEntryToView(ce);
                }
            } finally {
                updatingFromStorage = false;
            }
            persistEntries();
        } catch (Exception ex) {
            showError("Import fehlgeschlagen", ex);
        }
    }

    private void doExport(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("calendar-export.ics");
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(ics);
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Path out = file.toPath();
            String lower = file.getName().toLowerCase();
            if (!lower.endsWith(".ics") && !lower.endsWith(".vcs")) {
                var sel = chooser.getSelectedExtensionFilter();
                if (sel != null && sel.getExtensions().contains("*.vcs")) {
                    out = out.resolveSibling(file.getName() + ".vcs");
                } else {
                    out = out.resolveSibling(file.getName() + ".ics");
                }
            }
            List<CalendarEntry> snapshot = snapshotEntriesFromView();
            if (out.toString().toLowerCase().endsWith(".vcs")) {
                IcsUtil.exportVcs(out, snapshot);
            } else {
                IcsUtil.exportIcs(out, snapshot);
            }
        } catch (Exception ex) {
            showError("Export fehlgeschlagen", ex);
        }
    }

    private void showCreateDialog(Stage owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Neuer Termin");
        ButtonType saveType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        applyThemeToDialog(dialog.getDialogPane());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10;");

        TextField titleField = new TextField();
        titleField.setPromptText("Titel");
        TextField descField = new TextField();
        descField.setPromptText("Beschreibung (optional)");
        DatePicker startDate = new DatePicker(LocalDate.now());
        TextField startTime = new TextField("09:00");
        DatePicker endDate = new DatePicker(LocalDate.now());
        TextField endTime = new TextField("10:00");

        grid.add(new Label("Titel:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Beschreibung:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Start (Datum / Zeit):"), 0, 2);
        grid.add(startDate, 1, 2);
        grid.add(startTime, 2, 2);
        grid.add(new Label("Ende (Datum / Zeit):"), 0, 3);
        grid.add(endDate, 1, 3);
        grid.add(endTime, 2, 3);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            if (!validateInputs(titleField.getText(), startDate.getValue(), startTime.getText(), endDate.getValue(), endTime.getText())) {
                evt.consume();
                Alert warning = new Alert(Alert.AlertType.WARNING, "Bitte Titel angeben und gültige Zeiten im Format HH:mm eingeben. Ende muss nach Start liegen.", ButtonType.OK);
                warning.initOwner(owner);
                warning.setHeaderText("Eingaben prüfen");
                applyThemeToDialog(warning.getDialogPane());
                warning.showAndWait();
                return;
            }
            try {
                LocalDateTime start = LocalDateTime.of(startDate.getValue(), parseTime(startTime.getText()));
                LocalDateTime end = LocalDateTime.of(endDate.getValue(), parseTime(endTime.getText()));
                CalendarEntry ce = new CalendarEntry(titleField.getText().trim(), descField.getText(), start, end);
                addEntryToView(ce);
                persistEntries();
            } catch (Exception ex) {
                evt.consume();
                showError("Speichern fehlgeschlagen", ex);
            }
        });

        dialog.showAndWait();
    }

    private boolean validateInputs(String title, LocalDate sd, String st, LocalDate ed, String et) {
        if (title == null || title.isBlank() || sd == null || ed == null) {
            return false;
        }
        LocalTime start = parseTime(st);
        LocalTime end = parseTime(et);
        if (start == null || end == null) {
            return false;
        }
        return LocalDateTime.of(sd, start).isBefore(LocalDateTime.of(ed, end));
    }

    private LocalTime parseTime(String text) {
        try {
            String normalized = text == null ? "" : text.trim();
            if (normalized.matches("^\\d{1}:\\d{2}$")) {
                normalized = "0" + normalized;
            }
            return LocalTime.parse(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private void exitAndSave() {
        if (exiting) {
            return;
        }
        exiting = true;
        persistEntries();
        Platform.exit();
    }

    private void addEntryToView(CalendarEntry ce) {
        if (ce == null || ce.getStart() == null || ce.getEnd() == null) {
            return;
        }
        ZoneId zone = ZoneId.systemDefault();
        Entry<String> entry = new Entry<>(ce.getTitle());
        if (ce.getDescription() != null && !ce.getDescription().isBlank()) {
            entry.setLocation(ce.getDescription());
        }
        entry.setInterval(ce.getStart().atZone(zone), ce.getEnd().atZone(zone));
        if (ce.getRecurrenceRule() != null && !ce.getRecurrenceRule().isBlank()) {
            try {
                entry.setRecurrenceRule(ce.getRecurrenceRule());
            } catch (Exception ignored) {
            }
        }
        String category = (ce.getCategory() == null || ce.getCategory().isBlank()) ? "Allgemein" : ce.getCategory();
        Calendar target = getOrCreateCalendar(category);
        target.addEntry(entry);
    }

    private List<CalendarEntry> snapshotEntriesFromView() {
        List<CalendarEntry> snapshot = new ArrayList<>();
        if (mainSource == null) {
            return snapshot;
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDate start = LocalDate.now().minusYears(100);
        LocalDate end = LocalDate.now().plusYears(100);
        Set<Entry<?>> seen = new HashSet<>();
        for (Calendar calendar : mainSource.getCalendars()) {
            String category = calendar == fxCalendar ? "Allgemein" : calendar.getName();
            for (Entry<?> entry : calendar.findEntries(start, end, zone)) {
                if (!seen.add(entry)) {
                    continue;
                }
                LocalDateTime startLdt = entry.getStartAsLocalDateTime();
                LocalDateTime endLdt = entry.getEndAsLocalDateTime();
                if (startLdt == null || endLdt == null) {
                    continue;
                }
                CalendarEntry ce = new CalendarEntry(entry.getTitle(), entry.getLocation(), startLdt, endLdt);
                ce.setCategory(category);
                String recurrence = entry.getRecurrenceRule();
                if (recurrence != null && !recurrence.isBlank()) {
                    ce.setRecurrenceRule(recurrence);
                }
                snapshot.add(ce);
            }
        }
        return snapshot;
    }

    private void attachCalendarListeners(Calendar calendar) {
        if (calendar == null || observedCalendars.contains(calendar)) {
            return;
        }
        observedCalendars.add(calendar);
        calendar.addEventHandler(CalendarEvent.ANY, event -> {
            if (event == null || event.getEntry() == null || updatingFromStorage) {
                return;
            }
            persistEntries();
        });
    }

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fehler");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void showInfoDialog(Stage owner) {
        StringBuilder sb = new StringBuilder();
        sb.append("CalendarProjekt\n\n");
        sb.append("Verwendete Bibliotheken:\n");
        sb.append("- CalendarFX 12.0.1\n");
        sb.append("- JavaFX 22.0.1\n");
        sb.append("- ical4j 3.2.7\n\n");
        sb.append("Autor: Florian Knittel\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Info");
        alert.setHeaderText("Über dieses Projekt");
        alert.setContentText(sb.toString());
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyThemeToDialog(DialogPane pane) {
        if (pane == null) {
            return;
        }
        try {
            String darkCss = getClass().getResource("/dark.css").toExternalForm();
            var sheets = pane.getStylesheets();
            sheets.remove(darkCss);
            if (ConfigUtil.isDarkMode()) {
                sheets.add(darkCss);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        try {
            String darkCss = getClass().getResource("/dark.css").toExternalForm();
            var sheets = scene.getStylesheets();
            sheets.remove(darkCss);
            if (ConfigUtil.isDarkMode()) {
                sheets.add(darkCss);
            }
        } catch (Exception ignored) {
        }
    }

    private void localizeNode(Node node) {
        if (node == null) {
            return;
        }
        if (node instanceof ButtonBase button) {
            String text = button.getText();
            if (text != null) {
                switch (text) {
                    case "Today" -> button.setText("Heute");
                    case "Day" -> button.setText("Tag");
                    case "Week" -> button.setText("Woche");
                    case "Month" -> button.setText("Monat");
                    case "Year" -> button.setText("Jahr");
                    case "Print" -> button.setText("Drucken");
                    case "Add Calendar" -> button.setText("Kalender hinzufügen");
                }
            }
        }
        if (node instanceof TextField field) {
            if ("Search".equals(field.getPromptText())) {
                field.setPromptText("Suche");
            }
        }
        if (node instanceof Labeled labeled) {
            if ("Calendars".equals(labeled.getText())) {
                labeled.setText("Kalender");
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                localizeNode(child);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
