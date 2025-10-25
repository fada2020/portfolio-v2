package com.smartwork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * SmartWork Main Application Entry Point.
 * Enterprise intranet system for workflow management.
 */
@SpringBootApplication
@EnableJpaAuditing
public class SmartWorkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartWorkApplication.class, args);
    }
}
