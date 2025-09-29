import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * JavaFX-Controller für die FXML-Ansicht. Verknüpft Buttons und Kalenderansicht
 * (CalendarFX) mit der Anwendungslogik, lädt Daten aus DB bzw. ICS und bietet
 * Aktionen zum Anlegen, Importieren und Exportieren von Terminen sowie Einstellungen.
 */
public class CalendarProjektController implements Initializable {

    @FXML private AnchorPane calendarContainer;
    @FXML private Button newButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private Button settingsButton;

    private CalendarView calendarView;
    private final Calendar fxCalendar = new Calendar("Termine");

    private final CalendarEntryDAO dao = new CalendarEntryDAO();
    private final ZoneId zone = ZoneId.systemDefault();

    // Used in ICS mode
    private final List<CalendarEntry> currentEntries = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        calendarView = new CalendarView();
        CalendarSource source = new CalendarSource("Meine Kalender");
        source.getCalendars().add(fxCalendar);
        calendarView.getCalendarSources().add(source);

        AnchorPane.setTopAnchor(calendarView, 0.0);
        AnchorPane.setRightAnchor(calendarView, 0.0);
        AnchorPane.setBottomAnchor(calendarView, 0.0);
        AnchorPane.setLeftAnchor(calendarView, 0.0);
        calendarContainer.getChildren().add(calendarView);

        // Wire actions
        newButton.setOnAction(this::onNewEntry);
        importButton.setOnAction(this::onImport);
        exportButton.setOnAction(this::onExport);
        settingsButton.setOnAction(this::onSettings);

        // Load initial data (default ICS)
        reloadData();
    }

    private void reloadData() {
        fxCalendar.clear();
        if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
            try {
                List<CalendarEntry> items = dao.findAll();
                populateCalendar(items);
            } catch (Exception ex) {
                showError("Fehler beim Laden aus der Datenbank", ex);
            }
        } else {
            try {
                currentEntries.clear();
                var path = ConfigUtil.getIcsPath();
                if (Files.exists(path)) {
                    currentEntries.addAll(IcsUtil.importIcs(path));
                }
                populateCalendar(currentEntries);
            } catch (Exception ex) {
                showError("Fehler beim Laden aus ICS", ex);
            }
        }
    }

    private void populateCalendar(List<CalendarEntry> items) {
        for (CalendarEntry ce : items) {
            Entry<String> entry = new Entry<>(ce.getTitle());
            if (ce.getDescription() != null && !ce.getDescription().isBlank()) {
                entry.setLocation(ce.getDescription());
            }
            entry.setInterval(ce.getStart().atZone(zone), ce.getEnd().atZone(zone));
            fxCalendar.addEntry(entry);
        }
    }

    private void onNewEntry(ActionEvent evt) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Neuer Termin");
        ButtonType saveType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

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

        var saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (!validateInputs(titleField.getText(), startDate.getValue(), startTime.getText(), endDate.getValue(), endTime.getText())) {
                e.consume();
                Alert a = new Alert(Alert.AlertType.WARNING, "Bitte Titel angeben und gültige Zeiten im Format HH:mm eingeben. Ende muss nach Start liegen.", ButtonType.OK);
                a.setHeaderText("Eingaben prüfen");
                a.showAndWait();
                return;
            }
            try {
                LocalDateTime start = LocalDateTime.of(startDate.getValue(), parseTime(startTime.getText()));
                LocalDateTime end = LocalDateTime.of(endDate.getValue(), parseTime(endTime.getText()));
                CalendarEntry ce = new CalendarEntry(titleField.getText().trim(), descField.getText(), start, end);
                if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
                    dao.save(ce);
                } else {
                    currentEntries.add(ce);
                    IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
                }
                reloadData();
            } catch (Exception ex) {
                e.consume();
                showError("Speichern fehlgeschlagen", (ex instanceof Exception) ? (Exception) ex : new Exception(ex));
            }
        });

        dialog.showAndWait();
    }

    private boolean validateInputs(String title, LocalDate sd, String st, LocalDate ed, String et) {
        if (title == null || title.isBlank() || sd == null || ed == null) return false;
        LocalTime ltStart = parseTime(st);
        LocalTime ltEnd = parseTime(et);
        if (ltStart == null || ltEnd == null) return false;
        LocalDateTime start = LocalDateTime.of(sd, ltStart);
        LocalDateTime end = LocalDateTime.of(ed, ltEnd);
        return end.isAfter(start);
    }

    private LocalTime parseTime(String text) {
        try {
            String t = text == null ? "" : text.trim();
            if (t.matches("^\\d{1}:\\d{2}$")) {
                t = "0" + t; // normalize 9:00 -> 09:00
            }
            return LocalTime.parse(t);
        } catch (Exception e) {
            return null;
        }
    }

    private void onImport(ActionEvent evt) {
        Stage stage = (Stage) calendarContainer.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            List<CalendarEntry> imported = IcsUtil.importIcs(file.toPath());
            if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
                for (CalendarEntry ce : imported) {
                    dao.save(ce);
                }
            } else {
                currentEntries.addAll(imported);
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
            }
            reloadData();
        } catch (Exception ex) {
            showError("Import fehlgeschlagen", ex);
        }
    }

    private void onExport(ActionEvent evt) {
        Stage stage = (Stage) calendarContainer.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("calendar-export.ics");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            List<CalendarEntry> items;
            if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
                items = dao.findAll();
            } else {
                items = new ArrayList<>(currentEntries);
            }
            IcsUtil.exportIcs(file.toPath(), items);
        } catch (Exception ex) {
            showError("Export fehlgeschlagen", ex);
        }
    }

    private void onSettings(ActionEvent evt) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Einstellungen");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ToggleGroup group = new ToggleGroup();
        RadioButton rbIcs = new RadioButton("Speichern als ICS");
        RadioButton rbDb = new RadioButton("Speichern in Datenbank");
        rbIcs.setToggleGroup(group);
        rbDb.setToggleGroup(group);
        rbIcs.setSelected(ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.ICS);
        rbDb.setSelected(ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB);

        TextField icsPathField = new TextField(ConfigUtil.getIcsPath().toString());
        Button browse = new Button("…");
        browse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(icsPathField.getText().isBlank() ? "calendar.ics" : icsPathField.getText());
            File f = chooser.showSaveDialog(dialog.getDialogPane().getScene().getWindow());
            if (f != null) icsPathField.setText(f.getAbsolutePath());
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10;");
        grid.add(new Label("Speicher-Modus:"), 0, 0);
        grid.add(rbIcs, 1, 0);
        grid.add(rbDb, 2, 0);
        grid.add(new Label("ICS-Datei:"), 0, 1);
        grid.add(icsPathField, 1, 1);
        grid.add(browse, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                ConfigUtil.setStorageMode(rbIcs.isSelected() ? ConfigUtil.StorageMode.ICS : ConfigUtil.StorageMode.DB);
                if (rbIcs.isSelected()) {
                    ConfigUtil.setIcsPath(new java.io.File(icsPathField.getText()).toPath());
                }
                ConfigUtil.save();
                reloadData();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Einstellungen gespeichert.", ButtonType.OK);
                a.setHeaderText(null);
                a.showAndWait();
            } catch (Exception ex) {
                showError("Konnte Einstellungen nicht speichern", ex);
            }
        }
    }

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fehler");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }
}
