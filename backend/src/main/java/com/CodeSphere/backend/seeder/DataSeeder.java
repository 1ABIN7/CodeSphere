package com.CodeSphere.backend.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * DataSeeder — Seeds initial demo data into the database
 *
 * Runs automatically on application startup in 'dev' profile only.
 * Seeds: organization, users, roles, question bank, assessments
 *
 * P5 responsibility: maintain and update seed data as schema evolves
 *
 * To run: make sure spring.profiles.active=dev in application-dev.yml
 */
@Component
@Profile("dev") // Only runs in development — never in production
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // Skip seeding if data already exists
        Integer userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users", Integer.class);

        if (userCount != null && userCount > 0) {
            System.out.println("[DataSeeder] Data already exists — skipping seed.");
            return;
        }

        System.out.println("[DataSeeder] Seeding demo data...");

        seedOrganization();
        seedUsers();
        seedQuestions();
        seedAssessment();

        System.out.println("[DataSeeder] Seeding complete!");
    }

    /**
     * Seeds a demo organization
     */
    private void seedOrganization() {
        jdbcTemplate.update(
            "INSERT INTO organizations (name) VALUES (?) ON CONFLICT DO NOTHING",
            "Demo Corp"
        );
        System.out.println("[DataSeeder] Organization seeded.");
    }

    /**
     * Seeds demo users:
     * - admin@demo.com (ROLE_ADMIN)
     * - evaluator@demo.com (ROLE_USER)
     * - candidate@demo.com (ROLE_USER)
     *
     * Password for all: password123 (BCrypt hashed)
     */
    private void seedUsers() {
        // BCrypt hash of "password123"
        String passwordHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        // Insert admin user
        jdbcTemplate.update(
            "INSERT INTO users (email, password_hash, first_name, last_name, organization_id) " +
            "VALUES (?, ?, ?, ?, (SELECT id FROM organizations WHERE name = 'Demo Corp'))",
            "admin@demo.com", passwordHash, "Admin", "User"
        );

        // Insert evaluator user
        jdbcTemplate.update(
            "INSERT INTO users (email, password_hash, first_name, last_name, organization_id) " +
            "VALUES (?, ?, ?, ?, (SELECT id FROM organizations WHERE name = 'Demo Corp'))",
            "evaluator@demo.com", passwordHash, "Evaluator", "User"
        );

        // Insert candidate user
        jdbcTemplate.update(
            "INSERT INTO users (email, password_hash, first_name, last_name, organization_id) " +
            "VALUES (?, ?, ?, ?, (SELECT id FROM organizations WHERE name = 'Demo Corp'))",
            "candidate@demo.com", passwordHash, "Candidate", "User"
        );

        // Assign ROLE_ADMIN to admin user
        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_id) " +
            "VALUES ((SELECT id FROM users WHERE email = 'admin@demo.com'), " +
            "(SELECT id FROM roles WHERE name = 'ROLE_ADMIN'))"
        );

        // Assign ROLE_USER to evaluator and candidate
        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_id) " +
            "VALUES ((SELECT id FROM users WHERE email = 'evaluator@demo.com'), " +
            "(SELECT id FROM roles WHERE name = 'ROLE_USER'))"
        );

        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_id) " +
            "VALUES ((SELECT id FROM users WHERE email = 'candidate@demo.com'), " +
            "(SELECT id FROM roles WHERE name = 'ROLE_USER'))"
        );

        System.out.println("[DataSeeder] Users seeded.");
    }

    /**
     * Seeds 5 sample MCQ questions into the question bank
     */
    private void seedQuestions() {
        // Question 1
        jdbcTemplate.update(
            "INSERT INTO question_bank (title, question_type, content, options, correct_answer, difficulty, is_approved) " +
            "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)",
            "What is the time complexity of binary search?",
            "MCQ_SINGLE",
            "What is the time complexity of binary search on a sorted array?",
            "[\"O(n)\", \"O(log n)\", \"O(n^2)\", \"O(1)\"]",
            "O(log n)",
            "MEDIUM",
            true
        );

        // Question 2
        jdbcTemplate.update(
            "INSERT INTO question_bank (title, question_type, content, options, correct_answer, difficulty, is_approved) " +
            "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)",
            "Which of the following is not a Java primitive type?",
            "MCQ_SINGLE",
            "Which of the following is NOT a primitive data type in Java?",
            "[\"int\", \"boolean\", \"String\", \"char\"]",
            "String",
            "EASY",
            true
        );

        // Question 3
        jdbcTemplate.update(
            "INSERT INTO question_bank (title, question_type, content, options, correct_answer, difficulty, is_approved) " +
            "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)",
            "Which HTTP methods are idempotent?",
            "MCQ_MULTI",
            "Which of the following HTTP methods are idempotent? (Select all that apply)",
            "[\"GET\", \"POST\", \"PUT\", \"DELETE\"]",
            "GET,PUT,DELETE",
            "MEDIUM",
            true
        );

        // Question 4
        jdbcTemplate.update(
            "INSERT INTO question_bank (title, question_type, content, options, correct_answer, difficulty, is_approved) " +
            "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)",
            "What does REST stand for?",
            "MCQ_SINGLE",
            "What does REST stand for in RESTful APIs?",
            "[\"Remote Execution State Transfer\", \"Representational State Transfer\", \"Request State Transfer\", \"Resource State Transfer\"]",
            "Representational State Transfer",
            "EASY",
            true
        );

        // Question 5
        jdbcTemplate.update(
            "INSERT INTO question_bank (title, question_type, content, options, correct_answer, difficulty, is_approved) " +
            "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)",
            "Which of the following are OOP principles?",
            "MCQ_MULTI",
            "Which of the following are core principles of Object Oriented Programming?",
            "[\"Encapsulation\", \"Compilation\", \"Inheritance\", \"Polymorphism\"]",
            "Encapsulation,Inheritance,Polymorphism",
            "EASY",
            true
        );

        System.out.println("[DataSeeder] Questions seeded.");
    }

    /**
     * Seeds a sample MCQ assessment
     */
    private void seedAssessment() {
        jdbcTemplate.update(
            "INSERT INTO assessments (title, description, assessment_type, duration_minutes, " +
            "passing_score, is_published, created_by, organization_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, " +
            "(SELECT id FROM users WHERE email = 'admin@demo.com'), " +
            "(SELECT id FROM organizations WHERE name = 'Demo Corp'))",
            "Demo MCQ Assessment",
            "A sample MCQ assessment for testing purposes",
            "MCQ",
            30,
            60.0,
            true
        );

        System.out.println("[DataSeeder] Assessment seeded.");
    }
}