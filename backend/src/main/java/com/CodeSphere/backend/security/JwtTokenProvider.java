package com.CodeSphere.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT Token Provider using HS256 (symmetric key).
 *
 * Uses a static key so tokens survive backend restarts during development.
 */
@Component
public class JwtTokenProvider {

    // A static secret key string (must be at least 256 bits / 32 characters long for HS256)
    private static final String SECRET_KEY_STRING = "CodeSphereSecretKeyForDevelopmentOnly1234567890";
    private final SecretKey secretKey;
    
    // Token validity: 24 hours
    private final long jwtExpirationInMs = 86400000;

    public JwtTokenProvider() {
        this.secretKey = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());
    }

    // Generate token using the Secret Key
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract username from token
    public String getUsernameFromJWT(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    // Validate the token signature and expiration
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            System.err.println("JWT Validation Error: " + ex.getMessage());
        }
        return false;
    }
}
