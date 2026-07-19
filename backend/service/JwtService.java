package com.codesphere.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long jwtExpiration;

    /**
     * Generates a token with a unique ID (jti) and binds the User-Agent signature.
     */
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

    public boolean isTokenValid(String token, String actualUserAgent) {
        try {
            Claims claims = extractAllClaims(token);

            // 1. Verify basic expiration
            if (claims.getExpiration().before(new Date())) {
                return false;
            }

            // 2. Enforce User-Agent Binding
            String tokenUas = claims.get("uas", String.class);
            String currentUas = hashUserAgent(actualUserAgent);
            return currentUas.equals(tokenUas);

        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractTokenId(String token) {
        return extractAllClaims(token).get("jti", String.class);
    }

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