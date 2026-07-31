package com.CodeSphere.backend.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Seeds original repair-focused tasks for the Debugging task picker. */
@Component
@Profile("dev")
@Order(2)
public class DebuggingProblemBankSeeder implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public DebuggingProblemBankSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Long adminId = jdbcTemplate.query(
                "SELECT id FROM users WHERE username = 'admin' LIMIT 1",
                (rs, rowNum) -> rs.getLong("id")
        ).stream().findFirst().orElse(null);
        if (adminId == null) return;

        int added = 0;
        for (DebugProblem problem : problems()) {
            Integer existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM problems WHERE title = ?", Integer.class, problem.title());
            if (existing != null && existing > 0) continue;

            jdbcTemplate.update(
                    "INSERT INTO problems (title, description, input_format, output_format, constraints, difficulty, " +
                            "time_limit, memory_limit, tags, hints, editorial, is_published, created_by) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 1000, 262144, ?::jsonb, ?::jsonb, ?, true, ?)",
                    problem.title(), problem.description(), problem.input(), problem.output(), problem.constraints(),
                    problem.difficulty(), "[\"debugging\",\"" + problem.topic() + "\"]",
                    "[\"Read the supplied logic carefully and test the smallest edge case.\"]",
                    "Identify the faulty condition, boundary, or initialization, then repair it without changing the required input/output format.", adminId
            );
            Long problemId = jdbcTemplate.queryForObject("SELECT id FROM problems WHERE title = ?", Long.class, problem.title());
            jdbcTemplate.update(
                    "INSERT INTO test_cases (problem_id, input_data, expected_output, is_sample, order_index, explanation) VALUES (?, ?, ?, true, 0, ?)",
                    problemId, problem.sampleInput(), problem.sampleOutput(), "Use this to verify the repaired program.");
            jdbcTemplate.update(
                    "INSERT INTO test_cases (problem_id, input_data, expected_output, is_sample, order_index, explanation) VALUES (?, ?, ?, false, 1, ?)",
                    problemId, problem.hiddenInput(), problem.hiddenOutput(), "Hidden edge case.");
            added++;
        }
        System.out.printf("[DebuggingProblemBankSeeder] %d debugging problems added.%n", added);
    }

    private List<DebugProblem> problems() {
        return List.of(
                p("Debug: Inclusive Array Sum", "A program is meant to print the sum of all n integers, but its loop skips the final value. Repair the program so every value is included.", "n followed by n integers", "One integer: the total sum.", "1 <= n <= 100000", "EASY", "arrays", "4\n2 4 1 3", "10", "1\n9", "9"),
                p("Debug: Case-Insensitive Palindrome", "A program checks a word from both ends, but it treats uppercase and lowercase letters as different. Repair it so the palindrome check ignores letter case.", "One non-empty word containing English letters", "true or false.", "1 <= length <= 100000", "EASY", "strings", "Level", "true", "Code", "false"),
                p("Debug: First Target Index", "A binary-search program should return the first occurrence of a target in a sorted array. It currently stops at any matching value. Repair the search.", "n, n sorted integers, then target", "Zero-based first index, or -1.", "1 <= n <= 100000", "MEDIUM", "binary-search", "6\n1 2 2 2 5 8\n2", "1", "5\n1 3 5 7 9\n4", "-1"),
                p("Debug: Factorial of Zero", "A factorial program works for positive input but returns zero for 0. Repair the base case while keeping the result within 64-bit signed range.", "One non-negative integer n", "n factorial.", "0 <= n <= 20", "EASY", "math", "0", "1", "5", "120"),
                p("Debug: Unique Value Count", "A program counts distinct integers but accidentally counts repeated values more than once. Repair it so each value contributes only once.", "n followed by n integers", "One integer: the number of distinct values.", "1 <= n <= 100000", "MEDIUM", "hash-map", "6\n4 2 4 2 2 7", "3", "4\n9 9 9 9", "1")
        );
    }

    private DebugProblem p(String title, String description, String input, String output, String constraints,
                           String difficulty, String topic, String sampleInput, String sampleOutput,
                           String hiddenInput, String hiddenOutput) {
        return new DebugProblem(title, description, input, output, constraints, difficulty, topic,
                sampleInput, sampleOutput, hiddenInput, hiddenOutput);
    }

    private record DebugProblem(String title, String description, String input, String output, String constraints,
                                String difficulty, String topic, String sampleInput, String sampleOutput,
                                String hiddenInput, String hiddenOutput) { }
}
