package com.profiledekho.app.repository;

import com.profiledekho.app.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    Optional<UserProfile> findByUsernameIgnoreCase(String username);

    default Optional<UserProfile> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return findByUsernameIgnoreCase(username.trim());
    }

    void deleteByUsernameIgnoreCase(String username);

    default boolean deleteByUsername(String username) {
        if (username == null) return false;
        Optional<UserProfile> existing = findByUsernameIgnoreCase(username.trim());
        if (existing.isPresent()) {
            delete(existing.get());
            return true;
        }
        return false;
    }
}
