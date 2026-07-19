package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.ApprovalStatus;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuestionApprovalService {

    private final QuestionRepository repository;

    public QuestionApprovalService(QuestionRepository repository) {
        this.repository = repository;
    }

    public Question submitForApproval(Long id) {
        Question question = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        question.setStatus(ApprovalStatus.PENDING_APPROVAL);
        question.setRejectionFeedback(null); // Clear any old feedback
        return repository.save(question);
    }

    public Question approveQuestion(Long id) {
        Question question = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        question.setStatus(ApprovalStatus.APPROVED);
        question.setRejectionFeedback(null);
        return repository.save(question);
    }

    public Question rejectQuestion(Long id, String feedback) {
        Question question = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        question.setStatus(ApprovalStatus.REJECTED);
        question.setRejectionFeedback(feedback);
        return repository.save(question);
    }
}