package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.*;
import com.CodeSphere.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "User registration, login, JWT management, and password recovery")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final LoginAttemptService loginAttemptService;
    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    @GetMapping("/health")
    @Operation(summary = "API health check (public)")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CodeSphere API is running");
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain authenticated session / JWT cookie")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {

        String username = loginRequest.getUsername();

        // 1. Evaluate Rate Limiter (IP-based brute-force/DoS defense)
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        if (!rateLimiterService.isAllowed(ipAddress)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body("Too many login attempts from this IP. Please try again after 1 minute.");
        }

        // 2. Evaluate Account Lockout (Username-based credential stuffing defense)
        if (loginAttemptService.isLockedOut(username)) {
            long waitMinutes = loginAttemptService.getRemainingLockoutMinutes(username);
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(String.format("Account locked due to multiple failed attempts. Try again in %d minutes.", waitMinutes));
        }

        try {
            // 3. Process Authentication
            String userAgent = request.getHeader("User-Agent");
            AuthResponse authResponse = authService.login(loginRequest);

            String token = jwtService.generateToken(username, userAgent);
            loginAttemptService.loginSucceeded(username);

            // Audit Log
            auditLogService.logAction(authResponse.getUserId(), "LOGIN", "System Auth", request);

            // 4. Set Secure SameSite=Strict HTTP-Only Cookie
            response.addHeader("Set-Cookie", "AUTH_TOKEN=" + token + "; Path=/; Max-Age=3600; HttpOnly; Secure; SameSite=Strict");

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            loginAttemptService.loginFailed(username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.");
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate token")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = null;
        Long userId = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            try {
                String username = jwtService.extractUsername(token);
                userId = userService.getUserIdByUsername(username);

                String tokenId = jwtService.extractTokenId(token);
                long expiryLeft = jwtService.getRemainingExpiryTimeMs(token);
                tokenDenylistService.denylistToken(tokenId, expiryLeft);
            } catch (Exception ignored) {
                // Ignore token parsing errors during logout
            }
        }

        if (userId != null) {
            auditLogService.logAction(userId, "LOGOUT", "System Auth", request);
        }

        authService.logout();
        SecurityContextHolder.clearContext();

        // Expire cookie
        response.addHeader("Set-Cookie", "AUTH_TOKEN=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict");

        return ResponseEntity.ok("User logged out successfully!");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset token")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request);
        return ResponseEntity.ok("Password reset token generated successfully.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using reset token")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.processResetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully.");
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify account email address")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully! You can now log in.");
    }
}