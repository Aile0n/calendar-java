import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * FXML-basierte Startklasse, die die Datei calendar_view.fxml lädt und damit
 * den vorhandenen Controller CalendarProjektController (mit den neuen Debug-Logs)
 * tatsächlich aktiviert.
 */
public class CalendarFxmlApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("[UI_DEBUG] FXML_APP_START | Lade calendar_view.fxml");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/calendar_view.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("CalendarProjekt (FXML)");
        stage.setScene(scene);
        stage.show();
        System.out.println("[UI_DEBUG] FXML_APP_STARTED | Controller=" + loader.getController());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

