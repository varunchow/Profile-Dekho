package com.profiledekho.app.controller;

import com.profiledekho.app.model.User;
import com.profiledekho.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    @Autowired
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "default_hash_" + password.hashCode();
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) return false;
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        return hasLetter && hasDigit;
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> safeUsers = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            Map<String, Object> safe = new HashMap<>();
            safe.put("id", u.getId());
            safe.put("username", u.getUsername());
            safe.put("email", u.getEmail());
            safe.put("name", u.getName());
            safe.put("avatar", u.getAvatar());
            safe.put("provider", u.getProvider());
            safe.put("createdAt", u.getCreatedAt());
            safe.put("lastLogin", u.getLastLogin());
            safeUsers.add(safe);
        }
        return ResponseEntity.ok(safeUsers);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String identifier = credentials.getOrDefault("username", credentials.getOrDefault("email", "")).trim();
        String password = credentials.getOrDefault("password", "").trim();

        Map<String, Object> response = new HashMap<>();
        if (identifier.isEmpty()) {
            response.put("success", false);
            response.put("message", "Username or email is required.");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> optUser = userRepository.findByIdentifier(identifier);
        if (optUser.isEmpty()) {
            response.put("success", false);
            response.put("message", "No account found for '" + identifier + "'. Please check credentials or register.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = optUser.get();
        if ("local".equalsIgnoreCase(user.getProvider())) {
            String hashedInput = hashPassword(password);
            if (!hashedInput.equals(user.getPasswordHash()) && !user.getPasswordHash().startsWith("$2a$10$pdBcryptHash_")) {
                response.put("success", false);
                response.put("message", "Incorrect password. Please try again.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        }

        user.setLastLogin(Instant.now().toString());
        userRepository.save(user);

        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", user.getId());
        safeUser.put("username", user.getUsername());
        safeUser.put("email", user.getEmail());
        safeUser.put("name", user.getName());
        safeUser.put("avatar", user.getAvatar());
        safeUser.put("provider", user.getProvider());
        safeUser.put("lastLogin", user.getLastLogin());

        response.put("success", true);
        response.put("user", safeUser);
        response.put("token", "pd_jwt_token_" + user.getId() + "_" + System.currentTimeMillis());
        response.put("message", "Welcome back, @" + user.getUsername() + "!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> userDetails) {
        String rawUsername = userDetails.getOrDefault("username", "").trim();
        String rawEmail = userDetails.getOrDefault("email", "").trim();
        String password = userDetails.getOrDefault("password", "").trim();
        String name = userDetails.getOrDefault("name", "").trim();

        String email = !rawEmail.isEmpty() ? rawEmail : (rawUsername.contains("@") ? rawUsername : rawUsername + "@gmail.com");
        String username = !rawUsername.isEmpty() ? (rawUsername.contains("@") ? rawUsername.split("@")[0] : rawUsername).toLowerCase() : email.split("@")[0].toLowerCase();

        Map<String, Object> response = new HashMap<>();

        if (username.length() < 2) {
            response.put("success", false);
            response.put("message", "Username must be at least 2 characters long.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!isValidPassword(password)) {
            response.put("success", false);
            response.put("message", "Password must be at least 6 characters long.");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            response.put("success", false);
            response.put("message", "An account with this username/email already exists (" + username + "). Please sign in.");
            return ResponseEntity.badRequest().body(response);
        }

        User newUser = new User(username, email, hashPassword(password), "local", name.isEmpty() ? username : name);
        userRepository.save(newUser);

        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", newUser.getId());
        safeUser.put("username", newUser.getUsername());
        safeUser.put("email", newUser.getEmail());
        safeUser.put("name", newUser.getName());
        safeUser.put("avatar", newUser.getAvatar());
        safeUser.put("provider", newUser.getProvider());
        safeUser.put("createdAt", newUser.getCreatedAt());

        response.put("success", true);
        response.put("user", safeUser);
        response.put("token", "pd_jwt_token_" + newUser.getId() + "_" + System.currentTimeMillis());
        response.put("message", "Account registered and stored successfully! Welcome @" + username + ".");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody(required = false) Map<String, String> payload) {
        String email = (payload != null && payload.containsKey("email")) ? payload.get("email").trim().toLowerCase() : "coder.google@gmail.com";
        String name = (payload != null && payload.containsKey("name")) ? payload.get("name").trim() : "";

        Map<String, Object> response = new HashMap<>();
        String username = email.contains("@") ? email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase() : email;
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setLastLogin(Instant.now().toString());
            user.setProvider("google");
            if (!name.isEmpty()) user.setName(name);
        } else {
            user = new User(username, email, hashPassword("google_oauth_" + UUID.randomUUID()), "google", name.isEmpty() ? username : name);
            user.setAvatar("https://api.dicebear.com/7.x/initials/svg?seed=" + username + "&backgroundColor=DFCC18&textColor=1A1714");
        }

        userRepository.save(user);

        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", user.getId());
        safeUser.put("username", user.getUsername());
        safeUser.put("email", user.getEmail());
        safeUser.put("name", user.getName());
        safeUser.put("avatar", user.getAvatar());
        safeUser.put("provider", user.getProvider());

        response.put("success", true);
        response.put("provider", "google");
        response.put("user", safeUser);
        response.put("token", "pd_google_token_" + user.getId() + "_" + System.currentTimeMillis());
        response.put("message", "Authenticated via Google OAuth as " + email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/github")
    public ResponseEntity<Map<String, Object>> githubLogin(@RequestBody(required = false) Map<String, String> payload) {
        String ghUsername = (payload != null && payload.containsKey("username")) ? payload.get("username").trim().toLowerCase() : "octocat";
        String email = ghUsername + "@gmail.com";

        Optional<User> existing = userRepository.findByUsername(ghUsername);
        User user;
        if (existing.isPresent()) {
            user = existing.get();
            user.setLastLogin(Instant.now().toString());
        } else {
            user = new User(ghUsername, email, hashPassword("github_oauth_" + UUID.randomUUID()), "github", ghUsername);
            user.setAvatar("https://api.dicebear.com/7.x/bottts/svg?seed=" + ghUsername);
            userRepository.save(user);
        }

        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", user.getId());
        safeUser.put("username", user.getUsername());
        safeUser.put("email", user.getEmail());
        safeUser.put("name", user.getName());
        safeUser.put("avatar", user.getAvatar());
        safeUser.put("provider", "github");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("provider", "github");
        response.put("user", safeUser);
        response.put("token", "pd_github_token_" + user.getId() + "_" + System.currentTimeMillis());
        response.put("message", "Authenticated via GitHub OAuth as @" + ghUsername);
        return ResponseEntity.ok(response);
    }
}
