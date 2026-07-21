package com.CodeSphere.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Alias getter to satisfy controllers/services expecting getUsername()
     */
    public String getUsername() {
        return usernameOrEmail;
    }
}