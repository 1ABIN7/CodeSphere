package com.CodeSphere.backend.service;

public interface LoginAttemptService {

    boolean isLockedOut(String username);

    long getRemainingLockoutMinutes(String username);

    void loginSucceeded(String username);

    void loginFailed(String username);
}