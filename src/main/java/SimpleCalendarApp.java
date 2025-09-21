import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.calendarfx.view.CalendarView;

public class SimpleCalendarApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        CalendarView calendarView = new CalendarView();
        Scene scene = new Scene(calendarView, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Mein kleiner Kalender");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
