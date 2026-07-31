package com.CodeSphere.backend.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="certifications", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","assessment_id"})) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Certification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="user_id", nullable=false) private Long userId;
 @Column(name="assessment_id", nullable=false) private Long assessmentId;
 @Column(nullable=false) private String title;
 @Column(name="verification_code", nullable=false, unique=true) private String verificationCode;
 @Column(name="score", nullable=false) private Double score;
 @Column(name="issued_at", nullable=false) private OffsetDateTime issuedAt;
}
