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
        List<Question> passageQuestions = new java.util.ArrayList<>(question.getSubQuestions());
        // Save the passage first, then explicitly attach its nested questions. This avoids
        // relying on a lazy one-to-many join column during JSON request binding.
        question.setSubQuestions(new java.util.ArrayList<>());
        Question created = questionRepository.save(question);
        savePassageQuestions(created.getId(), passageQuestions);
        return created;
    }

    @Override
    public Question updateQuestion(Long id, Question questionDetails) {
        Question existingQuestion = getQuestionById(id);

        // Take a version snapshot using your QuestionVersionService before updating
        versionService.createVersionSnapshot(existingQuestion);

        existingQuestion.setTitle(questionDetails.getTitle());
        existingQuestion.setContent(questionDetails.getContent());
        existingQuestion.setCategory(questionDetails.getCategory());
        existingQuestion.setType(questionDetails.getType());
        existingQuestion.setQuestionType(questionDetails.getQuestionType());
        existingQuestion.setCodingProblemId(questionDetails.getCodingProblemId());
        existingQuestion.setDifficulty(questionDetails.getDifficulty());
        existingQuestion.setTags(questionDetails.getTags());
        existingQuestion.setOptions(questionDetails.getOptions());
        existingQuestion.setCorrectAnswers(questionDetails.getCorrectAnswers());
        existingQuestion.setPoints(questionDetails.getPoints());
        existingQuestion.setNegativeScore(questionDetails.getNegativeScore());
        existingQuestion.setMinWordCount(questionDetails.getMinWordCount());
        existingQuestion.setMaxWordCount(questionDetails.getMaxWordCount());
        existingQuestion.setPassageText(questionDetails.getPassageText());
        existingQuestion.setReadingDurationSeconds(questionDetails.getReadingDurationSeconds());
        Question updated = questionRepository.save(existingQuestion);
        if (questionDetails.getSubQuestions() != null && !questionDetails.getSubQuestions().isEmpty()) {
            questionRepository.findByParentQuestionId(id).forEach(questionRepository::delete);
            savePassageQuestions(id, questionDetails.getSubQuestions());
        }
        return updated;
    }

    private void savePassageQuestions(Long parentQuestionId, List<Question> passageQuestions) {
        for (Question passageQuestion : passageQuestions) {
            passageQuestion.setId(null);
            passageQuestion.setParentQuestionId(parentQuestionId);
            passageQuestion.setSubQuestions(new java.util.ArrayList<>());
            questionRepository.save(passageQuestion);
        }
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
