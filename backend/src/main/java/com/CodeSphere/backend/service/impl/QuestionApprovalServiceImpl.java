package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.ApprovalStatus;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.service.QuestionApprovalService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionApprovalServiceImpl implements QuestionApprovalService {

    private final QuestionBankRepository repository;

    @Override
    public Question submitForApproval(Long id) {
        Question question = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        question.setStatus(ApprovalStatus.PENDING_APPROVAL);
        question.setRejectionFeedback(null); // Clear any old feedback
        return repository.save(question);
    }

    @Override
    public Question approveQuestion(Long id) {
        Question question = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        question.setStatus(ApprovalStatus.APPROVED);
        question.setRejectionFeedback(null);
        return repository.save(question);
    }

    @Override
    public Question rejectQuestion(Long id, String feedback) {
        Question question = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        question.setStatus(ApprovalStatus.REJECTED);
        question.setRejectionFeedback(feedback);
        return repository.save(question);
    }
}