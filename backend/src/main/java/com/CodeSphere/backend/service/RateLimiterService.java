package com.CodeSphere.backend.service;

public interface RateLimiterService {

    boolean isAllowed(String ipAddress);
}