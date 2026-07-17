package com.codesphere.backend.controller;

import com.codesphere.backend.dto.AuthResponse;
import com.codesphere.backend.dto.LoginRequest;
import com.codesphere.backend.dto.RegisterRequest;
import com.codesphere.backend.dto.ForgotPasswordRequest;
import com.codesphere.backend.dto.ResetPasswordRequest;
import com.codesphere.backend.service.AuthService;
import com.codesphere.backend.service.LoginAttemptService;
import com.codesphere.backend.service.RateLimiterService;
import com.codesphere.backend.service.JwtService;
import com.codesphere.backend.service.TokenDenylistService;
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
    private final LoginAttemptService loginAttemptService;
    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
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
                    .body(String.format("This account has been locked out due to multiple failed login attempts. Please try again in %d minutes.", waitMinutes));
        }

        try {
            // 3. Process Authentication
            String userAgent = request.getHeader("User-Agent");
            AuthResponse authResponse = authService.login(loginRequest);

            // Generate a token bound specifically to the current request's User-Agent
            String token = jwtService.generateToken(username, userAgent);

            // Reset failed counter on successful verification
            loginAttemptService.loginSucceeded(username);

            // 4. Issue Secure HTTP-Only Cookie
            Cookie jwtCookie = new Cookie("AUTH_TOKEN", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(3600); // 1 hour token lifespan
            response.addCookie(jwtCookie);

            // Enforce explicit SameSite=Strict configuration mapping
            response.addHeader("Set-Cookie", "AUTH_TOKEN=" + token + "; Path=/; Max-Age=3600; HttpOnly; Secure; SameSite=Strict");

            return ResponseEntity.ok("Authentication Successful");

        } catch (Exception e) {
            // Log failed attempt to progress towards lockout thresholds
            loginAttemptService.loginFailed(username);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        // 1. Extract existing token from cookie
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 2. Denylist token globally in Redis
        if (token != null) {
            try {
                String tokenId = jwtService.extractTokenId(token);
                long expiryLeft = jwtService.getRemainingExpiryTimeMs(token);
                tokenDenylistService.denylistToken(tokenId, expiryLeft);
            } catch (Exception e) {
                // Ignore parsing errors for malformed tokens
            }
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

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request);
        return ResponseEntity.ok("Password reset token generated successfully.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.processResetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully.");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully! You can now log in.");
    }
}