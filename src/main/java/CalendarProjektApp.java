import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.calendarfx.view.CalendarView;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Calendar;
import com.calendarfx.model.Entry;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.nio.file.Files;

/**
 * Hauptanwendung des Projekts. Startet die JavaFX-Oberfläche, lädt Termine
 * aus der konfigurierten Quelle (Datenbank oder ICS) und stellt Funktionen wie
 * Import/Export, Einstellungen sowie eine Info-Anzeige bereit. Außerdem wird die
 * Oberfläche auf Deutsch lokalisiert.
 */
public class CalendarProjektApp extends Application {
    private final CalendarEntryDAO dao = new CalendarEntryDAO();
    private final Calendar fxCalendar = new Calendar("Termine");
    private final java.util.List<CalendarEntry> currentEntries = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        // Locale auf Deutsch setzen, bevor UI erzeugt wird
        java.util.Locale.setDefault(java.util.Locale.GERMANY);
        java.util.Locale.setDefault(java.util.Locale.Category.FORMAT, java.util.Locale.GERMANY);

        CalendarView calendarView = new CalendarView();

        // Add calendar source and our calendar
        CalendarSource source = new CalendarSource("Meine Kalender");
        source.getCalendars().add(fxCalendar);
        calendarView.getCalendarSources().add(source);

        // Load entries from DB into the CalendarFX calendar
        loadFromDatabase();

        // Create a custom toolbar
        ToolBar toolBar = new ToolBar();

        // Settings button
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

            javafx.scene.control.ToggleGroup group = new javafx.scene.control.ToggleGroup();
            javafx.scene.control.RadioButton rbIcs = new javafx.scene.control.RadioButton("Speichern als ICS");
            javafx.scene.control.RadioButton rbDb = new javafx.scene.control.RadioButton("Speichern in Datenbank");
            rbIcs.setToggleGroup(group);
            rbDb.setToggleGroup(group);
            rbIcs.setSelected(ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.ICS);
            rbDb.setSelected(ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB);

