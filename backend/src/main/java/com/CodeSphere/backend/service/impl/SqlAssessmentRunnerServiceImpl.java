package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.dto.SqlRunResponse;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.service.SqlAssessmentRunnerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/** Runs read-only candidate SQL in a brand-new H2 database for every test case. */
@Service
public class SqlAssessmentRunnerServiceImpl implements SqlAssessmentRunnerService {
    private final ObjectMapper objectMapper;
    public SqlAssessmentRunnerServiceImpl(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    public SqlRunResponse run(Question question, String candidateSql) {
        candidateSql = normalizeCandidateSql(candidateSql);
        List<SqlCase> cases = parseCases(question.getSqlTestCases());
        if (cases.isEmpty()) throw new IllegalStateException("This SQL task has no automated test cases yet.");
        List<SqlRunResponse.CaseResult> results = new ArrayList<>();
        int passed = 0;
        for (SqlCase testCase : cases) {
            try {
                List<Map<String, String>> actual = executeInIsolatedDatabase(question.getSqlSetup(), testCase.setupSql(), candidateSql);
                boolean matches = actual.equals(normalizeExpected(testCase.expectedRows()));
                if (matches) passed++;
                results.add(new SqlRunResponse.CaseResult(displayName(testCase), matches, actual.size(), matches ? "Passed" : "Result did not match expected rows"));
            } catch (Exception error) {
                results.add(new SqlRunResponse.CaseResult(displayName(testCase), false, 0, testCase.hidden() ? "Hidden test did not pass" : safeMessage(error)));
            }
        }
        double percent = Math.round(passed * 1000D / cases.size()) / 10D;
        return new SqlRunResponse(passed == cases.size() ? "ACCEPTED" : "WRONG_ANSWER", passed, cases.size(), percent, results,
                passed == cases.size() ? "All SQL test cases passed." : "Some SQL test cases did not pass.");
    }

    private List<Map<String, String>> executeInIsolatedDatabase(String baseSetup, String caseSetup, String query) throws Exception {
        String url = "jdbc:h2:mem:sql_task_" + UUID.randomUUID().toString().replace('-', '_') + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            runTrustedSetup(connection, baseSetup);
            runTrustedSetup(connection, caseSetup);
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setQueryTimeout(3);
                statement.setMaxRows(500);
                try (ResultSet rows = statement.executeQuery()) { return readRows(rows); }
            }
        }
    }

    private void runTrustedSetup(Connection connection, String script) throws SQLException {
        if (script == null || script.isBlank()) return;
        try (Statement statement = connection.createStatement()) {
            for (String part : script.split(";")) if (!part.isBlank()) statement.execute(part);
        }
    }

    private List<Map<String, String>> readRows(ResultSet rows) throws SQLException {
        List<Map<String, String>> output = new ArrayList<>();
        ResultSetMetaData metadata = rows.getMetaData();
        while (rows.next()) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int index = 1; index <= metadata.getColumnCount(); index++)
                row.put(metadata.getColumnLabel(index).toLowerCase(Locale.ROOT), Objects.toString(rows.getObject(index), null));
            output.add(row);
        }
        return output;
    }

    private List<SqlCase> parseCases(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<List<SqlCase>>() {}); }
        catch (Exception error) { throw new IllegalStateException("The SQL task test cases are invalid."); }
    }

    private List<Map<String, String>> normalizeExpected(List<Map<String, Object>> rows) {
        List<Map<String, String>> normalized = new ArrayList<>();
        for (Map<String, Object> source : rows == null ? List.<Map<String, Object>>of() : rows) {
            Map<String, String> row = new LinkedHashMap<>();
            source.forEach((key, value) -> row.put(key.toLowerCase(Locale.ROOT), Objects.toString(value, null)));
            normalized.add(row);
        }
        return normalized;
    }

    private String normalizeCandidateSql(String sql) {
        String executableSql = sql == null ? "" : sql.trim();
        // A final terminator is natural when candidates paste a SQL query. Remove only that
        // one; any earlier semicolon still means the input contains more than one statement.
        if (executableSql.endsWith(";")) executableSql = executableSql.substring(0, executableSql.length() - 1).trim();
        String normalized = executableSql.toUpperCase(Locale.ROOT);
        if (!(normalized.startsWith("SELECT") || normalized.startsWith("WITH") || normalized.startsWith("EXPLAIN"))
                || normalized.contains(";") || normalized.contains("--") || normalized.contains("/*")
                || normalized.contains("INFORMATION_SCHEMA") || normalized.contains("CSVREAD") || normalized.contains("RUNSCRIPT"))
            throw new IllegalArgumentException("Only one read-only SELECT, WITH, or EXPLAIN query is allowed.");
        if (executableSql.contains("?"))
            throw new IllegalArgumentException("Use the numeric value stated in the question; parameter placeholders are not supported in this SQL editor.");
        return executableSql;
    }
    private String displayName(SqlCase testCase) {
        return testCase.hidden() ? "Hidden test" : (testCase.name() == null || testCase.name().isBlank() ? "Test case" : testCase.name());
    }
    private String safeMessage(Exception error) { return error.getMessage() == null ? "Query could not be executed" : error.getMessage().replaceAll("[\\r\\n]+", " "); }
    private record SqlCase(String name, String setupSql, List<Map<String, Object>> expectedRows, boolean hidden) {}
}
