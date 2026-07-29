package com.CodeSphere.backend.service;

public interface JwtService {

    String generateToken(String username, String userAgent);

    boolean isTokenValid(String token, String actualUserAgent);

    String extractUsername(String token);

    String extractTokenId(String token);

    String extractRole(String token);

    long getRemainingExpiryTimeMs(String token);
}
