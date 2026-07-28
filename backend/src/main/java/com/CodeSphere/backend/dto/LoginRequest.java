package com.CodeSphere.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @JsonAlias({"username", "email", "usernameOrEmail"})
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Alias getter to satisfy controllers/services expecting getUsername()
     * Marked with @JsonIgnore so Jackson doesn't clash with getUsernameOrEmail()
     */
    @JsonIgnore
    public String getUsername() {
        return usernameOrEmail;
    }
}