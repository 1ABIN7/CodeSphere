package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.AuditLog;
import com.CodeSphere.backend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
                .timestamp(Instant.now())
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }
}