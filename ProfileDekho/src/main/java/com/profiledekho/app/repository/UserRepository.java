package com.profiledekho.app.repository;

import com.profiledekho.app.model.User;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {

    private final Map<String, User> userStorage = new ConcurrentHashMap<>();

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

    public List<User> findAll() {
        return new ArrayList<>(userStorage.values());
    }

    public boolean existsByUsername(String username) {
        return username != null && userStorage.containsKey(username.toLowerCase());
    }
}
