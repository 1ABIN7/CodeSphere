package com.CodeSphere.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    private static final String ATTEMPTS_PREFIX = "login:attempts:";
    private static final String LOCKOUT_PREFIX = "login:lockout:";

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Determines if a username is currently locked out.
     */
    public boolean isLockedOut(String username) {
        String lockoutKey = LOCKOUT_PREFIX + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey));
    }

    /**
     * Retrieves the remaining lockout time in minutes.
     */
    public long getRemainingLockoutMinutes(String username) {
        String lockoutKey = LOCKOUT_PREFIX + username;
        Long expire = redisTemplate.getExpire(lockoutKey, TimeUnit.MINUTES);
        return expire != null && expire > 0 ? expire : 0;
    }

    /**
     * Records a successful login attempt, resetting the user's failure counter.
     */
    public void loginSucceeded(String username) {
        String attemptsKey = ATTEMPTS_PREFIX + username;
        redisTemplate.delete(attemptsKey);
    }

    /**
     * Records a failed login attempt. Increments the counter and applies a lockout if the limit is reached.
     */
    public void loginFailed(String username) {
        if (isLockedOut(username)) {
            return;
        }

        String attemptsKey = ATTEMPTS_PREFIX + username;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);

        if (attempts == null) {
            attempts = 1L;
        }

        // Set an initial TTL on the failure counter if it's the first failure
        if (attempts == 1) {
            redisTemplate.expire(attemptsKey, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        if (attempts >= MAX_ATTEMPTS) {
            String lockoutKey = LOCKOUT_PREFIX + username;
            // Apply the 15-minute lock
            redisTemplate.opsForValue().set(lockoutKey, "locked", LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
            // Clean up the attempts counter
            redisTemplate.delete(attemptsKey);
        }
    }
}