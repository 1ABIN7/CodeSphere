package com.CodeSphere.backend.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Makes each curated coding problem available as a selectable assessment question. */
@Component
@Profile("dev")
@Order(3)
public class ProblemQuestionBankSyncSeeder implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public ProblemQuestionBankSyncSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        List<ProblemRow> problems = jdbcTemplate.query(
                "SELECT id, title, description, difficulty, tags FROM problems ORDER BY id",
                (rs, rowNum) -> new ProblemRow(rs.getLong("id"), rs.getString("title"),
                        rs.getString("description"), rs.getString("difficulty"), rs.getString("tags"))
        );

        int added = 0;
        for (ProblemRow problem : problems) {
            Integer existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM questions WHERE coding_problem_id = ?", Integer.class, problem.id());
            if (existing != null && existing > 0) continue;

            jdbcTemplate.update(
                    "INSERT INTO questions (title, content, category, type, difficulty, status, correct_answers, " +
                            "points, negative_score, question_type, coding_problem_id, system_generated) " +
                    "VALUES (?, ?, 'Coding practice', ?, ?, 'APPROVED', '', ?, 0, ?, ?, true)",
                    problem.title(), problem.description(), questionTypeFor(problem), problem.difficulty(),
                    pointsFor(problem.difficulty()), questionTypeFor(problem), problem.id());
            added++;
        }
        System.out.printf("[ProblemQuestionBankSyncSeeder] %d coding problems added to the Question Bank.%n", added);
    }

    private int pointsFor(String difficulty) {
        return switch (difficulty == null ? "" : difficulty) {
            case "HARD" -> 15;
            case "MEDIUM" -> 10;
            default -> 5;
        };
    }

    private String questionTypeFor(ProblemRow problem) {
        return problem.tags() != null && problem.tags().toLowerCase().contains("debugging") ? "DEBUGGING" : "CODING";
    }

    private record ProblemRow(Long id, String title, String description, String difficulty, String tags) { }
}
