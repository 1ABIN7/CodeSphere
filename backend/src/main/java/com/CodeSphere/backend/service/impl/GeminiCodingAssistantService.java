package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.service.CodingAssistantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GeminiCodingAssistantService implements CodingAssistantService {
    private static final String INSTRUCTIONS = "You are CodeSphere Coding Help, a concise and encouraging programming tutor. "
            + "Help with concepts, debugging strategy, syntax, and small examples. Do not provide solutions, final code, or direct answers for a live assessment, coding test, or interview question. "
            + "Instead, explain the approach and offer a hint. Never claim to run code or access private test cases.";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public GeminiCodingAssistantService(ObjectMapper objectMapper,
                                        @Value("${gemini.api-key:}") String apiKey,
                                        @Value("${gemini.model:gemini-3.6-flash}") String model) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    @Override
    public String answer(String message) {
        return generate(INSTRUCTIONS, message);
    }

    @Override
    public String evaluateCandidateSkills(String candidateName, String submissionSummary) {
        String evaluatorInstructions = "You are an advisory coding-skills analyst for an assessment platform. "
                + "Review only the provided completed-submission data and code excerpts. Be constructive, factual, and concise. "
                + "Do not infer personal traits, seniority, intelligence, or employability. Do not make hiring recommendations. "
                + "Do not alter grades or claim hidden test information. Return plain text with exactly these headings: "
                + "Strengths, Growth areas, Coding style, and Next practice steps. Use short bullet points under each heading.";
        return generate(evaluatorInstructions, "Candidate: " + candidateName + "\n\nCompleted submission data:\n" + submissionSummary);
    }

    private String generate(String instructions, String message) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Coding Help is not configured yet. Add GEMINI_API_KEY to the project .env file, then restart the backend.");
        }
        try {
            var requestBody = objectMapper.createObjectNode();
            requestBody.putObject("system_instruction").putArray("parts").addObject().put("text", instructions);
            requestBody.putArray("contents").addObject().put("role", "user")
                    .putArray("parts").addObject().put("text", message.trim());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                    .timeout(Duration.ofSeconds(35))
                    .header("Content-Type", "application/json")
                    // Gemini's current authorization keys are sent in this header.
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String providerMessage = objectMapper.readTree(response.body()).path("error").path("message").asText("")
                        .replaceAll("[\\r\\n]+", " ").trim();
                if (providerMessage.length() > 220) providerMessage = providerMessage.substring(0, 220) + "…";
                throw new IllegalStateException("Gemini rejected the request (" + response.statusCode() + ")"
                        + (providerMessage.isBlank() ? ". Check the key and project access." : ": " + providerMessage));
            }
            String answer = objectMapper.readTree(response.body()).path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text").asText("").trim();
            if (answer.isBlank()) throw new IllegalStateException("Coding Help returned an empty response. Please try again.");
            return answer;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Coding Help is temporarily unavailable. Please try again.");
        }
    }
}
