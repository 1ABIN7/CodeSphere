package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Looks up a user account by their unique username string handle.
     */
    Optional<User> findByUsername(String username);

    /**
     * Looks up a user account by their unique email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Validates if a specific username string is already registered in the system database.
     */
    Boolean existsByUsername(String username);

    /**
     * Validates if a specific email address is already registered in the system database.
     */
    Boolean existsByEmail(String email);

    /**
     * Finds a user context associated with an active password recovery reset token string.
     */
    Optional<User> findByResetPasswordToken(String token);

    /**
     * Finds a user context associated with a pending email confirmation/activation token string.
     */
    Optional<User> findByEmailVerificationToken(String token);
}