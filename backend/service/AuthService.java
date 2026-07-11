package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.AuthResponse;
import com.CodeSphere.backend.dto.LoginRequest;
import com.CodeSphere.backend.dto.RegisterRequest;
import com.CodeSphere.backend.model.Role;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService userDetailsService;

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new RuntimeException("Error: User not found!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Error: Invalid password!");
        }

        // Create an Authentication object for the token provider
        org.springframework.security.core.userdetails.UserDetails userDetails =
                userDetailsService.loadUserByUsername(request.getUsernameOrEmail());

        org.springframework.security.core.Authentication authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        // Generate the actual RS256 JWT Token
        String token = jwtTokenProvider.generateToken(authentication);

        com.CodeSphere.backend.model.RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new AuthResponse(token, refreshToken.getToken(), user.getUsername(), user.getRole().name());
    }

    public void logout() {
        // Handled client-side by dropping token, server-side context clear in controller
    }

    public void processForgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: No account found with this email."));

        // Generate a secure random token valid for 15 minutes
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(Instant.now().plusSeconds(900)); // 15 mins

        userRepository.save(user);

        // In production, you would trigger an email service here to send the token link.
        System.out.println("Password Reset Token generated for " + user.getEmail() + " -> " + token);
    }

    public void processResetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Error: Invalid or expired reset token."));

        // Verify expiration window
        if (user.getResetPasswordTokenExpiry().isBefore(Instant.now())) {
            user.setResetPasswordToken(null);
            user.setResetPasswordTokenExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("Error: Reset token has expired.");
        }

        // Encrypt new password and clear out token fields
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);

        userRepository.save(user);
    }
}