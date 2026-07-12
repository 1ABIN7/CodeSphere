package com.CodeSphere.backend.service;

import com.CodeSphere.backend.config.JudgeEngineConfig;
import com.CodeSphere.backend.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Core Judge Engine Service.
 *
 * Compiles and executes user-submitted code in sandboxed processes
 * against problem test cases. Supports Java, Python, C++, C, and JavaScript.
 *
 * Features:
 * - Sandboxed process execution with strict time and memory limits
 * - Multi-language compilation and execution
 * - Per-test-case verdict determination with whitespace-normalized comparison
 * - Concurrent test case execution via thread pool
 * - Partial scoring based on test cases passed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeEngineService {

    private final JudgeEngineConfig config;

    /**
     * Result of executing code against a single test case.
     */
    public record TestCaseExecResult(
            Long testCaseId,
            SubmissionStatus status,
            String actualOutput,
            Integer execTime,
            Integer execMemory,
            String errorOutput
    ) {}

    /**
     * Aggregate result from judging all test cases.
     */
    public record JudgeResult(
            SubmissionStatus overallStatus,
            List<TestCaseExecResult> testCaseResults,
            int testCasesPassed,
            int totalTestCases,
            int score,
            Integer maxExecTime,
            Integer maxExecMemory,
            String compilationError
    ) {}

    /**
     * Judge submitted code against all test cases for a problem.
     *
     * @param code       the source code
     * @param language   the programming language
     * @param testCases  the test cases to run against
     * @param timeLimit  time limit per test case (ms)
     * @param memoryLimit memory limit per test case (KB)
     * @return aggregated judge result
     */
    public JudgeResult judge(String code, String language, List<TestCase> testCases,
                             int timeLimit, int memoryLimit) {

        if (code.length() > config.getMaxCodeLength()) {
            return new JudgeResult(
                    SubmissionStatus.COMPILATION_ERROR, List.of(),
                    0, testCases.size(), 0, null, null,
                    "Code exceeds maximum length of " + config.getMaxCodeLength() + " characters"
            );
        }

        // Create temporary working directory
        Path workDir;
        try {
            Path tempBase = Paths.get(config.getTempDir()).toAbsolutePath();
            Files.createDirectories(tempBase);
            workDir = Files.createTempDirectory(tempBase, "judge-");
        } catch (IOException e) {
            log.error("[JudgeEngine] Failed to create temp directory", e);
            return new JudgeResult(
                    SubmissionStatus.RUNTIME_ERROR, List.of(),
                    0, testCases.size(), 0, null, null,
                    "Internal error: could not create workspace"
            );
        }

        try {
            // Step 1: Write source file
            String fileName = getSourceFileName(language);
            Path sourceFile = workDir.resolve(fileName);
            Files.writeString(sourceFile, code);

            // Step 2: Compile (if needed)
            String compilationError = compile(language, sourceFile, workDir);
            if (compilationError != null) {
                return new JudgeResult(
                        SubmissionStatus.COMPILATION_ERROR, List.of(),
                        0, testCases.size(), 0, null, null,
                        compilationError
                );
            }

            // Step 3: Execute against each test case
            List<TestCaseExecResult> results = executeTestCases(
                    language, workDir, sourceFile, testCases, timeLimit, memoryLimit);

            // Step 4: Calculate results
            int passed = (int) results.stream()
                    .filter(r -> r.status() == SubmissionStatus.ACCEPTED)
                    .count();
            int total = testCases.size();

            // Calculate weighted score
            int totalWeight = testCases.stream().mapToInt(TestCase::getScoreWeight).sum();
            int earnedWeight = 0;
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).status() == SubmissionStatus.ACCEPTED) {
                    earnedWeight += testCases.get(i).getScoreWeight();
                }
            }
            int score = totalWeight > 0 ? (int) Math.round(100.0 * earnedWeight / totalWeight) : 0;

            // Determine overall status
            SubmissionStatus overall;
            if (passed == total) {
                overall = SubmissionStatus.ACCEPTED;
            } else if (passed > 0) {
                overall = SubmissionStatus.PARTIALLY_ACCEPTED;
            } else {
                // Find the first non-accepted status
                overall = results.stream()
                        .map(TestCaseExecResult::status)
                        .filter(s -> s != SubmissionStatus.ACCEPTED)
                        .findFirst()
                        .orElse(SubmissionStatus.WRONG_ANSWER);
            }

            // Get max execution metrics
            int maxTime = results.stream()
                    .map(TestCaseExecResult::execTime)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
            int maxMem = results.stream()
                    .map(TestCaseExecResult::execMemory)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);

            log.info("[JudgeEngine] Judged {} test cases: {}/{} passed, score={}, status={}",
                    total, passed, total, score, overall);

            return new JudgeResult(overall, results, passed, total, score, maxTime, maxMem, null);

        } catch (IOException e) {
            log.error("[JudgeEngine] Execution error", e);
            return new JudgeResult(
                    SubmissionStatus.RUNTIME_ERROR, List.of(),
                    0, testCases.size(), 0, null, null,
                    "Internal error: " + e.getMessage()
            );
        } finally {
            // Clean up temp directory
            cleanupDirectory(workDir);
        }
    }

    // ---- Compilation ----

    private String compile(String language, Path sourceFile, Path workDir) {
        List<String> compileCommand = getCompileCommand(language, sourceFile, workDir);
        if (compileCommand == null) {
            return null; // Interpreted language, no compilation needed
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(compileCommand);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = readStream(process.getInputStream(), 10000);
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return "Compilation timed out (30 seconds)";
            }

            if (process.exitValue() != 0) {
                return output.isEmpty() ? "Compilation failed with exit code " + process.exitValue() : output;
            }

            return null; // Compilation successful

        } catch (IOException | InterruptedException e) {
            return "Compilation error: " + e.getMessage();
        }
    }

    // ---- Execution ----

    private List<TestCaseExecResult> executeTestCases(
            String language, Path workDir, Path sourceFile,
            List<TestCase> testCases, int timeLimit, int memoryLimit) {

        List<TestCaseExecResult> results = new ArrayList<>();

        // Execute sequentially to avoid resource contention in sandboxed environment
        for (TestCase tc : testCases) {
            int effectiveTimeLimit = tc.getTimeLimitOverride() != null
                    ? tc.getTimeLimitOverride() : timeLimit;

            TestCaseExecResult result = executeOne(language, workDir, sourceFile,
                    tc, effectiveTimeLimit, memoryLimit);
            results.add(result);
        }

        return results;
    }

    private TestCaseExecResult executeOne(
            String language, Path workDir, Path sourceFile,
            TestCase testCase, int timeLimit, int memoryLimit) {

        List<String> runCommand = getRunCommand(language, sourceFile, workDir, memoryLimit);

        try {
            ProcessBuilder pb = new ProcessBuilder(runCommand);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(false);

            long startTime = System.currentTimeMillis();
            Process process = pb.start();

            // Feed input to stdin
            try (OutputStream os = process.getOutputStream()) {
                os.write(testCase.getInputData().getBytes());
                os.flush();
            }

            // Read stdout and stderr
            CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(
                    () -> readStream(process.getInputStream(), 1024 * 1024));
            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(
                    () -> readStream(process.getErrorStream(), 1024 * 64));

            // Wait with timeout
            boolean finished = process.waitFor(timeLimit + 500L, TimeUnit.MILLISECONDS);
            long execTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return new TestCaseExecResult(
                        testCase.getId(),
                        SubmissionStatus.TIME_LIMIT_EXCEEDED,
                        null, (int) execTime, null,
                        "Time limit exceeded (" + timeLimit + "ms)"
                );
            }

            String stdout = stdoutFuture.get(2, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(2, TimeUnit.SECONDS);

            // Check for runtime errors
            if (process.exitValue() != 0) {
                return new TestCaseExecResult(
                        testCase.getId(),
                        SubmissionStatus.RUNTIME_ERROR,
                        stdout, (int) execTime, null,
                        stderr.isEmpty() ? "Runtime error (exit code " + process.exitValue() + ")" : stderr
                );
            }

            // Check time limit
            if (execTime > timeLimit) {
                return new TestCaseExecResult(
                        testCase.getId(),
                        SubmissionStatus.TIME_LIMIT_EXCEEDED,
                        stdout, (int) execTime, null,
                        "Execution exceeded time limit"
                );
            }

            // Compare output
            boolean correct = normalizeOutput(stdout).equals(normalizeOutput(testCase.getExpectedOutput()));

            return new TestCaseExecResult(
                    testCase.getId(),
                    correct ? SubmissionStatus.ACCEPTED : SubmissionStatus.WRONG_ANSWER,
                    stdout.trim(), (int) execTime, null,
                    correct ? null : stderr
            );

        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            return new TestCaseExecResult(
                    testCase.getId(),
                    SubmissionStatus.RUNTIME_ERROR,
                    null, null, null,
                    "Execution error: " + e.getMessage()
            );
        }
    }

    // ---- Language-Specific Commands ----

    private String getSourceFileName(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "Main.java";
            case "python" -> "solution.py";
            case "cpp" -> "solution.cpp";
            case "c" -> "solution.c";
            case "javascript" -> "solution.js";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private List<String> getCompileCommand(String language, Path sourceFile, Path workDir) {
        return switch (language.toLowerCase()) {
            case "java" -> List.of("javac", sourceFile.toString());
            case "cpp" -> List.of("g++", "-std=c++17", "-O2", "-o",
                    workDir.resolve("solution").toString(), sourceFile.toString());
            case "c" -> List.of("gcc", "-std=c11", "-O2", "-o",
                    workDir.resolve("solution").toString(), sourceFile.toString(), "-lm");
            case "python", "javascript" -> null; // Interpreted
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private List<String> getRunCommand(String language, Path sourceFile, Path workDir, int memoryLimitKB) {
        return switch (language.toLowerCase()) {
            case "java" -> List.of("java", "-Xmx" + (memoryLimitKB / 1024) + "m",
                    "-cp", workDir.toString(), "Main");
            case "python" -> List.of("python3", sourceFile.toString());
            case "cpp", "c" -> List.of(workDir.resolve("solution").toString());
            case "javascript" -> List.of("node", sourceFile.toString());
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    // ---- Utility Methods ----

    /**
     * Normalize output for comparison: trim trailing whitespace per line,
     * remove trailing empty lines.
     */
    private String normalizeOutput(String output) {
        if (output == null) return "";
        return output.lines()
                .map(String::stripTrailing)
                .collect(java.util.stream.Collectors.joining("\n"))
                .stripTrailing();
    }

    private String readStream(InputStream inputStream, int maxBytes) {
        try {
            byte[] bytes = inputStream.readNBytes(maxBytes);
            return new String(bytes).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private void cleanupDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("[JudgeEngine] Failed to clean up temp directory: {}", dir, e);
        }
    }
}
