package com.CodeSphere.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginRequest {
    @NotBlank
    private String usernameOrEmail;
    @NotBlank
    private String password;
}
