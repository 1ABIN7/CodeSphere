package com.codesphere.backend.repository;

import com.codesphere.backend.model.QuestionVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface QuestionVersionRepository extends JpaRepository<QuestionVersion, Long> {

    // Get version history sorted chronologically downward
    List<QuestionVersion> findByQuestionIdOrderByVersionNumberDesc(Long questionId);

    // Find a specific version snapshot for a question
    Optional<QuestionVersion> findByQuestionIdAndVersionNumber(Long questionId, Integer versionNumber);

    // Find the highest current version number to figure out the next increment
    @Query("SELECT COALESCE(MAX(qv.versionNumber), 0) FROM QuestionVersion qv WHERE qv.question.id = :questionId")
    Integer findMaxVersionNumberByQuestionId(@Param("questionId") Long questionId);
}