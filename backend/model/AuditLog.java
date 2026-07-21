package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import com.CodeSphere.backend.util.AesEncryptor;
import java.time.LocalDateTime;



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

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "ip_address")
    private String ipAddress;

    @Entity
    @Table(name = "audit_logs")
    public class AuditLog {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String action;

        private String username;

        @Convert(converter = AesEncryptor.class)
        @Column(name = "ip_address")
        private String ipAddress;

        private LocalDateTime timestamp;

        // Getters, Setters, other fields...
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    }
}