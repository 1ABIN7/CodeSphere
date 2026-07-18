package com.codesphere.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Added import
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Added annotation to enable role-based method security
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Enable CORS using our custom configuration source
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Disable CSRF since we are using state-minimized JWTs (optionally secure via SameSite cookies)
                .csrf(csrf -> csrf.disable())

                // 3. Set session management to stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Configure Endpoint Authorizations
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Allow authentication routes
                        .anyRequest().authenticated()               // Secure all other endpoints
                )

                // 5. Inject our custom JWT and Session Verification Filter
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Defines strict CORS rules, locking down origins, headers, and methods.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Strictly restrict to your configured frontend domain
        configuration.setAllowedOrigins(List.of(allowedOrigin));

        // Explicitly declare allowed REST methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Explicitly declare allowed request headers (avoid wildcard '*' if credentials are true)
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "User-Agent"));

        // Allow the browser to send/receive secure HTTP-Only cookies
        configuration.setAllowCredentials(true);

        // Cache CORS preflight responses for 1 hour to reduce overhead traffic
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}