package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.service.TokenDenylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenDenylistServiceImpl implements TokenDenylistService {

    private final StringRedisTemplate redisTemplate;

    private static final String DENYLIST_PREFIX = "jwt:denylist:";

    /**
     * Denylists a token's JTI until it would naturally expire.
     */
    @Override
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
    @Override
    @Transactional(readOnly = true)
    public boolean isDenylisted(String tokenId) {
        if (tokenId == null) return true;
        return Boolean.TRUE.equals(redisTemplate.hasKey(DENYLIST_PREFIX + tokenId));
    }
}