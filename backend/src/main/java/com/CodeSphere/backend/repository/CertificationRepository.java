package com.CodeSphere.backend.repository;
import com.CodeSphere.backend.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CertificationRepository extends JpaRepository<Certification,Long>{ List<Certification> findByUserIdOrderByIssuedAtDesc(Long userId); Optional<Certification> findByUserIdAndAssessmentId(Long userId,Long assessmentId); Optional<Certification> findByVerificationCode(String verificationCode); }
