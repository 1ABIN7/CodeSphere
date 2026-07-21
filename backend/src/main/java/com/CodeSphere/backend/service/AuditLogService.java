package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface AuditLogService {

    void logAction(Long userId, String action, String resource, HttpServletRequest request);

    Page<AuditLog> getFilteredAuditLogs(String username, String action,
                                        Instant startDate, Instant endDate,
                                        Pageable pageable);
}