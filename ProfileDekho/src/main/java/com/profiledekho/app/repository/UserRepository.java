package com.profiledekho.app.repository;

import com.profiledekho.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    default Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return findByUsernameIgnoreCase(username.trim());
    }

    default Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return findByEmailIgnoreCase(email.trim());
    }

    default Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return Optional.empty();
        String clean = identifier.trim();
        Optional<User> byUsername = findByUsernameIgnoreCase(clean);
        if (byUsername.isPresent()) return byUsername;
        return findByEmailIgnoreCase(clean);
    }

    default boolean existsByUsername(String username) {
        if (username == null) return false;
        return existsByUsernameIgnoreCase(username.trim());
    }

    default boolean existsByEmail(String email) {
        if (email == null) return false;
        return existsByEmailIgnoreCase(email.trim());
    }
}
