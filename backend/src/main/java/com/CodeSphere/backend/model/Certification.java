package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "certifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cert_title", nullable = false)
    private String certTitle;

    @Column(name = "issued_by", nullable = false)
    private String issuedBy;

    @Column(name = "verification_code", nullable = false, unique = true)
    private String verificationCode;

    @Column(name = "exam_session_id")
    private Long examSessionId;

    @Column(name = "score")
    private Double score;

    @Column(name = "is_valid", nullable = false)
    @Builder.Default
    private boolean isValid = true;

    @Column(name = "issued_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime issuedAt = OffsetDateTime.now();
}
