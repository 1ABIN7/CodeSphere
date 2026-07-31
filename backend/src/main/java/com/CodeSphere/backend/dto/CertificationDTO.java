package com.CodeSphere.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificationDTO {
    private Long certificationId;
    private String title;
    private String certificateUrl;
    private Instant issuedAt;
    private String verificationCode;
    private Double score;
}
