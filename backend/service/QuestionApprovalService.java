package com.codesphere.backend.service;

import com.codesphere.backend.model.ApprovalStatus;
import com.codesphere.backend.model.Question;
import com.codesphere.backend.repository.QuestionBankRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuestionApprovalService {

    private final QuestionBankRepository repository;

    public QuestionApprovalService(QuestionBankRepository repository) {
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