package com.CodeSphere.backend.security;

import com.CodeSphere.backend.service.JwtService;
import com.CodeSphere.backend.service.TokenDenylistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenDenylistService denylistService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Skip JWT validation for public authentication endpoints
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        String userAgent = request.getHeader("User-Agent");

        if (StringUtils.hasText(token)) {
            try {
                String tokenId = null;
                try {
                    tokenId = jwtService.extractTokenId(token);
                } catch (Exception e) {
                    log.warn("Could not extract jti (Token ID) from token: {}", e.getMessage());
                }

                // Check denylist only if a valid tokenId (jti) exists in the token
                boolean isDenylisted = StringUtils.hasText(tokenId) && denylistService.isDenylisted(tokenId);

                // 1. Verify token is not denylisted and is valid
                if (!isDenylisted && jwtService.isTokenValid(token, userAgent)) {
                    String username = jwtService.extractUsername(token);

                    // 2. Load UserDetails to retain roles & authorities
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Successfully authenticated user: {}", username);
                } else {
                    log.warn("Token validation failed. Is Denylisted: {}", isDenylisted);
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception e) {
                log.error("Authentication failed during JWT processing: {}", e.getMessage(), e);
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts token from either HTTP Header or Cookie.
     */
    private String extractToken(HttpServletRequest request) {
        // First check Authorization Header
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Fallback: Check Cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}