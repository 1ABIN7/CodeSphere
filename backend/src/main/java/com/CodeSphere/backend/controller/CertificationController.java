package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.Certification;
import com.CodeSphere.backend.repository.CertificationRepository;
import com.CodeSphere.backend.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;
    private final CertificationRepository certificationRepository;

    @GetMapping("/verify/{code}")
    public ResponseEntity<Certification> verifyCertificate(@PathVariable String code) {
        return ResponseEntity.ok(certificationService.verifyCertificate(code));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadCertificatePDF(@PathVariable Long id) {
        Resource resource = certificationService.downloadCertificatePDF(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate_" + id + ".pdf\"")
                .body(resource);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Certification>> getUserCertifications(@PathVariable Long userId) {
        return ResponseEntity.ok(certificationRepository.findByUserId(userId));
    }
}
