package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.RefreshToken;
import com.CodeSphere.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token entity by its raw string token value.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Purges all active refresh tokens assigned to a target user profile.
     * Used during forced logouts or security resets.
     */
    @Modifying
    int deleteByUser(User user);
}