package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.AuditLog;
import com.CodeSphere.backend.repository.AuditLogRepository;
import com.CodeSphere.backend.repository.AuditLogSpecification;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logAction(Long userId, String action, String resource, HttpServletRequest request) {
        // Fallback extraction for IP Address behind proxies or load balancers
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .resource(resource)
                .timestamp(LocalDateTime.now())
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }

    /**
     * Executes advanced server-side pagination and filtration using specifications.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getFilteredAuditLogs(String username, String action,
                                               LocalDateTime startDate, LocalDateTime endDate,
                                               Pageable pageable) {

        Specification<AuditLog> spec = Specification
                .where(AuditLogSpecification.hasUsername(username))
                .and(AuditLogSpecification.hasAction(action))
                .and(AuditLogSpecification.isBetweenDates(startDate, endDate));

        return auditLogRepository.findAll(spec, pageable);
    }
}