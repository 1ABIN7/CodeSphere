package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.service.impl.SqlAssessmentRunnerServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlAssessmentRunnerServiceImplTest {
    private final SqlAssessmentRunnerService runner = new SqlAssessmentRunnerServiceImpl(new ObjectMapper());

    @Test
    void runsSelectAgainstFreshIsolatedDataAndAwardsPartialCredit() {
        Question question = task();
        var result = runner.run(question, "SELECT id, name FROM employees ORDER BY id");
        assertEquals("WRONG_ANSWER", result.status());
        assertEquals(1, result.passedTestCases());
        assertEquals(2, result.totalTestCases());
        assertEquals(50D, result.scorePercent());
    }

    @Test
    void rejectsStatementsThatCouldChangeTheDatabase() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> runner.run(task(), "DELETE FROM employees"));
        assertTrue(exception.getMessage().contains("read-only"));
    }

    private Question task() {
        Question question = new Question();
        question.setSqlSetup("CREATE TABLE employees (id INT, name VARCHAR(80)); INSERT INTO employees VALUES (1, 'Ada')");
        question.setSqlTestCases("""
                [{"name":"base","setupSql":"","expectedRows":[{"id":1,"name":"Ada"}],"hidden":false},
                 {"name":"extra","setupSql":"INSERT INTO employees VALUES (2, 'Grace')","expectedRows":[{"id":1,"name":"Ada"}],"hidden":true}]
                """);
        return question;
    }
}
