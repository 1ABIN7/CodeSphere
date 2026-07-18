package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    Optional<Certification> findByVerificationCode(String verificationCode);
    List<Certification> findByUserId(Long userId);
}
