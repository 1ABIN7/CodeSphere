package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.service.QuestionService;
import com.CodeSphere.backend.service.QuestionVersionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionBankRepository questionRepository;
    private final QuestionVersionService versionService;

    @Override
    @Transactional(readOnly = true)
    public List<Question> getAllQuestions() {
        return questionRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Override
    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));
    }

    @Override
    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }

    @Override
    public Question updateQuestion(Long id, Question questionDetails) {
        Question existingQuestion = getQuestionById(id);

        // Take a version snapshot using your QuestionVersionService before updating
        versionService.createVersionSnapshot(existingQuestion);

        existingQuestion.setTitle(questionDetails.getTitle());
        existingQuestion.setContent(questionDetails.getContent());

        return questionRepository.save(existingQuestion);
    }

    @Override
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new EntityNotFoundException("Question not found with id: " + id);
        }
        questionRepository.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        Question question = getQuestionById(id);
        question.setStatus(com.CodeSphere.backend.model.ApprovalStatus.valueOf(status));
        questionRepository.save(question);
    }

    @Override
    public List<Question> getFilteredQuestions(String category, String difficulty, String status, List<String> tags, String search) {
        // Filtering will be expanded separately; always return the newest entries first
        // so recently created questions are visible immediately in the bank.
        return questionRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }
}
