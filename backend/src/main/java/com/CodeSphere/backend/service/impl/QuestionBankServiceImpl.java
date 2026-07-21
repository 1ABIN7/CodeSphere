package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.specification.QuestionSpecification;
import com.CodeSphere.backend.service.QuestionBankService;
import com.CodeSphere.backend.service.QuestionVersionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionVersionService versionService; // Injected missing dependency

    @Override
    public Question createQuestion(Question question) {
        return questionBankRepository.save(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> getFilteredQuestions(
            String category, String type, String difficulty, List<String> tags, String search) {

        if (search != null && !search.isBlank()) {
            List<Question> searchResults = questionBankRepository.searchByTsVector(search.trim());

            return searchResults.stream()
                    .filter(q -> category == null || q.getCategory().equalsIgnoreCase(category))
                    .filter(q -> type == null || q.getType().equalsIgnoreCase(type))
                    .filter(q -> difficulty == null || q.getDifficulty().equalsIgnoreCase(difficulty))
                    .filter(q -> tags == null || q.getTags().containsAll(tags))
                    .collect(Collectors.toList());
        }

        Specification<Question> spec = QuestionSpecification.filterQuestions(category, type, difficulty, tags);
        return questionBankRepository.findAll(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        return questionBankRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));
    }

    @Override
    public Question updateQuestion(Long id, Question questionDetails) {
        Question existingQuestion = getQuestionById(id);

        versionService.createVersionSnapshot(existingQuestion);

        existingQuestion.setTitle(questionDetails.getTitle());
        existingQuestion.setContent(questionDetails.getContent());
        existingQuestion.setCategory(questionDetails.getCategory());
        existingQuestion.setType(questionDetails.getType());
        existingQuestion.setDifficulty(questionDetails.getDifficulty());
        existingQuestion.setTags(questionDetails.getTags());

        return questionBankRepository.save(existingQuestion);
    }

    @Override
    public void deleteQuestion(Long id) {
        Question question = getQuestionById(id);
        questionBankRepository.delete(question);
    }
}