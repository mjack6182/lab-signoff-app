package com.example.lab_signoff_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the Lab Signoff Backend.
 *
 * This application provides a backend service for managing lab assignments,
 * student groups, and LTI integration with Canvas LMS.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@SpringBootApplication
public class LabSignoffBackendApplication {

	/**
	 * Application entry point.
	 *
	 * @param args Command-line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(LabSignoffBackendApplication.class, args);
	}

}
