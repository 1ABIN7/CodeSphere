package com.CodeSphere.backend.model;

import com.CodeSphere.backend.util.AesEncryptor;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Audit log entity for system and admin activity tracking.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String username;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resource;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private Instant timestamp;

    @Convert(converter = AesEncryptor.class)
    @Column(name = "ip_address")
    private String ipAddress;

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
        if (this.resourceType == null) {
            this.resourceType = "SYSTEM"; // Fallback default if not specified
        }
    }
}