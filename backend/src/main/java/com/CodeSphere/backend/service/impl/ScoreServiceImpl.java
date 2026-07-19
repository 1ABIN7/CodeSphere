package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.service.ScoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ScoreServiceImpl implements ScoreService {

    @Override
    public void updateSubmissionScore(Long submissionId, Double score) {
        System.out.println("Updating submission " + submissionId + " to score: " + score);
    }
}