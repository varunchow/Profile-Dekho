package com.profiledekho.app.controller;

import com.profiledekho.app.model.User;
import com.profiledekho.app.repository.UserRepository;
import com.profiledekho.app.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.frontend-url:http://localhost:5173}")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) return false;
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        return hasLetter && hasDigit;
    }

    private Map<String, Object> buildSafeUserResponse(User user) {
        Map<String, Object> safe = new HashMap<>();
        safe.put("id", user.getId());
        safe.put("username", user.getUsername());
        safe.put("email", user.getEmail());
        safe.put("name", user.getName());
        safe.put("avatar", user.getAvatar());
        safe.put("provider", user.getProvider());
        safe.put("createdAt", user.getCreatedAt());
        safe.put("lastLogin", user.getLastLogin());
        return safe;
    }

    /**
     * Register a new local account
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> userDetails) {
        String rawUsername = userDetails.getOrDefault("username", "").trim();
        String password = userDetails.getOrDefault("password", "").trim();
        String name = userDetails.getOrDefault("name", "").trim();

        String email = rawUsername.contains("@") ? rawUsername : (rawUsername + "@example.com");
        String username = rawUsername.contains("@") ? rawUsername.split("@")[0] : rawUsername;
        username = username.toLowerCase().replaceAll("[^a-z0-9_]", "_");

        Map<String, Object> response = new HashMap<>();

        if (username.length() < 2) {
            response.put("success", false);
            response.put("message", "Username must be at least 2 characters long.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!isValidPassword(password)) {
            response.put("success", false);
            response.put("message", "Password must be at least 6 characters long and contain letters & numbers.");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            response.put("success", false);
            response.put("message", "An account with this username/email already exists. Please sign in.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setName(name.isEmpty() ? username : name);
        newUser.setProvider("local");
        newUser.setCreatedAt(Instant.now().toString());
        newUser.setLastLogin(Instant.now().toString());

        userRepository.save(newUser);

        // Generate JWT tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("provider", "local");
        claims.put("name", newUser.getName());

        String accessToken = jwtUtil.generateAccessToken(username, claims);
        String refreshToken = jwtUtil.generateRefreshToken(username);

        response.put("success", true);
        response.put("user", buildSafeUserResponse(newUser));
        response.put("token", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("message", "Account created successfully!");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login with username/email and password
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String identifier = credentials.getOrDefault("username", credentials.getOrDefault("email", "")).trim();
        String password = credentials.getOrDefault("password", "").trim();

        Map<String, Object> response = new HashMap<>();

        if (identifier.isEmpty() || password.isEmpty()) {
            response.put("success", false);
            response.put("message", "Username/email and password are required.");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> optUser = userRepository.findByIdentifier(identifier);

        if (optUser.isEmpty()) {
            response.put("success", false);
            response.put("message", "No account found. Please check credentials or register.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = optUser.get();

        // Only allow local password authentication for "local" provider
        if (!"local".equalsIgnoreCase(user.getProvider())) {
            response.put("success", false);
            response.put("message", "Please sign in using " + user.getProvider().toUpperCase());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            response.put("success", false);
            response.put("message", "Incorrect password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        user.setLastLogin(Instant.now().toString());
        userRepository.save(user);

        // Generate JWT tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("provider", "local");
        claims.put("name", user.getName());

        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), claims);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        response.put("success", true);
        response.put("user", buildSafeUserResponse(user));
        response.put("token", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("message", "Welcome back, @" + user.getUsername() + "!");

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.getOrDefault("refreshToken", "").trim();

        Map<String, Object> response = new HashMap<>();

        if (refreshToken.isEmpty() || !jwtUtil.validateToken(refreshToken)) {
            response.put("success", false);
            response.put("message", "Invalid or expired refresh token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String username = jwtUtil.extractUsername(refreshToken);
        Optional<User> optUser = userRepository.findByUsername(username);

        if (optUser.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = optUser.get();
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("provider", user.getProvider());
        claims.put("name", user.getName());

        String newAccessToken = jwtUtil.generateAccessToken(username, claims);

        response.put("success", true);
        response.put("token", newAccessToken);
        response.put("message", "Token refreshed.");

        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        // The authenticated username is available via SecurityContextHolder
        // For now, return a generic response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Use the Authorization header with Bearer token");
        return ResponseEntity.ok(response);
    }
}
