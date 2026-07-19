package com.CodeSphere.backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String role;
}
