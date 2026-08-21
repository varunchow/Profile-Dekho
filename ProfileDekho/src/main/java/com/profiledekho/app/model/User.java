package com.profiledekho.app.model;

import java.time.Instant;
import java.util.UUID;

public class User {
    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private String provider; // "local", "google", "github"
    private String name;
    private String avatar;
    private String createdAt;
    private String lastLogin;

    public User() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now().toString();
        this.lastLogin = Instant.now().toString();
    }

    public User(String username, String email, String passwordHash, String provider, String name) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.provider = provider;
        this.name = name != null ? name : username;
        this.avatar = "https://api.dicebear.com/7.x/initials/svg?seed=" + username + "&backgroundColor=4A7FD4&textColor=ffffff";
        this.createdAt = Instant.now().toString();
        this.lastLogin = Instant.now().toString();
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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
}
