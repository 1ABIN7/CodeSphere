package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimiterServiceImpl implements RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 1;

    /**
     * Checks if the incoming IP address has exceeded the login threshold.
     * @return true if the request is allowed, false if it is rate-limited.
     */
    @Override
    public boolean isAllowed(String ipAddress) {
        String key = "rate:login:" + ipAddress;

        // Increment counter atomic operation
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == null) {
            return true;
        }

        // If it's the first request in the window, set the TTL expiration
        if (currentCount == 1) {
            redisTemplate.expire(key, WINDOW_MINUTES, TimeUnit.MINUTES);
        }

        return currentCount <= MAX_ATTEMPTS;
    }
}