package com.profiledekho.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profiledekho.app.model.UserProfile;
import com.profiledekho.app.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class ProfileStorageService {

    private final String FILE_PATH = "profiles.json";
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private UserProfileRepository userProfileRepository;

    public Map<String, UserProfile> loadAllProfiles() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new HashMap<>();
        }
        try {
            Map<String, UserProfile> map = mapper.readValue(file, new TypeReference<Map<String, UserProfile>>() {});
            map.values().forEach(userProfileRepository::save);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public UserProfile getProfile(String username) {
        if (username == null) return null;
        Optional<UserProfile> cached = userProfileRepository.findByUsername(username);
        if (cached.isPresent()) {
            return cached.get();
        }
        Map<String, UserProfile> profiles = loadAllProfiles();
        return profiles.get(username.toLowerCase());
    }

    public void saveProfile(UserProfile profile) {
        if (profile == null || profile.getUsername() == null) return;
        userProfileRepository.save(profile);
        Map<String, UserProfile> profiles = loadAllProfiles();
        profiles.put(profile.getUsername().toLowerCase(), profile);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), profiles);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
