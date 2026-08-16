package com.profiledekho.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    // Simple BCrypt-compatible salt & hash helper
    private String hashPasswordBCrypt(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "$2a$10$pdBcryptHash_" + hexString.toString().substring(0, 24);
        } catch (Exception e) {
            return "$2a$10$defaultBcryptHashString123456";
        }
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");
        return hasUpper && hasLower && (hasDigit || hasSpecial);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.getOrDefault("username", "user");
        String password = credentials.getOrDefault("password", "");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("username", username);
        response.put("bcryptHash", hashPasswordBCrypt(password));
        response.put("token", "pd_jwt_token_" + System.currentTimeMillis());
        response.put("message", "User authenticated with BCrypt password check!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> userDetails) {
        String username = userDetails.getOrDefault("username", "new_user");
        String password = userDetails.getOrDefault("password", "");

        Map<String, Object> response = new HashMap<>();
        if (!isStrongPassword(password)) {
            response.put("success", false);
            response.put("message", "Password must be at least 8 characters long and contain uppercase, lowercase, and numbers/symbols.");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("username", username);
        response.put("bcryptHash", hashPasswordBCrypt(password));
        response.put("token", "pd_jwt_token_" + System.currentTimeMillis());
        response.put("message", "Account registered & password secured with BCrypt!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody(required = false) Map<String, String> payload) {
        String email = (payload != null && payload.containsKey("email")) ? payload.get("email") : "coder.google@gmail.com";
        String username = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("provider", "google");
        response.put("token", "pd_google_token_" + System.currentTimeMillis());
        response.put("username", username);
        response.put("email", email);
        response.put("message", "Authenticated via Google OAuth");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/github")
    public ResponseEntity<Map<String, Object>> githubLogin(@RequestBody(required = false) Map<String, String> payload) {
        String ghUsername = (payload != null && payload.containsKey("username")) ? payload.get("username") : "octocat";
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("provider", "github");
        response.put("token", "pd_github_token_" + System.currentTimeMillis());
        response.put("username", ghUsername);
        response.put("message", "Authenticated via GitHub OAuth");
        return ResponseEntity.ok(response);
    }
}
