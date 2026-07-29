package com.CodeSphere.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidate_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String name;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "candidate_group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "user_id", nullable = false)
    @Builder.Default private List<Long> memberUserIds = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
