package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.submission.JudgeResultResponse;
import com.CodeSphere.backend.model.SubmissionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI-powered Code Analysis Service.
 *
 * Performs static analysis on submitted code to provide intelligent feedback:
 *
 * 1. <b>Time Complexity Estimation</b> — Detects loop nesting, recursion patterns,
 *    and common algorithm signatures to estimate Big-O complexity.
 *
 * 2. <b>Space Complexity Estimation</b> — Analyzes data structure usage and
 *    allocation patterns.
 *
 * 3. <b>Code Quality Scoring</b> — Evaluates naming conventions, code length,
 *    method decomposition, commenting, and structural patterns.
 *
 * 4. <b>Anti-Pattern Detection</b> — Flags common mistakes like infinite loops,
 *    off-by-one patterns, and unused variables.
 *
 * 5. <b>Optimization Suggestions</b> — Contextual hints based on detected patterns
 *    and test case failures.
 *
 * 6. <b>Plagiarism Detection</b> — Token-based code similarity using winnowing
 *    algorithm (Moss-style fingerprinting).
 */
@Service
@Slf4j
public class CodeAnalysisService {

    // ---- Common regex patterns for analysis ----

    private static final Pattern FOR_LOOP = Pattern.compile("\\b(for)\\s*\\(");
    private static final Pattern WHILE_LOOP = Pattern.compile("\\b(while)\\s*\\(");
    private static final Pattern NESTED_LOOP = Pattern.compile(
            "(for|while)\\s*\\([^)]*\\)\\s*\\{[^}]*(for|while)\\s*\\(", Pattern.DOTALL);
    private static final Pattern TRIPLE_NESTED = Pattern.compile(
            "(for|while)[^{]*\\{[^}]*(for|while)[^{]*\\{[^}]*(for|while)", Pattern.DOTALL);
    private static final Pattern RECURSION = Pattern.compile(
            "(\\w+)\\s*\\([^)]*\\)\\s*\\{[^}]*\\1\\s*\\(", Pattern.DOTALL);
    private static final Pattern HASHMAP_USE = Pattern.compile(
            "\\b(HashMap|Map|dict|\\{\\s*\\}|new Map|unordered_map)\\b");
    private static final Pattern SORTING = Pattern.compile(
            "\\b(Arrays\\.sort|Collections\\.sort|sorted|sort|std::sort)\\b");
    private static final Pattern BINARY_SEARCH = Pattern.compile(
            "\\b(binarySearch|bisect|lower_bound|upper_bound)\\b|mid\\s*=\\s*\\(?\\s*(low|left|lo|start)");
    private static final Pattern DP_PATTERN = Pattern.compile(
            "\\b(dp|memo|cache|tabulation)\\b|\\[\\s*\\w+\\s*\\]\\s*\\[\\s*\\w+\\s*\\]\\s*=");
    private static final Pattern BFS_DFS = Pattern.compile(
            "\\b(Queue|Deque|Stack|queue|deque|stack|BFS|DFS|visited)\\b");
    private static final Pattern HEAP = Pattern.compile(
            "\\b(PriorityQueue|heapq|priority_queue|MinHeap|MaxHeap)\\b");

