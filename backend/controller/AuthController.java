package com.codesphere.backend.controller;

import com.codesphere.backend.dto.AuthResponse;
import com.codesphere.backend.dto.LoginRequest;
import com.codesphere.backend.dto.RegisterRequest;
import com.codesphere.backend.dto.ForgotPasswordRequest;
import com.codesphere.backend.dto.ResetPasswordRequest;
import com.codesphere.backend.service.AuthService;
import com.codesphere.backend.service.RateLimiterService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {

        // 1. Extract client IP address (handles reverse proxies like Nginx/Cloudflare if present)
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        // 2. Evaluate Rate Limit
        if (!rateLimiterService.isAllowed(ipAddress)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body("Too many login attempts. Please try again after 1 minute.");
        }

        // 3. Process normal authentication logic
        AuthResponse authResponse = authService.login(loginRequest);
        String token = authResponse.getToken();

        // 4. Enforce secure, HTTPS-only cookie attributes
        Cookie jwtCookie = new Cookie("AUTH_TOKEN", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(3600); // 1 hour expiry
        response.addCookie(jwtCookie);

        // Standard header modification fallback to ensure cross-browser SameSite support
        response.addHeader("Set-Cookie", "AUTH_TOKEN=" + token + "; Path=/; Max-Age=3600; HttpOnly; Secure; SameSite=Strict");

        return ResponseEntity.ok("Authentication Successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletResponse response) {
        authService.logout();
        SecurityContextHolder.clearContext();

        // Clear out the cookie from the browser on logout
        Cookie cookie = new Cookie("AUTH_TOKEN", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        response.addHeader("Set-Cookie", "AUTH_TOKEN=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict");

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

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully! You can now log in.");
    }
}