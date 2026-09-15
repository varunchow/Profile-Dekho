package com.profiledekho.app.controller;

import com.profiledekho.app.model.UserProfile;
import com.profiledekho.app.service.CodingPlatformService;
import com.profiledekho.app.service.ProfileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private CodingPlatformService codingPlatformService;

    @Autowired
    private ProfileStorageService profileStorageService;

    @GetMapping
    public ResponseEntity<List<UserProfile>> getAllProfiles() {
        Map<String, UserProfile> all = profileStorageService.loadAllProfiles();
        return ResponseEntity.ok(new ArrayList<>(all.values()));
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getProfileByUsername(@PathVariable String username) {
        UserProfile profile = profileStorageService.getProfile(username);
        if (profile != null) {
            return ResponseEntity.ok(profile);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/fetch")
    public ResponseEntity<UserProfile> fetchAndAggregate(
            @RequestParam(required = false, defaultValue = "coder") String username,
            @RequestParam(required = false) String leetcode,
            @RequestParam(required = false) String codeforces,
            @RequestParam(required = false) String codechef,
            @RequestParam(required = false) String hackerrank,
            @RequestParam(required = false) String gfg,
            @RequestParam(required = false) String github
    ) {
        UserProfile profile = codingPlatformService.fetchAndAggregateStats(
                username, leetcode, codeforces, codechef, hackerrank, gfg, github
        );
        profileStorageService.saveProfile(profile);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveProfile(@RequestBody UserProfile profile) {
        profileStorageService.saveProfile(profile);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile saved successfully!");
        response.put("username", profile.getUsername());
        return ResponseEntity.ok(response);
    }
}
