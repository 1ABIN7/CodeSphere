package com.CodeSphere.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origin:http://localhost:5173}")
    private String allowedOrigin;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ---- Public Endpoints ----
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/problems/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ---- Problem Management (create/update/delete = admin/examiner) ----
                        .requestMatchers(HttpMethod.POST, "/api/problems/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN", "EXAMINER")
                        .requestMatchers(HttpMethod.PUT, "/api/problems/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN", "EXAMINER")
                        .requestMatchers(HttpMethod.PATCH, "/api/problems/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN", "EXAMINER")
                        .requestMatchers(HttpMethod.DELETE, "/api/problems/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN")

                        // ---- Submissions ----
                        .requestMatchers("/api/submissions/**").authenticated()

                        // ---- Interview Module ----
                        .requestMatchers(HttpMethod.GET, "/api/interview/categories").permitAll()
                        .requestMatchers("/api/interview/**").authenticated()

                        // ---- File Storage ----
                        .requestMatchers("/api/files/**").authenticated()

                        // ---- Admin & Role Endpoints ----
                        .requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/org/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN")
                        .requestMatchers("/api/exams/manage/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN", "EXAMINER")
                        .requestMatchers("/api/courses/**").hasAnyRole("SUPER_ADMIN", "ORG_ADMIN", "INSTRUCTOR")
                        .requestMatchers("/api/candidate/**").hasRole("CANDIDATE")

                        // ---- Default ----
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allows both local dev URLs and property-based origin
        configuration.setAllowedOrigins(List.of(
                allowedOrigin,
                "http://localhost:5173",
                "http://localhost:3000"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Cache-Control",
                "X-Requested-With", "Accept", "Origin"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}