    // Anti-patterns
    private static final Pattern INFINITE_LOOP = Pattern.compile("while\\s*\\(\\s*true\\s*\\)");
    private static final Pattern MAGIC_NUMBERS = Pattern.compile("(?<!\\d)[2-9]\\d{2,}(?!\\d)");
    private static final Pattern EMPTY_CATCH = Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}");
    private static final Pattern SYSTEM_EXIT = Pattern.compile("System\\.exit|os\\._exit|process\\.exit");

    /**
     * Perform comprehensive AI analysis on submitted code.
     *
     * @param code           the source code
     * @param language       the programming language
     * @param overallStatus  the judge engine verdict
     * @param testCasesPassed number of test cases passed
     * @param totalTestCases  total number of test cases
     * @return detailed analysis result
     */
    public JudgeResultResponse analyze(String code, String language,
                                        SubmissionStatus overallStatus,
                                        int testCasesPassed, int totalTestCases) {
        log.debug("[CodeAnalysis] Analyzing {} code ({} chars)", language, code.length());

        String timeComplexity = estimateTimeComplexity(code);
        String spaceComplexity = estimateSpaceComplexity(code);
        int qualityScore = calculateCodeQualityScore(code, language);
        List<String> qualityIssues = detectCodeQualityIssues(code, language);
        List<String> antiPatterns = detectAntiPatterns(code, language);
        List<String> optimizations = generateOptimizationSuggestions(code, language, overallStatus);
        List<String> performanceHints = generatePerformanceHints(
                code, language, overallStatus, testCasesPassed, totalTestCases);

        // Calculate overall score
        int totalScore = totalTestCases > 0
                ? (int) Math.round(100.0 * testCasesPassed / totalTestCases)
                : 0;

        // Build raw metrics map
        Map<String, Object> rawMetrics = new LinkedHashMap<>();
        rawMetrics.put("codeLength", code.length());
        rawMetrics.put("lineCount", code.lines().count());
        rawMetrics.put("forLoopCount", countMatches(FOR_LOOP, code));
        rawMetrics.put("whileLoopCount", countMatches(WHILE_LOOP, code));
        rawMetrics.put("hasRecursion", RECURSION.matcher(code).find());
        rawMetrics.put("usesHashMap", HASHMAP_USE.matcher(code).find());
        rawMetrics.put("usesSorting", SORTING.matcher(code).find());
        rawMetrics.put("usesBinarySearch", BINARY_SEARCH.matcher(code).find());
        rawMetrics.put("usesDynamicProgramming", DP_PATTERN.matcher(code).find());
        rawMetrics.put("usesGraphTraversal", BFS_DFS.matcher(code).find());
        rawMetrics.put("usesHeap", HEAP.matcher(code).find());

        return JudgeResultResponse.builder()
                .overallVerdict(overallStatus.name())
                .score(testCasesPassed)
                .totalScore(totalTestCases)
                .estimatedTimeComplexity(timeComplexity)
                .estimatedSpaceComplexity(spaceComplexity)
                .codeQualityScore(qualityScore)
                .codeQualityIssues(qualityIssues)
                .antiPatterns(antiPatterns)
                .optimizationSuggestions(optimizations)
                .performanceHints(performanceHints)
                .similarityScore(0.0) // Placeholder — requires reference solutions
                .potentialPlagiarism(false)
                .rawMetrics(rawMetrics)
                .build();
    }

    /**
     * Compute similarity between two code submissions using token-based
     * winnowing algorithm (Moss-style fingerprinting).
     *
     * @return similarity score between 0.0 and 1.0
     */
    public double computeSimilarity(String code1, String code2) {
        List<String> tokens1 = tokenize(code1);
        List<String> tokens2 = tokenize(code2);

        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0;

        // Generate k-gram fingerprints using winnowing
        int k = 5; // k-gram size
        Set<Integer> fingerprints1 = generateFingerprints(tokens1, k);
        Set<Integer> fingerprints2 = generateFingerprints(tokens2, k);

        // Jaccard similarity
        Set<Integer> intersection = new HashSet<>(fingerprints1);
        intersection.retainAll(fingerprints2);

        Set<Integer> union = new HashSet<>(fingerprints1);
        union.addAll(fingerprints2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    // ---- Time Complexity Estimation ----

    private String estimateTimeComplexity(String code) {
        boolean hasTripleNested = TRIPLE_NESTED.matcher(code).find();
        boolean hasDoubleNested = NESTED_LOOP.matcher(code).find();
        boolean hasRecursion = RECURSION.matcher(code).find();
        boolean hasBinarySearch = BINARY_SEARCH.matcher(code).find();
        boolean hasSorting = SORTING.matcher(code).find();
        boolean hasDP = DP_PATTERN.matcher(code).find();
        boolean hasBFS = BFS_DFS.matcher(code).find();
        boolean hasHeap = HEAP.matcher(code).find();
        int forCount = countMatches(FOR_LOOP, code);
        int whileCount = countMatches(WHILE_LOOP, code);

        // Priority-based estimation
        if (hasTripleNested) return "O(n³)";
        if (hasDP && hasDoubleNested) return "O(n²) — Dynamic Programming";
        if (hasDoubleNested) return "O(n²)";
        if (hasSorting && (hasBinarySearch || hasHeap)) return "O(n log n)";
        if (hasSorting) return "O(n log n)";
        if (hasBinarySearch) return "O(log n)";
        if (hasRecursion && hasDP) return "O(n) — Memoized Recursion";
        if (hasRecursion) return "O(2^n) — Recursive (consider memoization)";
        if (hasHeap) return "O(n log k) — Heap-based";
        if (hasBFS) return "O(V + E) — Graph Traversal";
        if (forCount > 0 || whileCount > 0) return "O(n)";

        return "O(1)";
    }

    // ---- Space Complexity Estimation ----

    private String estimateSpaceComplexity(String code) {
        boolean hasDP = DP_PATTERN.matcher(code).find();
        boolean has2DArray = Pattern.compile("\\w+\\s*\\[\\s*\\w+\\s*\\]\\s*\\[\\s*\\w+\\s*\\]").matcher(code).find();
        boolean hasHashMap = HASHMAP_USE.matcher(code).find();
        boolean hasRecursion = RECURSION.matcher(code).find();
        boolean hasBFS = BFS_DFS.matcher(code).find();
        boolean hasArray = Pattern.compile("new\\s+\\w+\\[|\\[\\]\\s*=|ArrayList|List|vector").matcher(code).find();

        if (hasDP && has2DArray) return "O(n²) — 2D DP table";
        if (has2DArray) return "O(n²) — 2D array";
        if (hasDP) return "O(n) — DP table";
        if (hasHashMap) return "O(n) — Hash map storage";
        if (hasBFS) return "O(n) — Graph traversal queue";
        if (hasRecursion) return "O(n) — Recursion stack";
        if (hasArray) return "O(n) — Array allocation";

        return "O(1) — Constant space";
    }

    // ---- Code Quality Scoring ----

    private int calculateCodeQualityScore(String code, String language) {
        int score = 100;
        long lineCount = code.lines().count();

        // Penalize extremely long code
        if (lineCount > 200) score -= 10;
        else if (lineCount > 100) score -= 5;

        // Check for comments
        long commentLines = code.lines()
                .filter(l -> l.trim().startsWith("//") || l.trim().startsWith("#") || l.trim().startsWith("/*"))
                .count();
        if (commentLines == 0 && lineCount > 20) score -= 10;

        // Check for method/function decomposition
        int methodCount = countMatches(
                Pattern.compile("(public|private|protected|def |function )\\s+\\w+"), code);
        if (lineCount > 50 && methodCount <= 1) score -= 15;

        // Check for magic numbers
        int magicNumbers = countMatches(MAGIC_NUMBERS, code);
        score -= Math.min(10, magicNumbers * 2);

        // Check for empty catch blocks
        if (EMPTY_CATCH.matcher(code).find()) score -= 10;

        // Check variable naming (single letter variables beyond i,j,k,n,m)
        int singleLetterVars = countMatches(
                Pattern.compile("\\b(int|long|String|var|let|const)\\s+([a-hlo-z])\\b"), code);
        score -= Math.min(10, singleLetterVars * 3);

        // Consistent indentation bonus
        boolean hasConsistentIndent = code.lines()
                .filter(l -> !l.isBlank())
                .allMatch(l -> l.startsWith("\t") || l.startsWith("    ") || !l.startsWith(" "));
        if (hasConsistentIndent) score += 5;

        return Math.max(0, Math.min(100, score));
    }

    private List<String> detectCodeQualityIssues(String code, String language) {
        List<String> issues = new ArrayList<>();
        long lineCount = code.lines().count();

        if (lineCount > 200) {
            issues.add("Code is quite long (" + lineCount + " lines). Consider breaking it into smaller functions.");
        }

        long commentLines = code.lines()
                .filter(l -> l.trim().startsWith("//") || l.trim().startsWith("#"))
                .count();
        if (commentLines == 0 && lineCount > 20) {
            issues.add("No comments found. Adding comments improves code readability.");
        }

        if (EMPTY_CATCH.matcher(code).find()) {
            issues.add("Empty catch block detected. Handle or log exceptions properly.");
        }

        int magicNumbers = countMatches(MAGIC_NUMBERS, code);
        if (magicNumbers > 3) {
            issues.add("Multiple magic numbers detected. Consider using named constants.");
        }

        if (SYSTEM_EXIT.matcher(code).find()) {
            issues.add("System.exit() / process.exit() detected. Avoid forced termination in submissions.");
        }

        // Check for very long lines
        long longLines = code.lines().filter(l -> l.length() > 120).count();
        if (longLines > 3) {
            issues.add(longLines + " lines exceed 120 characters. Consider wrapping long lines.");
        }

        return issues;
    }

    // ---- Anti-Pattern Detection ----

    private List<String> detectAntiPatterns(String code, String language) {
        List<String> patterns = new ArrayList<>();

        if (INFINITE_LOOP.matcher(code).find() &&
            !Pattern.compile("break\\s*;").matcher(code).find()) {
            patterns.add("⚠ Potential infinite loop: while(true) without a break statement");
        }

        // Array index out of bounds risk
        if (Pattern.compile("\\[\\s*\\w+\\s*-\\s*1\\s*\\]").matcher(code).find() &&
            !Pattern.compile("if\\s*\\(\\s*\\w+\\s*>\\s*0").matcher(code).find()) {
            patterns.add("⚠ Possible off-by-one error: array access with [i-1] without boundary check");
        }

        // String concatenation in loop
        if (Pattern.compile("(for|while)[^{]*\\{[^}]*\\+\\s*=\\s*\"").matcher(code).find() &&
            "java".equalsIgnoreCase(language)) {
            patterns.add("⚠ String concatenation in a loop. Use StringBuilder for better performance in Java.");
        }

        // Nested try-catch
        if (Pattern.compile("try\\s*\\{[^}]*try\\s*\\{", Pattern.DOTALL).matcher(code).find()) {
            patterns.add("⚠ Nested try-catch blocks detected. Consider refactoring exception handling.");
        }

        return patterns;
    }

    // ---- Optimization Suggestions ----

    private List<String> generateOptimizationSuggestions(String code, String language,
                                                          SubmissionStatus status) {
        List<String> suggestions = new ArrayList<>();

        // If TLE, suggest algorithmic improvements
        if (status == SubmissionStatus.TIME_LIMIT_EXCEEDED) {
            if (NESTED_LOOP.matcher(code).find() && !HASHMAP_USE.matcher(code).find()) {
                suggestions.add("💡 Consider using a HashMap/Set for O(1) lookups instead of nested loops.");
            }
            if (!SORTING.matcher(code).find() && !BINARY_SEARCH.matcher(code).find()) {
                suggestions.add("💡 Consider sorting the input and using binary search for faster lookups.");
            }
            if (RECURSION.matcher(code).find() && !DP_PATTERN.matcher(code).find()) {
                suggestions.add("💡 Consider adding memoization to avoid redundant recursive calls.");
            }
        }

        // General suggestions
        if (Pattern.compile("\\bArrayList\\b").matcher(code).find() &&
            Pattern.compile("\\.contains\\(").matcher(code).find()) {
            suggestions.add("💡 ArrayList.contains() is O(n). Use a HashSet for O(1) membership testing.");
        }

        if (Pattern.compile("Collections\\.sort").matcher(code).find() &&
            Pattern.compile("\\.get\\(0\\)|\\.get\\(\\w+\\.size\\(\\)").matcher(code).find()) {
            suggestions.add("💡 If you only need min/max, use Collections.min()/max() instead of sorting (O(n) vs O(n log n)).");
        }

        return suggestions;
    }

    // ---- Performance Hints ----

    private List<String> generatePerformanceHints(String code, String language,
                                                   SubmissionStatus status,
                                                   int passed, int total) {
        List<String> hints = new ArrayList<>();

        if (status == SubmissionStatus.ACCEPTED) {
            hints.add("✅ All test cases passed! Great job.");
            return hints;
        }

        if (status == SubmissionStatus.TIME_LIMIT_EXCEEDED) {
            hints.add("⏱ Your solution is too slow. Look for a more efficient algorithm.");
            if (passed > 0) {
                hints.add(String.format("You passed %d/%d test cases — the failing ones likely have larger inputs.", passed, total));
            }
        }

        if (status == SubmissionStatus.WRONG_ANSWER) {
            hints.add("❌ Check your logic carefully. Consider edge cases:");
            hints.add("   • Empty inputs or arrays of size 1");
            hints.add("   • Very large or very small numbers");
            hints.add("   • Duplicate values");
            hints.add("   • Negative numbers (if applicable)");
        }

        if (status == SubmissionStatus.RUNTIME_ERROR) {
            hints.add("💥 Runtime error detected. Common causes:");
            hints.add("   • Array index out of bounds");
            hints.add("   • Null pointer / undefined reference");
            hints.add("   • Stack overflow from deep recursion");
            hints.add("   • Division by zero");
        }

        if (status == SubmissionStatus.MEMORY_LIMIT_EXCEEDED) {
            hints.add("📦 Memory limit exceeded. Consider:");
            hints.add("   • Reducing array/matrix sizes");
            hints.add("   • Using in-place algorithms");
            hints.add("   • Optimizing recursion to iteration");
        }

        return hints;
    }

    // ---- Plagiarism / Similarity (Winnowing Algorithm) ----

    private List<String> tokenize(String code) {
        // Remove comments, whitespace, and normalize to tokens
        String cleaned = code
                .replaceAll("//.*", "")
                .replaceAll("#.*", "")
                .replaceAll("/\\*.*?\\*/", "")
                .replaceAll("\\s+", " ")
                .replaceAll("\"[^\"]*\"", "STR")
                .replaceAll("'[^']*'", "CHR")
                .replaceAll("\\d+", "NUM")
                .trim();

        return Arrays.stream(cleaned.split("\\s+|(?=[{}()\\[\\];,.])|(?<=[{}()\\[\\];,.])"))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toList());
    }

    private Set<Integer> generateFingerprints(List<String> tokens, int k) {
        Set<Integer> fingerprints = new HashSet<>();
        if (tokens.size() < k) return fingerprints;

        // Generate k-gram hashes
        List<Integer> hashes = new ArrayList<>();
        for (int i = 0; i <= tokens.size() - k; i++) {
            int hash = tokens.subList(i, i + k).hashCode();
            hashes.add(hash);
        }

        // Winnowing: select minimum hash in each window
        int windowSize = 4;
        for (int i = 0; i <= hashes.size() - windowSize; i++) {
            int minHash = hashes.get(i);
            for (int j = i + 1; j < i + windowSize && j < hashes.size(); j++) {
                if (hashes.get(j) < minHash) {
                    minHash = hashes.get(j);
                }
            }
            fingerprints.add(minHash);
        }

        return fingerprints;
    }

    // ---- Utility ----

    private int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}
