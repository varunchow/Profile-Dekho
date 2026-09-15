package com.profiledekho.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profiledekho.app.model.UserProfile;
import com.profiledekho.app.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;

@Service
public class ProfileStorageService {

    private final String FILE_PATH = "profiles.json";
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private UserProfileRepository userProfileRepository;

    public Map<String, UserProfile> loadAllProfiles() {
        List<UserProfile> dbProfiles = userProfileRepository.findAll();
        if (!dbProfiles.isEmpty()) {
            Map<String, UserProfile> map = new HashMap<>();
            for (UserProfile p : dbProfiles) {
                if (p.getUsername() != null) {
                    map.put(p.getUsername().toLowerCase(), p);
                }
            }
            return map;
        }

        // If PostgreSQL is fresh and empty, seed from profiles.json
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try {
                Map<String, UserProfile> map = mapper.readValue(file, new TypeReference<Map<String, UserProfile>>() {});
                for (UserProfile p : map.values()) {
                    userProfileRepository.save(p);
                }
                return map;
            } catch (Exception e) {
                // Ignore file read error if unreadable
            }
        }
        return new HashMap<>();
    }

    public UserProfile getProfile(String username) {
        if (username == null || username.trim().isEmpty()) return null;
        Optional<UserProfile> dbProfile = userProfileRepository.findByUsername(username.trim());
        if (dbProfile.isPresent()) {
            return dbProfile.get();
        }
        // Check if unseeded profiles exist
        Map<String, UserProfile> profiles = loadAllProfiles();
        return profiles.get(username.trim().toLowerCase());
    }

    @Transactional
    public void saveProfile(UserProfile profile) {
        if (profile == null || profile.getUsername() == null || profile.getUsername().trim().isEmpty()) return;
        userProfileRepository.save(profile);
    }
}
