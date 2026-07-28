package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.RefreshToken;
import com.CodeSphere.backend.model.User;

import java.util.Optional;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(Long userId);

    RefreshToken createRefreshToken(User user);

    Optional<RefreshToken> findByToken(String token);

    RefreshToken verifyExpiration(RefreshToken token);

    boolean isExpired(RefreshToken token);

    int deleteByUserId(Long userId);

    void deleteByUser(User user);
}