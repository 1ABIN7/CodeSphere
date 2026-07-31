package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionImportService {

    /** Creates rows without an ID and updates matching IDs when updateExisting is true. */
    List<Question> importBulkQuestions(MultipartFile file, boolean updateExisting);
}
