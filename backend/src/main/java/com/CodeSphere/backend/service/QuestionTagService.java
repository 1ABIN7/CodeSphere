package com.CodeSphere.backend.service;

import java.util.List;
import java.util.Map;

public interface QuestionTagService {

    List<String> autocompleteTags(String prefix);

    List<Map.Entry<String, Long>> getTrendingTags(int limit);
}