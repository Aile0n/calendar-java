import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * FXML-basierte Variante der Anwendung mit denselben Funktionen wie
 * {@link CalendarProjektApp}: automatische ICS-Speicherung, vereinfachte
 * Einstellungen und ein Button zum Beenden mit Speichern.
 */
public class CalendarProjektController implements Initializable {

    @FXML private AnchorPane calendarContainer;
    @FXML private Button newButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private Button settingsButton;
    @FXML private Button exitButton;

    private CalendarView calendarView;
    private final Calendar fxCalendar = new Calendar("Allgemein");
    private final Map<String, Calendar> categoryCalendars = new HashMap<>();
    private final List<CalendarEntry> currentEntries = new ArrayList<>();
    private CalendarSource mainSource;
    private Path calendarFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        calendarFile = ConfigUtil.ensureCalendarFile();

        calendarView = new CalendarView();
        mainSource = new CalendarSource("Meine Kalender");
        mainSource.getCalendars().add(fxCalendar);
        calendarView.getCalendarSources().add(mainSource);

        AnchorPane.setTopAnchor(calendarView, 0.0);
        AnchorPane.setRightAnchor(calendarView, 0.0);
        AnchorPane.setBottomAnchor(calendarView, 0.0);
        AnchorPane.setLeftAnchor(calendarView, 0.0);
        calendarContainer.getChildren().add(calendarView);

        calendarContainer.sceneProperty().addListener((obs, oldScene, newScene) -> applyTheme());
        applyTheme();

        newButton.setOnAction(this::onNewEntry);
        importButton.setOnAction(this::onImport);
        exportButton.setOnAction(this::onExport);
        settingsButton.setOnAction(this::onSettings);
        exitButton.setOnAction(evt -> exitAndSave());

        reloadFromStorage();

        Platform.runLater(() -> localizeNode(calendarContainer));
    }

    private void reloadFromStorage() {
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
        }
    }

    private void populateCalendar(List<CalendarEntry> items) {
        ZoneId zone = ZoneId.systemDefault();
        for (CalendarEntry ce : items) {
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
            return cal;
        });
    }

    private void persistEntries() {
        try {
            ConfigUtil.ensureCalendarFile();
            IcsUtil.exportIcs(calendarFile, new ArrayList<>(currentEntries));
        } catch (Exception ex) {
            showError("Kalender konnte nicht gespeichert werden", ex);
        }
    }

    private void onSettings(ActionEvent event) {
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
                    applyTheme();
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
    }

    private void onImport(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(all);
        File file = chooser.showOpenDialog(calendarContainer.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            List<CalendarEntry> imported = IcsUtil.importAuto(file.toPath());
            currentEntries.addAll(imported);
            persistEntries();
            reloadFromStorage();
        } catch (Exception ex) {
            showError("Import fehlgeschlagen", ex);
        }
    }

    private void onExport(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("calendar-export.ics");
        FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("Kalenderdateien (*.ics, *.vcs)", "*.ics", "*.vcs");
        FileChooser.ExtensionFilter ics = new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics");
        FileChooser.ExtensionFilter vcs = new FileChooser.ExtensionFilter("vCalendar (*.vcs)", "*.vcs");
        chooser.getExtensionFilters().addAll(all, ics, vcs);
        chooser.setSelectedExtensionFilter(ics);
        File file = chooser.showSaveDialog(calendarContainer.getScene().getWindow());
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
            if (out.toString().toLowerCase().endsWith(".vcs")) {
                IcsUtil.exportVcs(out, currentEntries);
            } else {
                IcsUtil.exportIcs(out, currentEntries);
            }
        } catch (Exception ex) {
            showError("Export fehlgeschlagen", ex);
        }
    }

    private void onNewEntry(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
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
                warning.setHeaderText("Eingaben prüfen");
                applyThemeToDialog(warning.getDialogPane());
                warning.showAndWait();
                return;
            }
            try {
                LocalDateTime start = LocalDateTime.of(startDate.getValue(), parseTime(startTime.getText()));
                LocalDateTime end = LocalDateTime.of(endDate.getValue(), parseTime(endTime.getText()));
                CalendarEntry ce = new CalendarEntry(titleField.getText().trim(), descField.getText(), start, end);
                currentEntries.add(ce);
                persistEntries();
                reloadFromStorage();
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
        persistEntries();
        Platform.exit();
    }

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fehler");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyTheme() {
        if (calendarContainer == null || calendarContainer.getScene() == null) {
            return;
        }
        try {
            String darkCss = getClass().getResource("/dark.css").toExternalForm();
            var sheets = calendarContainer.getScene().getStylesheets();
            sheets.remove(darkCss);
            if (ConfigUtil.isDarkMode()) {
                sheets.add(darkCss);
            }
        } catch (Exception ignored) {
        }
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
}
