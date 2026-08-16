package com.profiledekho.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProfileDekhoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileDekhoApplication.class, args);
        System.out.println("==================================================");
        System.out.println(" ProfileDekho Spring Boot Backend Started ");
        System.out.println(" Server running on http://localhost:8080");
        System.out.println("==================================================");
    }
}
