package com.CodeSphere.backend.security;

import com.CodeSphere.backend.model.RefreshToken;
import com.CodeSphere.backend.model.Role;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String username = email.substring(0, email.indexOf('@'));
            if (userRepository.existsByUsername(username)) {
                username = username + "_" + System.currentTimeMillis();
            }
            User newUser = User.builder()
                    .username(username)
                    .email(email)
                    .password(null)
                    .role(Role.ROLE_CANDIDATE)
                    .build();
            return userRepository.save(newUser);
        });

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, Collections.emptyList());

        String jwt = jwtTokenProvider.generateToken(auth);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        String redirectUrl = UriComponentsBuilder.fromHttpUrl("http://localhost:5173/auth/callback")
                .queryParam("token", jwt)
                .queryParam("refreshToken", refreshToken.getToken())
                .queryParam("username", user.getUsername())
                .queryParam("role", user.getRole().name())
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }
}
