package com.profiledekho.app.repository;

import com.profiledekho.app.model.User;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {

    private final Map<String, User> userStorage = new ConcurrentHashMap<>();

    @PostConstruct
    public void initDefaultUsers() {
        if (userStorage.isEmpty()) {
            save(new User("alex_coder", "alex.coder@gmail.com", "$2a$10$pdBcryptHash_alex1234567890", "local", "Alex Coder"));
            save(new User("tourist", "tourist@gmail.com", "$2a$10$pdBcryptHash_tourist1234567", "google", "Gennady Korotkevich"));
            save(new User("neal_wu", "neal.wu@gmail.com", "$2a$10$pdBcryptHash_neal123456789", "local", "Neal Wu"));
        }
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        userStorage.put(user.getUsername().toLowerCase(), user);
        return user;
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(userStorage.get(username.toLowerCase()));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userStorage.values().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }

    public Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return Optional.empty();
        String clean = identifier.trim();
        Optional<User> byUsername = findByUsername(clean);
        if (byUsername.isPresent()) return byUsername;
        return findByEmail(clean);
    }

    public List<User> findAll() {
        return new ArrayList<>(userStorage.values());
    }

    public boolean existsByUsername(String username) {
        return username != null && userStorage.containsKey(username.toLowerCase());
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
}
