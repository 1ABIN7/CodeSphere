package com.codesphere.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenDenylistService {

    private final StringRedisTemplate redisTemplate;
    private static final String DENYLIST_PREFIX = "jwt:denylist:";

    public TokenDenylistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Denylists a token's JTI until it would naturally expire.
     */
    public void denylistToken(String tokenId, long expiryDurationMs) {
        if (expiryDurationMs > 0) {
            redisTemplate.opsForValue().set(
                    DENYLIST_PREFIX + tokenId,
                    "revoked",
                    expiryDurationMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Checks if a token ID has been revoked.
     */
    public boolean isDenylisted(String tokenId) {
        if (tokenId == null) return true;
        return Boolean.TRUE.equals(redisTemplate.hasKey(DENYLIST_PREFIX + tokenId));
    }
}