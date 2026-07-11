package com.codesphere.backend.service;

import com.codesphere.backend.model.Question;
import com.codesphere.backend.repository.QuestionBankRepository; // Updated import
import com.codesphere.backend.repository.specification.QuestionSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionBankService {

    private final QuestionBankRepository questionBankRepository; // Updated variable

    // Updated constructor
    public QuestionBankService(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

    public Question createQuestion(Question question) {
        return questionBankRepository.save(question);
    }

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

    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        return questionBankRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + id));
    }

    public Question updateQuestion(Long id, Question questionDetails) {
        Question existingQuestion = getQuestionById(id);

        existingQuestion.setTitle(questionDetails.getTitle());
        existingQuestion.setContent(questionDetails.getContent());
        existingQuestion.setCategory(questionDetails.getCategory());
        existingQuestion.setType(questionDetails.getType());
        existingQuestion.setDifficulty(questionDetails.getDifficulty());
        existingQuestion.setTags(questionDetails.getTags());

        return questionBankRepository.save(existingQuestion);
    }

    public void deleteQuestion(Long id) {
        Question question = getQuestionById(id);
        questionBankRepository.delete(question);
    }
}