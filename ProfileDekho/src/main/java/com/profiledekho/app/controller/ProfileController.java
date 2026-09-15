package com.profiledekho.app.controller;

import com.profiledekho.app.model.UserProfile;
import com.profiledekho.app.service.CodingPlatformService;
import com.profiledekho.app.service.ProfileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "${app.frontend-url:http://localhost:5173}")
public class ProfileController {

    @Autowired
    private CodingPlatformService codingPlatformService;

    @Autowired
    private ProfileStorageService profileStorageService;

    /**
     * Get all profiles (public, for demo/search)
     */
    @GetMapping
    public ResponseEntity<List<UserProfile>> getAllProfiles() {
        Map<String, UserProfile> all = profileStorageService.loadAllProfiles();
        return ResponseEntity.ok(new ArrayList<>(all.values()));
    }

    /**
     * Get profile by username (public read)
     */
    @GetMapping("/{username}")
    public ResponseEntity<?> getProfileByUsername(@PathVariable String username) {
        UserProfile profile = profileStorageService.getProfile(username);
        if (profile != null) {
            return ResponseEntity.ok(profile);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Fetch and aggregate stats from all platforms
     * Public endpoint - can be called by anyone to trigger a fetch
     */
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

    /**
     * Refresh profile for authenticated user
     * Only accessible to authenticated users
     */
    @PostMapping("/{username}/refresh")
    @PreAuthorize("authentication.name == #username")
    public ResponseEntity<?> refreshProfile(
            @PathVariable String username,
            @RequestBody(required = false) Map<String, String> handles
    ) {
        UserProfile existingProfile = profileStorageService.getProfile(username);
        if (existingProfile == null) {
            return ResponseEntity.notFound().build();
        }

        // Use provided handles or get from existing profile
        String leetcode = (handles != null && handles.containsKey("leetcode")) ? handles.get("leetcode") : existingProfile.getLeetcodeHandle();
        String codeforces = (handles != null && handles.containsKey("codeforces")) ? handles.get("codeforces") : existingProfile.getCodeforcesHandle();
        String codechef = (handles != null && handles.containsKey("codechef")) ? handles.get("codechef") : existingProfile.getCodechefHandle();
        String hackerrank = (handles != null && handles.containsKey("hackerrank")) ? handles.get("hackerrank") : existingProfile.getHackerrankHandle();
        String gfg = (handles != null && handles.containsKey("gfg")) ? handles.get("gfg") : existingProfile.getGfgHandle();
        String github = (handles != null && handles.containsKey("github")) ? handles.get("github") : existingProfile.getGithubHandle();

        // Refresh stats
        UserProfile refreshedProfile = codingPlatformService.fetchAndAggregateStats(
                username, leetcode, codeforces, codechef, hackerrank, gfg, github
        );
        profileStorageService.saveProfile(refreshedProfile);

        return ResponseEntity.ok(refreshedProfile);
    }

    /**
     * Save/update profile
     */
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
