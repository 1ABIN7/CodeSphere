package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.model.QuestionVersion;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.QuestionVersionRepository;
import com.CodeSphere.backend.service.QuestionVersionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionVersionServiceImpl implements QuestionVersionService {

    private final QuestionVersionRepository versionRepository;
    private final QuestionBankRepository questionRepository;

    // Called right before saving edits inside QuestionBankService
    @Override
    public void createVersionSnapshot(Question question) {
        Integer latestVersion = versionRepository.findMaxVersionNumberByQuestionId(question.getId());
        int nextVersionNumber = (latestVersion == null ? 0 : latestVersion) + 1;

        QuestionVersion snapshot = new QuestionVersion(
                question,
                nextVersionNumber,
                question.getTitle(),
                question.getContent()
        );
        versionRepository.save(snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionVersion> getVersionHistory(Long questionId) {
        // Ensure the question actually exists first
        if (!questionRepository.existsById(questionId)) {
            throw new EntityNotFoundException("Question not found with id: " + questionId);
        }
        return versionRepository.findByQuestionIdOrderByVersionNumberDesc(questionId);
    }

    @Override
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