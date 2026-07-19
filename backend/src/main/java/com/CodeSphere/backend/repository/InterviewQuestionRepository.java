package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.Difficulty;
import com.CodeSphere.backend.model.interview.InterviewQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    Page<InterviewQuestion> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    @Query(value = "SELECT * FROM interview_questions WHERE category_id IN :categoryIds AND is_active = true ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<InterviewQuestion> findRandomQuestionsByCategories(@Param("categoryIds") List<Long> categoryIds, @Param("limit") int limit);
}
