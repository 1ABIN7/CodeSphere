package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Certification;
import org.springframework.core.io.Resource;

public interface CertificationService {
    Certification issueCertificate(Long userId, String certTitle, Long examSessionId, Double score);
    Certification verifyCertificate(String verificationCode);
    Resource downloadCertificatePDF(Long certificationId);
}
