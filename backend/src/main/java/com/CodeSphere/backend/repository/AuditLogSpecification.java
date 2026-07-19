package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class AuditLogSpecification {

    public static Specification<AuditLog> hasUsername(String username) {
        return (root, query, cb) -> username == null || username.trim().isEmpty() ?
                cb.conjunction() : cb.equal(root.get("username"), username);
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, cb) -> action == null || action.trim().isEmpty() ?
                cb.conjunction() : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> isBetweenDates(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return cb.conjunction();
            if (start != null && end != null) return cb.between(root.get("timestamp"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("timestamp"), start);
            return cb.lessThanOrEqualTo(root.get("timestamp"), end);
        };
    }
}