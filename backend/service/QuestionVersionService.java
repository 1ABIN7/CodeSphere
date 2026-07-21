package com.codesphere.backend.service;

import com.codesphere.backend.model.Question;
import com.codesphere.backend.model.QuestionVersion;
import com.codesphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.QuestionVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class QuestionVersionService {

    private final QuestionVersionRepository versionRepository;
    private final QuestionBankRepository questionRepository;

    public QuestionVersionService(QuestionVersionRepository versionRepository, QuestionBankRepository questionRepository) {
        this.versionRepository = versionRepository;
        this.questionRepository = questionRepository;
    }

    // Called right before saving edits inside QuestionBankService
    public void createVersionSnapshot(Question question) {
        Integer latestVersion = versionRepository.findMaxVersionNumberByQuestionId(question.getId());
        QuestionVersion snapshot = new QuestionVersion(
                question,
                latestVersion + 1,
                question.getTitle(),
                question.getContent()
        );
        versionRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public List<QuestionVersion> getVersionHistory(Long questionId) {
        // Ensure the question actually exists first
        if (!questionRepository.existsById(questionId)) {
            throw new EntityNotFoundException("Question not found with id: " + questionId);
        }
        return versionRepository.findByQuestionIdOrderByVersionNumberDesc(questionId);
    }

    public Question restoreToVersion(Long questionId, Integer versionNumber) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

        QuestionVersion historicVersion = versionRepository.findByQuestionIdAndVersionNumber(questionId, versionNumber)
                .orElseThrow(() -> new EntityNotFoundException("Version " + versionNumber + " not found for question id: " + questionId));

        // Before restoring, save the current state as a new version snapshot so nothing is lost
        createVersionSnapshot(question);

        // Roll back the content fields
        question.setTitle(historicVersion.getTitle());
        question.setContent(historicVersion.getContent());

        return questionRepository.save(question);
    }
}