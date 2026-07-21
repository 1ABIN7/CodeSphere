package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.repository.TagRepository;
import com.CodeSphere.backend.service.QuestionTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionTagServiceImpl implements QuestionTagService {

    private final TagRepository tagRepository;

    // Fetches autocompletion recommendations based on a partial query string
    @Override
    @Transactional(readOnly = true)
    public List<String> autocompleteTags(String prefix) {
        if (prefix == null || prefix.trim().length() < 1) {
            return List.of();
        }
        return tagRepository.findTagsStartingWith(prefix.trim());
    }

    // Fetches the top N most frequent tags across the entire question bank
    @Override
    @Transactional(readOnly = true)
    public List<Map.Entry<String, Long>> getTrendingTags(int limit) {
        return tagRepository.getTagUsageFrequency().entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())) // Descending order
                .limit(limit)
                .collect(Collectors.toList());
    }
}