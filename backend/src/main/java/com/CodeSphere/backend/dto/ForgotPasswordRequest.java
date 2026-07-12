package com.CodeSphere.backend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ForgotPasswordRequest {
    private String email;
}
