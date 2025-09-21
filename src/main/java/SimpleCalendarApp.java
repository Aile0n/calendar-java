
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.calendarfx.view.CalendarView;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

public class SimpleCalendarApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        CalendarView calendarView = new CalendarView();

        // Create a custom toolbar
        ToolBar toolBar = new ToolBar();

        // Create settings button
        Button settingsButton = new Button();
        settingsButton.setTooltip(new Tooltip("Einstellungen"));

        // Try to load icon from resources: /icons/settings.png
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
            Stage dialog = new Stage();
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Einstellungen");
            VBox box = new VBox(new Label("Hier können Einstellungen vorgenommen werden."));
            box.setSpacing(10);
            box.setStyle("-fx-padding: 20;");
            dialog.setScene(new Scene(box, 300, 120));
            dialog.showAndWait();
        });

        toolBar.getItems().add(settingsButton);

        // Place toolbar above calendar view
        VBox root = new VBox(toolBar, calendarView);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Mein kleiner Kalender");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}