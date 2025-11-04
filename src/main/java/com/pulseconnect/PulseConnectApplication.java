package com.pulseconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Pulse Connect - Smart Blood Donor Directory
 * 
 * @author Pulse Connect Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class PulseConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseConnectApplication.class, args);
        System.out.println("=================================================");
        System.out.println("  Pulse Connect Application Started Successfully");
        System.out.println("  Access at: http://localhost:8081");
        System.out.println("  API Documentation: http://localhost:8081/swagger-ui.html");
        System.out.println("=================================================");
    }
}
