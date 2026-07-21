package com.CodeSphere.backend.repository.specification;

import com.codesphere.backend.model.Question;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class QuestionSpecification {

    public static Specification<Question> filterQuestions(
            String category, String type, String difficulty, List<String> tags) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }

            if (type != null && !type.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (difficulty != null && !difficulty.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("difficulty"), difficulty));
            }

            if (tags != null && !tags.isEmpty()) {
                // Assumes a Many-to-Many or ElementCollection mapping for tags
                Join<Object, String> tagJoin = root.join("tags");
                predicates.add(tagJoin.in(tags));
                // If needed strict "contains all tags", change logic to loop and create multiple joins
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}