            TextField icsPathField = new TextField(ConfigUtil.getIcsPath().toString());
            Button browse = new Button("…");
            browse.setOnAction(ev -> {
                FileChooser chooser2 = new FileChooser();
                chooser2.setInitialFileName(icsPathField.getText().isBlank() ? "calendar.ics" : icsPathField.getText());
                File f = chooser2.showSaveDialog(((Button) ev.getSource()).getScene().getWindow());
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

            // Dark mode toggle
            Label displayLbl = new Label("Darstellung:");
            CheckBox darkMode = new CheckBox("Dunkelmodus");
            darkMode.setSelected(ConfigUtil.isDarkMode());
            grid.add(displayLbl, 0, 2);
            grid.add(darkMode, 1, 2);

            dialog.getDialogPane().setContent(grid);

            var res = dialog.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                try {
                    ConfigUtil.setStorageMode(rbIcs.isSelected() ? ConfigUtil.StorageMode.ICS : ConfigUtil.StorageMode.DB);
                    if (rbIcs.isSelected()) {
                        ConfigUtil.setIcsPath(new java.io.File(icsPathField.getText()).toPath());
                    }
                    // Save dark mode and apply immediately
                    ConfigUtil.setDarkMode(darkMode.isSelected());
                    applyTheme(primaryStage.getScene());

                    ConfigUtil.save();
                    loadFromDatabase();
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Einstellungen gespeichert.", ButtonType.OK);
                    a.setHeaderText(null);
                    a.showAndWait();
                } catch (Exception ex) {
                    showError("Konnte Einstellungen nicht speichern", ex);
                }
            }
        });

        // Create new event button
        Button createBtn = new Button("Neuer Termin");
        createBtn.setOnAction(e -> showCreateDialog(primaryStage));

        // Import/Export buttons
        Button importBtn = new Button("Importieren (ICS/VCS)");
        importBtn.setOnAction(e -> doImport(primaryStage));
        Button exportBtn = new Button("Exportieren (ICS/VCS)");
        exportBtn.setOnAction(e -> doExport(primaryStage));

        toolBar.getItems().addAll(settingsButton, createBtn, importBtn, exportBtn);

        // Place toolbar above calendar view and add footer with Info button
        Button infoBtn = new Button("Info");
        infoBtn.setTooltip(new Tooltip("Informationen zu Bibliotheken und Autor"));
        infoBtn.setOnAction(e -> showInfoDialog(primaryStage));
        ToolBar footer = new ToolBar(infoBtn);

        VBox root = new VBox(toolBar, calendarView, footer);
        VBox.setVgrow(calendarView, Priority.ALWAYS);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        applyTheme(scene);
        primaryStage.setTitle("CalendarProjekt");
        primaryStage.show();

        // Localize CalendarFX built-in controls (e.g., Today/Day/Week/Month/Year, Search)
        Platform.runLater(() -> localizeNode(root));
    }

    private void loadFromDatabase() {
        fxCalendar.clear();
        if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
            try {
                List<CalendarEntry> items = dao.findAll();
                ZoneId zone = ZoneId.systemDefault();
                for (CalendarEntry ce : items) {
                    Entry<String> entry = new Entry<>(ce.getTitle());
                    entry.setLocation(ce.getDescription());
                    entry.setInterval(ce.getStart().atZone(zone), ce.getEnd().atZone(zone));
                    fxCalendar.addEntry(entry);
                }
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
                ZoneId zone = ZoneId.systemDefault();
                for (CalendarEntry ce : currentEntries) {
                    Entry<String> entry = new Entry<>(ce.getTitle());
                    entry.setLocation(ce.getDescription());
                    entry.setInterval(ce.getStart().atZone(zone), ce.getEnd().atZone(zone));
                    fxCalendar.addEntry(entry);
                }
            } catch (Exception ex) {
                showError("Fehler beim Laden aus ICS", ex);
            }
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
        if (file == null) return;
        try {
            List<CalendarEntry> imported = IcsUtil.importAuto(file.toPath());
            if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
                for (CalendarEntry ce : imported) {
                    dao.save(ce);
                }
            } else {
                currentEntries.addAll(imported);
                IcsUtil.exportIcs(ConfigUtil.getIcsPath(), currentEntries);
            }
            loadFromDatabase();
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
        if (file == null) return;
        try {
            List<CalendarEntry> items;
            if (ConfigUtil.getStorageMode() == ConfigUtil.StorageMode.DB) {
                items = dao.findAll();
            } else {
                items = new ArrayList<>(currentEntries);
            }
            java.nio.file.Path out = file.toPath();
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
                IcsUtil.exportVcs(out, items);
            } else {
                IcsUtil.exportIcs(out, items);
            }
        } catch (Exception ex) {
            showError("Export fehlgeschlagen", ex);
        }
    }

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Fehler");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    private void showInfoDialog(Stage owner) {
        StringBuilder sb = new StringBuilder();
        sb.append("CalendarProjekt\n\n");
        sb.append("Verwendete Bibliotheken:\n");
        sb.append("- CalendarFX 12.0.1\n");
        sb.append("- JavaFX 22.0.1\n");
        sb.append("- sqlite-jdbc 3.42.0.0\n");
        sb.append("- ical4j 3.2.7 (mit Kompatibilitäts-Shim für Frequency)\n\n");
        sb.append("Autor: Florian Knittel\n");

        Alert alert = new Alert(AlertType.INFORMATION);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("Info");
        alert.setHeaderText("Über dieses Projekt");
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    private void localizeNode(Node node) {
        if (node == null) return;
        if (node instanceof ButtonBase) {
            ButtonBase b = (ButtonBase) node;
            String t = b.getText();
            if (t != null) {
                switch (t) {
                    case "Today": b.setText("Heute"); break;
                    case "Day": b.setText("Tag"); break;
                    case "Week": b.setText("Woche"); break;
                    case "Month": b.setText("Monat"); break;
                    case "Year": b.setText("Jahr"); break;
                    case "Print": b.setText("Drucken"); break;
                    case "Add Calendar": b.setText("Kalender hinzufügen"); break;
                }
            }
        }
        if (node instanceof TextField) {
            TextField tf = (TextField) node;
            String p = tf.getPromptText();
            if (p != null && p.equals("Search")) {
                tf.setPromptText("Suche");
            }
        }
        // Translate non-button labels like "Calendars"
        if (node instanceof javafx.scene.control.Labeled) {
            javafx.scene.control.Labeled l = (javafx.scene.control.Labeled) node;
            String t2 = l.getText();
            if (t2 != null) {
                if (t2.equals("Calendars")) l.setText("Kalender");
            }
        }
        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            for (Node child : parent.getChildrenUnmodifiable()) {
                localizeNode(child);
            }
        }
    }

    private void showCreateDialog(Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
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

        // Validate before closing on save
        var saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            if (!validateInputs(titleField.getText(), startDate.getValue(), startTime.getText(), endDate.getValue(), endTime.getText())) {
                evt.consume();
                Alert a = new Alert(AlertType.WARNING, "Bitte Titel angeben und gültige Zeiten im Format HH:mm eingeben. Ende muss nach Start liegen.", ButtonType.OK);
                a.initOwner(owner);
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
                loadFromDatabase();
            } catch (Exception ex) {
                evt.consume();
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

    private void applyTheme(Scene scene) {
        if (scene == null) return;
        try {
            String darkCss = getClass().getResource("/dark.css").toExternalForm();
            var stylesheets = scene.getStylesheets();
            stylesheets.remove(darkCss);
            if (ConfigUtil.isDarkMode()) {
                stylesheets.add(darkCss);
            }
        } catch (Exception ignored) {
            // ignore if stylesheet not found
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}