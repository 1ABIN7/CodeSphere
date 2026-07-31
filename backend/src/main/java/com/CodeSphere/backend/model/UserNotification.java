package com.CodeSphere.backend.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="user_notifications") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserNotification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="user_id", nullable=false) private Long userId;
 @Column(nullable=false) private String title;
 @Column(columnDefinition="TEXT", nullable=false) private String message;
 @Column(name="is_read", nullable=false) private boolean read;
 @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
}
