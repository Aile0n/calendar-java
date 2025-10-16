import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Hauptanwendung für die Web-Version der Kalender-Anwendung.
 * Diese Klasse startet einen eingebetteten Webserver und stellt die Kalender-Funktionalität
 * über REST-APIs und eine Web-Oberfläche zur Verfügung.
 * 
 * Verwendung:
 * - Start: mvn spring-boot:run
 * - oder: java -jar target/calendar-java-*.jar
 * - Web-Interface: http://localhost:8080
 */
@SpringBootApplication
public class CalendarWebApp {
    public static void main(String[] args) {
        SpringApplication.run(CalendarWebApp.class, args);
    }
}
