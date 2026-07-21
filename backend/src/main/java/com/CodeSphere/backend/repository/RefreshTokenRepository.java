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
     * Finds a refresh token by its token string.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes all refresh tokens assigned to a specific user.
     * Note: Make sure to call this inside a `@Transactional` service method!
     */
    @Modifying
    int deleteByUser(User user);
}