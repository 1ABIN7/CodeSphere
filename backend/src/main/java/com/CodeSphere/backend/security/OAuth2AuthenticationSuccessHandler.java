package com.CodeSphere.backend.security;

import com.CodeSphere.backend.model.RefreshToken;
import com.CodeSphere.backend.model.Role;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        org.springframework.security.core.Authentication authentication) throws java.io.IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        if (email == null || email.isBlank()) throw new IllegalStateException("Google did not provide an email address");
        String displayName = oauthUser.getAttribute("name");
        User user = userRepository.findByEmail(email).orElseGet(() -> createCandidate(email, displayName));
        var appAuthentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new CustomUserDetails(user.getId(), user.getUsername(), user.getPassword(),
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))), null,
                List.of(new SimpleGrantedAuthority(user.getRole().name())));
        String token = jwtTokenProvider.generateToken(appAuthentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        String target = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token).queryParam("refreshToken", refreshToken.getToken())
                .queryParam("username", user.getUsername()).queryParam("role", user.getRole().name()).build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, target);
    }

    private User createCandidate(String email, String displayName) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^A-Za-z0-9_]", "");
        if (base.isBlank()) base = "google_user";
        String username = base;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) username = base + suffix++;
        byte[] random = new byte[32]; new SecureRandom().nextBytes(random);
        return userRepository.save(User.builder().username(username).email(email).fullName(displayName)
                .password(Base64.getUrlEncoder().withoutPadding().encodeToString(random))
                .role(Role.ROLE_CANDIDATE).emailVerified(true).build());
    }
}
