package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.model.QuestionVersion;

import java.util.List;

public interface QuestionVersionService {

    void createVersionSnapshot(Question question);

    List<QuestionVersion> getVersionHistory(Long questionId);

    Question restoreToVersion(Long questionId, Integer versionNumber);
}