package com.calendar.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

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
@ComponentScan(basePackages = {"com.calendar.web"})
public class CalendarWebApp {
    public static void main(String[] args) {
        SpringApplication.run(CalendarWebApp.class, args);
    }
}
