package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.access-token-expiration-ms:3600000}")
    private long jwtExpiration;

    /**
     * Generates a token with a unique ID (jti) and binds the User-Agent signature.
     */
    @Override
    public String generateToken(String username, String userAgent) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("uas", hashUserAgent(userAgent)); // User-Agent Signature

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean isTokenValid(String token, String actualUserAgent) {
        try {
            Claims claims = extractAllClaims(token);

            // 1. Verify basic expiration
            if (claims.getExpiration().before(new Date())) {
                log.warn("Token validation failed: Token is expired");
                return false;
            }

            // 2. User-Agent Binding check (Only enforce if 'uas' claim exists in token)
            String tokenUas = claims.get("uas", String.class);
            if (tokenUas != null) {
                String currentUas = hashUserAgent(actualUserAgent);
                if (!currentUas.equals(tokenUas)) {
                    log.warn("Token validation failed: User-Agent signature mismatch");
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public String extractTokenId(String token) {
        return extractAllClaims(token).get("jti", String.class);
    }

    @Override
    public long getRemainingExpiryTimeMs(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Hashes the user agent string to protect user privacy in JWT payloads.
     */
    private String hashUserAgent(String userAgent) {
        if (userAgent == null) {
            userAgent = "unknown";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userAgent.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}