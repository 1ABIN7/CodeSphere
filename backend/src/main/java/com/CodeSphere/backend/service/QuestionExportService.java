package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionExportService {

    private final ObjectMapper objectMapper;

    public QuestionExportService() {
        this.objectMapper = new ObjectMapper();
    }

    public byte[] exportQuestions(List<Question> questions, String format) {
        if ("json".equalsIgnoreCase(format)) {
            return exportToJson(questions);
        } else if ("csv".equalsIgnoreCase(format)) {
            return exportToCsv(questions);
        } else {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }

    private byte[] exportToJson(List<Question> questions) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(questions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JSON export payload", e);
        }
    }

    private byte[] exportToCsv(List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        // CSV Header row
        sb.append("Title,Content,Category,Type,Difficulty,Tags\n");

        for (Question q : questions) {
            sb.append(escapeCsvField(q.getTitle())).append(",")
                    .append(escapeCsvField(q.getContent())).append(",")
                    .append(escapeCsvField(q.getCategory() != null ? q.getCategory().toString() : "")).append(",")
                    .append(escapeCsvField(q.getType())).append(",")
                    .append(escapeCsvField(q.getDifficulty())).append(",")
                    .append(escapeCsvField(q.getTags() != null ? String.join(";", q.getTags()) : ""))
                    .append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        String clean = field.trim();
        // If field contains quotes, commas, or line breaks, escape them per RFC 4180
        if (clean.contains(",") || clean.contains("\"") || clean.contains("\n") || clean.contains("\r")) {
            return "\"" + clean.replace("\"", "\"\"") + "\"";
        }
        return clean;
    }
}