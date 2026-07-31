package com.CodeSphere.backend.seeder;

import com.CodeSphere.backend.model.ApprovalStatus;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Seeds original written, SQL, API, and reading-comprehension assessment questions. */
@Component
@Profile("dev")
@Order(6)
public class OriginalSpecialQuestionSeeder implements CommandLineRunner {
    private final QuestionBankRepository questions;
    public OriginalSpecialQuestionSeeder(QuestionBankRepository questions) { this.questions = questions; }

    @Override public void run(String... args) {
        int added = 0;
        for (int i = 0; i < TOPICS.size(); i++) {
            int number = i + 1; String topic = TOPICS.get(i);
            added += saveIfMissing(written(number, topic));
            added += saveIfMissing(sql(number, topic));
            added += saveIfMissing(api(number, topic));
            added += saveIfMissing(reading(number, topic));
        }
        System.out.printf("[OriginalSpecialQuestionSeeder] %d written, SQL, API, and reading questions added.%n", added);
    }

    private int saveIfMissing(Question question) { if (questions.existsByTitleIgnoreCase(question.getTitle())) return 0; questions.save(question); return 1; }
    private Question base(String title, String content, String type, String category, String difficulty, int points) { Question q = new Question(); q.setTitle(title); q.setContent(content); q.setType(type); q.setQuestionType(type); q.setCategory(category); q.setDifficulty(difficulty); q.setPoints(points); q.setNegativeScore(0); q.setStatus(ApprovalStatus.APPROVED); return q; }

    private Question written(int n, String topic) {
        Question q = base("Written " + n + ": " + topic, "Explain a practical approach to " + topic.toLowerCase() + ". State your assumptions, outline the steps, and mention one trade-off or edge case.", "WRITTEN", "Written response", n % 3 == 0 ? "HARD" : "MEDIUM", 10);
        q.setMinWordCount(80); q.setMaxWordCount(300); return q;
    }

    private Question sql(int n, String topic) {
        Question q = base("SQL " + n + ": " + topic, "Using the employees table, write one read-only SELECT query that returns the name and salary of employees whose salary is at least 120000 for this scenario: " + topic + ". Order results by salary descending.", "SQL", "SQL", n % 3 == 0 ? "HARD" : "MEDIUM", 10);
        q.setSqlSetup("CREATE TABLE employees (id INT, name VARCHAR(80), salary INT, department VARCHAR(80));\nINSERT INTO employees VALUES (1, 'Ada', 120000, 'Engineering'), (2, 'Ben', 90000, 'Support'), (3, 'Cam', 120000, 'Engineering'), (4, 'Dee', 70000, 'Sales');");
        q.setSqlTestCases("[{\"name\":\"Salary threshold\",\"setupSql\":\"\",\"expectedRows\":[{\"name\":\"Ada\",\"salary\":120000},{\"name\":\"Cam\",\"salary\":120000}],\"hidden\":false},{\"name\":\"No qualifying salaries\",\"setupSql\":\"UPDATE employees SET salary = salary - 100000\",\"expectedRows\":[],\"hidden\":true}]");
        return q;
    }

    private Question api(int n, String topic) {
        Question q = base("API " + n + ": " + topic, "Build a Node HTTP service for the " + topic.toLowerCase() + " scenario. It must listen on port 3000 and return JSON from GET /health with an ok field set to true.", "API_IMPLEMENTATION", "API implementation", n % 3 == 0 ? "HARD" : "MEDIUM", 10);
        q.setApiTestCases("[{\"name\":\"Health check\",\"method\":\"GET\",\"path\":\"/health\",\"expectedStatus\":200,\"expectedBody\":\"{\\\"ok\\\":true}\",\"hidden\":false},{\"name\":\"Missing route\",\"method\":\"GET\",\"path\":\"/missing\",\"expectedStatus\":404,\"hidden\":true}]");
        return q;
    }

    private Question reading(int n, String topic) {
        String passage = "A project team is discussing " + topic.toLowerCase() + ". The team agrees that clear goals, small experiments, and measurable feedback reduce uncertainty. They document decisions, review outcomes, and adjust their plan when evidence changes. The approach does not promise instant success, but it helps the team learn before committing more time and resources.";
        Question q = base("Reading " + n + ": " + topic, "Read the passage and answer the passage question.", "READING_COMPREHENSION", "Reading comprehension", "MEDIUM", 5);
        q.setPassageText(passage); q.setReadingDurationSeconds(45); q.setMinWordCount(0); q.setMaxWordCount(250);
        Question child = base("Passage question", "According to the passage, what helps the team reduce uncertainty?", "MCQ_SINGLE", "Reading comprehension", "EASY", 5);
        child.setOptions(List.of("Clear goals, small experiments, and measurable feedback", "Avoiding documentation", "Committing all resources immediately", "Ignoring evidence")); child.setCorrectAnswers("A");
        q.setSubQuestions(new ArrayList<>(List.of(child))); return q;
    }

    private static final List<String> TOPICS = List.of(
        "Requirements Clarification", "User Authentication", "Input Validation", "Error Handling", "Logging Strategy", "Performance Tuning", "Database Indexing", "Caching Policy", "API Versioning", "Rate Limiting",
        "File Uploads", "Data Privacy", "Role Permissions", "Unit Testing", "Integration Testing", "Deployment Planning", "Incident Response", "Code Review", "Refactoring", "Technical Debt",
        "Search Design", "Pagination", "Sorting Results", "Notification Delivery", "Background Jobs", "Queue Processing", "Data Migration", "Backup Recovery", "Monitoring", "Alert Thresholds",
        "Accessibility", "Responsive Layouts", "Internationalization", "Configuration Management", "Feature Flags", "Release Rollback", "Service Health", "Audit Trails", "Team Collaboration", "Documentation",
        "Algorithm Choice", "Memory Optimization", "Concurrency", "Session Management", "Password Security", "Token Expiration", "Request Tracing", "Analytics Events", "Candidate Feedback", "Assessment Fairness"
    );
}
