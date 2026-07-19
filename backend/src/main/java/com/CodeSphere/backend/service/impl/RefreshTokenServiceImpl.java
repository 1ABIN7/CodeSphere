package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.RefreshToken;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.service.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Override
    public RefreshToken createRefreshToken(Long userId) {
        // Your logic...
        return null;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        // Return matching token instance from DB layer
        return Optional.empty();
    }

    @Override
    public boolean isExpired(RefreshToken token) {
        // Evaluate instance expiration dates safely
        return token.getExpiryDate().isBefore(Instant.now());
    }

    @Override
    public int deleteByUser(User user) {
        return deleteByUserId(user.getId());
    }

    @Override
    public int deleteByUserId(Long userId) {
        return 0;
    }
}