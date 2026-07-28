package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.service.ScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreServiceImpl implements ScoreService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void updateSubmissionScore(Long submissionId, Double newScore) {
        log.info("Admin manual override: updating submission {} score to {}", submissionId, newScore);

        int rowsUpdated = jdbcTemplate.update(
                "UPDATE submissions SET score = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                newScore, submissionId
        );

        if (rowsUpdated == 0) {
            throw new IllegalArgumentException("Submission not found with ID: " + submissionId);
        }
    }
}