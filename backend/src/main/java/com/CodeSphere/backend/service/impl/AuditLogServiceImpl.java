package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.AuditLog;
import com.CodeSphere.backend.repository.AuditLogRepository;
import com.CodeSphere.backend.repository.specification.AuditLogSpecification;
import com.CodeSphere.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
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
                .timestamp(Instant.now())
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }

    /**
     * Executes advanced server-side pagination and filtration using specifications.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getFilteredAuditLogs(String username, String action,
                                               Instant startDate, Instant endDate,
                                               Pageable pageable) {

        // Convert Instant -> LocalDateTime expected by AuditLogSpecification
        LocalDateTime startLocal = (startDate != null) ? startDate.atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
        LocalDateTime endLocal = (endDate != null) ? endDate.atZone(ZoneId.systemDefault()).toLocalDateTime() : null;

        Specification<AuditLog> spec = Specification
                .where(AuditLogSpecification.hasUsername(username))
                .and(AuditLogSpecification.hasAction(action))
                .and(AuditLogSpecification.isBetweenDates(startLocal, endLocal));

        return auditLogRepository.findAll(spec, pageable);
    }
}