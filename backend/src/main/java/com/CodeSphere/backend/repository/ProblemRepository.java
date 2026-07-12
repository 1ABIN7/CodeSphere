package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.Difficulty;
import com.CodeSphere.backend.model.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Problem entity operations.
 * Supports pagination, filtering by difficulty/tags, and full-text search.
 */
@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Page<Problem> findByIsPublishedTrue(Pageable pageable);

    Page<Problem> findByDifficultyAndIsPublishedTrue(Difficulty difficulty, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.isPublished = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Problem> searchPublished(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.isPublished = true AND p.difficulty = :difficulty AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Problem> searchPublishedByDifficulty(
            @Param("search") String search,
            @Param("difficulty") Difficulty difficulty,
            Pageable pageable);

    @Query(value = "SELECT p.* FROM problems p WHERE p.is_published = true AND " +
                   "p.tags @> :tag::jsonb",
           nativeQuery = true)
    Page<Problem> findByTagContaining(@Param("tag") String tagJson, Pageable pageable);

    long countByIsPublishedTrue();

    long countByDifficultyAndIsPublishedTrue(Difficulty difficulty);

    List<Problem> findByCreatedBy(Long userId);

    @Query("SELECT p FROM Problem p ORDER BY p.acceptanceRate DESC")
    Page<Problem> findMostAccepted(Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.isPublished = true ORDER BY p.totalSubmissions DESC")
    Page<Problem> findMostPopular(Pageable pageable);

    boolean existsByTitle(String title);
}
