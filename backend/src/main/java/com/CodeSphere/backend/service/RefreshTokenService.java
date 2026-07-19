package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.RefreshToken;
import com.CodeSphere.backend.model.User;
import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);

    // Added methods matching AuthServiceImpl usage requirements:
    Optional<RefreshToken> findByToken(String token);
    boolean isExpired(RefreshToken token);
    int deleteByUser(User user);
    int deleteByUserId(Long userId);
}