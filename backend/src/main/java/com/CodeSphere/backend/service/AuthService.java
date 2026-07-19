package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.*;

/**
 * Consolidated Authentication Service interface managing system identity validation pipelines.
 */
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshTokenStr);
    void verifyEmail(String token);
    void processForgotPassword(ForgotPasswordRequest request);
    void processResetPassword(ResetPasswordRequest request);
    void logout();
}