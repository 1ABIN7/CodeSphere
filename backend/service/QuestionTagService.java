package com.codesphere.backend.service;

import com.codesphere.backend.repository.TagRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuestionTagService {

    private final TagRepository tagRepository;

    public QuestionTagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    // Fetches autocompletion recommendations based on a partial query string
    public List<String> autocompleteTags(String prefix) {
        if (prefix == null || prefix.trim().length() < 1) {
            return List.of();
        }
        return tagRepository.findTagsStartingWith(prefix.trim());
    }

    // Fetches the top N most frequent tags across the entire question bank
    public List<Map.Entry<String, Long>> getTrendingTags(int limit) {
        return tagRepository.getTagUsageFrequency().entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())) // Descending order
                .limit(limit)
                .collect(Collectors.toList());
    }
}