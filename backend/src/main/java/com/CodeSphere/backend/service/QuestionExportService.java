package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Question;

import java.util.List;

public interface QuestionExportService {

    byte[] exportQuestions(List<Question> questions, String format);
}