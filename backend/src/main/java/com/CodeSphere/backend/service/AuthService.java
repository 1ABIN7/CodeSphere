package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    void verifyEmail(String token);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshTokenStr);

    void logout();

    void processForgotPassword(ForgotPasswordRequest request);

    void processResetPassword(ResetPasswordRequest request);
}