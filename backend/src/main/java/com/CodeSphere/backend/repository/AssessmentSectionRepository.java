package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.entity.AssessmentSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssessmentSectionRepository extends JpaRepository<AssessmentSection, Long> {
    List<AssessmentSection> findByAssessmentIdOrderBySectionOrderAsc(Long assessmentId);
    void deleteByAssessmentId(Long assessmentId);
}
