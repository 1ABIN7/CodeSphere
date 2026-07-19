package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.model.ApprovalStatus;
import com.CodeSphere.backend.repository.QuestionRepository;
import com.CodeSphere.backend.repository.specification.QuestionSpecification;
import com.CodeSphere.backend.service.QuestionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionServiceImpl(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> getFilteredQuestions(
            String category, String type, String difficulty, List<String> tags, String search) {

        if (search != null && !search.isBlank()) {
            List<Question> searchResults = questionRepository.searchByTsVector(search.trim());

            return searchResults.stream()
                    .filter(q -> category == null || q.getCategory().equalsIgnoreCase(category))
                    .filter(q -> type == null || q.getType().equalsIgnoreCase(type))
                    .filter(q -> difficulty == null || q.getDifficulty().equalsIgnoreCase(difficulty))
                    .filter(q -> tags == null || q.getTags().containsAll(tags))
                    .collect(Collectors.toList());
        }

        Specification<Question> spec = QuestionSpecification.filterQuestions(category, type, difficulty, tags);
        return questionRepository.findAll(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));
    }

    @Override
    public Question updateQuestion(Long id, Question questionDetails) {
        Question existingQuestion = getQuestionById(id);

        existingQuestion.setTitle(questionDetails.getTitle());
        existingQuestion.setContent(questionDetails.getContent());
        existingQuestion.setCategory(questionDetails.getCategory());
        existingQuestion.setType(questionDetails.getType());
        existingQuestion.setDifficulty(questionDetails.getDifficulty());
        existingQuestion.setTags(questionDetails.getTags());

        return questionRepository.save(existingQuestion);
    }

    @Override
    public void deleteQuestion(Long id) {
        Question question = getQuestionById(id);
        questionRepository.delete(question);
    }

    @Override
    public void updateStatus(Long id, String status) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));

        // Fixed: safely convert String payload to ApprovalStatus Enum type
        question.setStatus(ApprovalStatus.valueOf(status.toUpperCase()));
        questionRepository.save(question);
    }
}