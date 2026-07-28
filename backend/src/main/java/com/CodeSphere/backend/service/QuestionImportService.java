package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionImportService {

    List<Question> importBulkQuestions(MultipartFile file);
}