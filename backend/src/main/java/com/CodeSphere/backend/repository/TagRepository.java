package com.CodeSphere.backend.repository;

import com.codesphere.backend.model.Question;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class TagRepository {

    private final QuestionBankRepository questionRepository;

    public TagRepository(QuestionBankRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    // Finds unique tags starting with a specific prefix (Case-Insensitive)
    public List<String> findTagsStartingWith(String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return questionRepository.findAll().stream()
                .filter(q -> q.getTags() != null)
                .flatMap(q -> q.getTags().stream())
                .map(String::trim)
                .filter(tag -> tag.toLowerCase().startsWith(lowerPrefix))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // Returns a frequency map of all tags used across all questions
    public Map<String, Long> getTagUsageFrequency() {
        return questionRepository.findAll().stream()
                .filter(q -> q.getTags() != null)
                .flatMap(q -> q.getTags().stream())
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
    }
}