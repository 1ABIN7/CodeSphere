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
@Tag(name = "Authentication", description = "User registration, secure JWT authentication management, and account recovery")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final LoginAttemptService loginAttemptService;
    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    @Operation(summary = "Secure login via HttpOnly cookies with brute-force protection")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {

        String identityKey = loginRequest.getUsernameOrEmail();

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

        // 2. Evaluate Account Lockout (Credential stuffing defense)
        if (loginAttemptService.isLockedOut(identityKey)) {
            long waitMinutes = loginAttemptService.getRemainingLockoutMinutes(identityKey);
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(String.format("This account has been locked out due to multiple failed login attempts. Please try again in %d minutes.", waitMinutes));
        }

        try {
            // 3. Process Authentication
            String userAgent = request.getHeader("User-Agent");
            AuthResponse authResponse = authService.login(loginRequest);

            // Generate a token bound specifically to the current request's User-Agent
            String token = jwtService.generateToken(authResponse.getUsername(), userAgent);

            // Reset failed counter on successful verification
            loginAttemptService.loginSucceeded(identityKey);

            // [Audit Log] Record successful login activity
            Long userId = authResponse.getUserId();
            auditLogService.logAction(userId, "LOGIN", "System Auth Successful", request);

            // 4. Issue Secure HTTP-Only Cookie
            Cookie jwtCookie = new Cookie("AUTH_TOKEN", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(3600); // 1 hour token lifespan
            response.addCookie(jwtCookie);

            // Enforce explicit SameSite=Strict configuration mapping
            response.addHeader("Set-Cookie", "AUTH_TOKEN=" + token + "; Path=/; Max-Age=3600; HttpOnly; Secure; SameSite=Strict");

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            // Log failed attempt to progress towards lockout thresholds
            loginAttemptService.loginFailed(identityKey);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username/email or password.");
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out active user, clean authentication context, and denylist session tokens")
    public ResponseEntity<String> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        // 1. Extract existing token from cookie
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

        // 2. Denylist token globally in Redis & resolve context for audit logs
        if (token != null) {
            try {
                String username = jwtService.extractUsername(token);
                userId = userService.getUserIdByUsername(username);

                String tokenId = jwtService.extractTokenId(token);
                long expiryLeft = jwtService.getRemainingExpiryTimeMs(token);
                tokenDenylistService.denylistToken(tokenId, expiryLeft);
            } catch (Exception e) {
                // Ignore parsing errors for malformed tokens during logout pipelines
            }
        }

        // [Audit Log] Record explicit system logout event before destroying authentication context
        if (userId != null) {
            auditLogService.logAction(userId, "LOGOUT", "System Auth Logout", request);
        }

        // 3. Clear auth context & scrub cookie state
        authService.logout();
        SecurityContextHolder.clearContext();

        Cookie cookie = new Cookie("AUTH_TOKEN", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        response.addHeader("Set-Cookie", "AUTH_TOKEN=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict");

        return ResponseEntity.ok("User logged out successfully!");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Trigger a password recovery request and generate token mapping")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request);
        return ResponseEntity.ok("Password reset token generated successfully.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset account credential mapping via token confirmation")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.processResetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully.");
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify user account activation token")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully! You can now log in.");
    }

    @GetMapping("/health")
    @Operation(summary = "API health check (public)")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CodeSphere API is running");
    }
}