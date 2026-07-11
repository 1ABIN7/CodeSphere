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
}