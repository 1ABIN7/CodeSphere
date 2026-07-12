package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Problem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for tracking and updating user skill proficiency scores.
 *
 * When a user solves a problem, their proficiency in the problem's
 * skill categories (tags) is updated. The score is computed as a
 * weighted combination of problems solved and difficulty level.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillScoreService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Difficulty-based score multipliers.
     */
    private static final BigDecimal EASY_MULTIPLIER = new BigDecimal("1.0");
    private static final BigDecimal MEDIUM_MULTIPLIER = new BigDecimal("2.0");
    private static final BigDecimal HARD_MULTIPLIER = new BigDecimal("3.5");

    /**
     * Update skill scores when a user gets an accepted submission on a problem.
     *
     * Each of the problem's tags becomes a skill category.
     * The user's proficiency score and problems_solved count
     * are incremented using upsert logic.
     *
     * @param userId  the user ID
     * @param problem the accepted problem
     */
    @Transactional
    public void updateScoresOnAccepted(Long userId, Problem problem) {
        List<String> tags = problem.getTags();
        if (tags == null || tags.isEmpty()) {
            // If no tags, use difficulty as the skill category
            tags = List.of(problem.getDifficulty().name());
        }

        BigDecimal scoreIncrement = getScoreIncrement(problem);

        for (String tag : tags) {
            String skillCategory = tag.toLowerCase().trim();

            // Upsert: insert or update on conflict
            jdbcTemplate.update(
                    "INSERT INTO skill_scores (user_id, skill_category, proficiency_score, problems_solved, last_updated_at) " +
                    "VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP) " +
                    "ON CONFLICT (user_id, skill_category) DO UPDATE SET " +
                    "proficiency_score = skill_scores.proficiency_score + EXCLUDED.proficiency_score, " +
                    "problems_solved = skill_scores.problems_solved + 1, " +
                    "last_updated_at = CURRENT_TIMESTAMP",
                    userId, skillCategory, scoreIncrement
            );
        }

        log.debug("[SkillScore] Updated {} skill categories for user {}: +{} points",
                tags.size(), userId, scoreIncrement);
    }

    private BigDecimal getScoreIncrement(Problem problem) {
        return switch (problem.getDifficulty()) {
            case EASY -> EASY_MULTIPLIER;
            case MEDIUM -> MEDIUM_MULTIPLIER;
            case HARD -> HARD_MULTIPLIER;
        };
    }
}
