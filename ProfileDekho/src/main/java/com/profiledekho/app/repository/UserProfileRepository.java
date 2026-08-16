package com.profiledekho.app.repository;

import com.profiledekho.app.model.UserProfile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserProfileRepository {

    private final Map<String, UserProfile> profileStorage = new ConcurrentHashMap<>();

    public UserProfile save(UserProfile profile) {
        if (profile != null && profile.getUsername() != null) {
            profileStorage.put(profile.getUsername().toLowerCase(), profile);
        }
        return profile;
    }

    public Optional<UserProfile> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(profileStorage.get(username.toLowerCase()));
    }

    public List<UserProfile> findAll() {
        return new ArrayList<>(profileStorage.values());
    }

    public boolean deleteByUsername(String username) {
        if (username == null) return false;
        return profileStorage.remove(username.toLowerCase()) != null;
    }
}
