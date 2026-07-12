package com.CodeSphere.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF because we are using stateless JWT tokens
                .csrf(csrf -> csrf.disable())

                // Set session management to stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure endpoint routing rules based on your new roles
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints open to everyone
                        .requestMatchers("/api/auth/**").permitAll()

                        // Role-Based Access Control Restrictions
                        .requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/org/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN")
                        .requestMatchers("/api/exams/manage/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN", "EXAMINER")
                        .requestMatchers("/api/courses/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN", "INSTRUCTOR")
                        .requestMatchers("/api/candidate/**").hasRole("CANDIDATE")

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}