package com.profiledekho.app.model;

import java.util.UUID;

public class User {
    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private String provider; // "local", "google", "github"

    public User() {}

    public User(String username, String email, String passwordHash, String provider) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.provider = provider;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
