package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {
    List<AssessmentQuestion> findByAssessmentId(Long assessmentId);
    List<AssessmentQuestion> findBySectionIdOrderByOrderIndexAsc(Long sectionId);
    List<AssessmentQuestion> findByAssessmentIdAndQuestionBankId(Long assessmentId, Long questionBankId);
    void deleteByAssessmentId(Long assessmentId);
    void deleteBySectionId(Long sectionId);
}
