package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.AuthResponse;
import com.CodeSphere.backend.dto.LoginRequest;
import com.CodeSphere.backend.dto.RegisterRequest;
import com.CodeSphere.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.CodeSphere.backend.dto.ForgotPasswordRequest;
import com.CodeSphere.backend.dto.ResetPasswordRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser() {
        authService.logout();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("User logged out successfully!");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request);
        return ResponseEntity.ok("Password reset token generated successfully.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.processResetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}