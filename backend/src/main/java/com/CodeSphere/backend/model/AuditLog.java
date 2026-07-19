package com.CodeSphere.backend.model;

import com.CodeSphere.backend.security.AesEncryptor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit log entity for system and administrative security activity tracking.
 */
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Convert(converter = AesEncryptor.class)
    @Column(name = "ip_address")
    private String ipAddress;
}