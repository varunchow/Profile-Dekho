package com.profiledekho.app.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseDataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // No hardcoded or dummy users/profiles are seeded.
        // Data is only created when explicitly registered or saved by users.
    }
}
