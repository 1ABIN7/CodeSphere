package com.CodeSphere.backend.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * DataSeeder — Seeds initial demo data into the database
 *
 * Runs automatically on application startup in 'dev' profile only.
 * Seeds: organization, users, roles, question bank, assessments,
 *        and 25 curated coding problems with test cases.
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
        seedProblems();

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
     * - admin@demo.com (ROLE_SUPER_ADMIN)
     * - evaluator@demo.com (ROLE_EXAMINER)
     * - candidate@demo.com (ROLE_CANDIDATE)
     *
     * Password for all: password123 (BCrypt hashed)
     */
    private void seedUsers() {
        // BCrypt hash of "password123"
        String passwordHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        // Insert admin user
        jdbcTemplate.update(
            "INSERT INTO users (username, email, password, role, first_name, last_name, organization_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, (SELECT id FROM organizations WHERE name = 'Demo Corp'))",
            "admin", "admin@demo.com", passwordHash, "ROLE_SUPER_ADMIN", "Admin", "User"
        );

        // Insert evaluator user
        jdbcTemplate.update(
            "INSERT INTO users (username, email, password, role, first_name, last_name, organization_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, (SELECT id FROM organizations WHERE name = 'Demo Corp'))",
            "evaluator", "evaluator@demo.com", passwordHash, "ROLE_EXAMINER", "Evaluator", "User"
        );

        // Insert candidate user
        jdbcTemplate.update(
            "INSERT INTO users (username, email, password, role, first_name, last_name, organization_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, (SELECT id FROM organizations WHERE name = 'Demo Corp'))",
            "candidate", "candidate@demo.com", passwordHash, "ROLE_CANDIDATE", "Candidate", "User"
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

    // =========================================================================
    //  CODING PROBLEMS — 25 curated problems across Easy, Medium, Hard
    // =========================================================================

    /**
     * Seeds 25 coding problems with test cases for training and practice.
     */
    private void seedProblems() {
        Long adminId = jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE email = 'admin@demo.com'", Long.class);

        // ---- EASY PROBLEMS (1-8) ----
        seedTwoSum(adminId);
        seedReverseString(adminId);
        seedFizzBuzz(adminId);
        seedPalindromeCheck(adminId);
        seedMaximumElement(adminId);
        seedCountVowels(adminId);
        seedFactorial(adminId);
        seedFibonacci(adminId);

        // ---- MEDIUM PROBLEMS (9-18) ----
        seedValidParentheses(adminId);
        seedMergeSortedArrays(adminId);
        seedBinarySearch(adminId);
        seedLongestSubstringNoRepeat(adminId);
        seedMatrixSpiralOrder(adminId);
        seedGroupAnagrams(adminId);
        seedContainerWithMostWater(adminId);
        seedThreeSum(adminId);
        seedRotateMatrix(adminId);
        seedProductExceptSelf(adminId);

        // ---- HARD PROBLEMS (19-25) ----
        seedLongestPalindromicSubstring(adminId);
        seedMergeKSortedLists(adminId);
        seedTrappingRainWater(adminId);
        seedNQueens(adminId);
        seedWordBreak(adminId);
        seedSlidingWindowMaximum(adminId);
        seedMinimumWindowSubstring(adminId);

        System.out.println("[DataSeeder] 25 coding problems seeded.");
    }

    private Long insertProblem(String title, String description, String inputFormat,
                               String outputFormat, String constraints, String difficulty,
                               int timeLimit, int memoryLimit, String tagsJson,
                               String hintsJson, String editorial, Long createdBy) {
        jdbcTemplate.update(
            "INSERT INTO problems (title, description, input_format, output_format, constraints, " +
            "difficulty, time_limit, memory_limit, tags, hints, editorial, is_published, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, true, ?)",
            title, description, inputFormat, outputFormat, constraints,
            difficulty, timeLimit, memoryLimit, tagsJson, hintsJson, editorial, createdBy
        );
        return jdbcTemplate.queryForObject(
            "SELECT id FROM problems WHERE title = ?", Long.class, title);
    }

    private void insertTestCase(Long problemId, String input, String expectedOutput,
                                boolean isSample, int orderIndex, String explanation) {
        jdbcTemplate.update(
            "INSERT INTO test_cases (problem_id, input_data, expected_output, is_sample, order_index, explanation) " +
            "VALUES (?, ?, ?, ?, ?, ?)",
            problemId, input, expectedOutput, isSample, orderIndex, explanation
        );
    }

    // ---- EASY PROBLEMS ----

    private void seedTwoSum(Long adminId) {
        Long id = insertProblem(
            "Two Sum",
            "Given an array of integers `nums` and an integer `target`, return the indices of the two numbers such that they add up to `target`.\n\nYou may assume that each input would have exactly one solution, and you may not use the same element twice.\n\nReturn the answer with the smaller index first.",
            "First line: n (size of array)\nSecond line: n space-separated integers\nThird line: target integer",
            "Two space-separated indices (0-indexed)",
            "2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9\n-10^9 <= target <= 10^9\nExactly one valid answer exists.",
            "EASY", 1000, 262144,
            "[\"array\", \"hash-map\"]",
            "[\"Try using a hash map to store values you've seen.\", \"For each number, check if target - number exists in the map.\"]",
            "Use a HashMap: iterate through the array, for each element check if (target - element) exists in the map. If yes, return both indices. Otherwise, add the current element and its index to the map. Time: O(n), Space: O(n).",
            adminId
        );
        insertTestCase(id, "4\n2 7 11 15\n9", "0 1", true, 0, "2 + 7 = 9");
        insertTestCase(id, "3\n3 2 4\n6", "1 2", true, 1, "2 + 4 = 6");
        insertTestCase(id, "2\n3 3\n6", "0 1", false, 2, null);
        insertTestCase(id, "5\n1 5 3 7 2\n9", "1 3", false, 3, null);
        insertTestCase(id, "4\n-1 -2 -3 -4\n-6", "1 3", false, 4, null);
        insertTestCase(id, "6\n0 4 3 0 1 2\n0", "0 3", false, 5, null);
    }

    private void seedReverseString(Long adminId) {
        Long id = insertProblem(
            "Reverse String",
            "Write a function that reverses a string. The input string is given as a single line of text.\n\nReturn the reversed string.",
            "A single line containing the string to reverse.",
            "A single line containing the reversed string.",
            "1 <= s.length <= 10^5\nThe string consists of printable ASCII characters.",
            "EASY", 1000, 262144,
            "[\"string\", \"two-pointers\"]",
            "[\"Try using two pointers from both ends.\", \"You can also convert to a char array and swap.\"]",
            "Use two pointers: one at the start and one at the end. Swap characters and move inward. Time: O(n), Space: O(1) if done in-place.",
            adminId
        );
        insertTestCase(id, "hello", "olleh", true, 0, "Reverse of 'hello'");
        insertTestCase(id, "Hannah", "hannaH", true, 1, "Case-sensitive reversal");
        insertTestCase(id, "a", "a", false, 2, null);
        insertTestCase(id, "ab", "ba", false, 3, null);
        insertTestCase(id, "racecar", "racecar", false, 4, null);
        insertTestCase(id, "Hello World!", "!dlroW olleH", false, 5, null);
    }

    private void seedFizzBuzz(Long adminId) {
        Long id = insertProblem(
            "FizzBuzz",
            "Given an integer `n`, return a string with numbers from 1 to n separated by newlines, but:\n- For multiples of 3, print \"Fizz\" instead of the number.\n- For multiples of 5, print \"Buzz\" instead of the number.\n- For multiples of both 3 and 5, print \"FizzBuzz\".",
            "A single integer n.",
            "Lines from 1 to n with FizzBuzz rules applied.",
            "1 <= n <= 10^4",
            "EASY", 1000, 262144,
            "[\"math\", \"string\"]",
            "[\"Check divisibility by 15 first (both 3 and 5).\", \"Use modulo operator %.\"]",
            "Simple conditional: check n%15==0 first, then n%3==0, then n%5==0, else print the number.",
            adminId
        );
        insertTestCase(id, "5", "1\n2\nFizz\n4\nBuzz", true, 0, "1-5 with FizzBuzz");
        insertTestCase(id, "15", "1\n2\nFizz\n4\nBuzz\nFizz\n7\n8\nFizz\nBuzz\n11\nFizz\n13\n14\nFizzBuzz", true, 1, "1-15 shows FizzBuzz at 15");
        insertTestCase(id, "1", "1", false, 2, null);
        insertTestCase(id, "3", "1\n2\nFizz", false, 3, null);
    }

    private void seedPalindromeCheck(Long adminId) {
        Long id = insertProblem(
            "Palindrome Check",
            "Given a string `s`, determine if it is a palindrome, considering only alphanumeric characters and ignoring case.\n\nReturn \"true\" if it is a palindrome, \"false\" otherwise.",
            "A single line containing the string s.",
            "\"true\" or \"false\"",
            "1 <= s.length <= 2 * 10^5\nString consists of printable ASCII characters.",
            "EASY", 1000, 262144,
            "[\"string\", \"two-pointers\"]",
            "[\"Filter out non-alphanumeric characters first.\", \"Compare characters from both ends.\"]",
            "Use two pointers. Skip non-alphanumeric chars. Compare lowercase versions. Time: O(n), Space: O(1).",
            adminId
        );
        insertTestCase(id, "A man, a plan, a canal: Panama", "true", true, 0, "Classic palindrome");
        insertTestCase(id, "race a car", "false", true, 1, "Not a palindrome");
        insertTestCase(id, " ", "true", false, 2, null);
        insertTestCase(id, "a.", "true", false, 3, null);
        insertTestCase(id, "0P", "false", false, 4, null);
    }

    private void seedMaximumElement(Long adminId) {
        Long id = insertProblem(
            "Maximum Element",
            "Given an array of integers, find and return the maximum element.",
            "First line: n (size of array)\nSecond line: n space-separated integers",
            "A single integer — the maximum element.",
            "1 <= n <= 10^5\n-10^9 <= nums[i] <= 10^9",
            "EASY", 1000, 262144,
            "[\"array\"]",
            "[\"Initialize max with the first element.\", \"Iterate and update max when a larger element is found.\"]",
            "Linear scan keeping track of the maximum. Time: O(n), Space: O(1).",
            adminId
        );
        insertTestCase(id, "5\n3 1 4 1 5", "5", true, 0, "Max is 5");
        insertTestCase(id, "3\n-1 -2 -3", "-1", true, 1, "All negatives, max is -1");
        insertTestCase(id, "1\n42", "42", false, 2, null);
        insertTestCase(id, "4\n0 0 0 0", "0", false, 3, null);
        insertTestCase(id, "6\n1000000000 -1000000000 999999999 -999999999 0 1", "1000000000", false, 4, null);
    }

    private void seedCountVowels(Long adminId) {
        Long id = insertProblem(
            "Count Vowels",
            "Given a string, count the number of vowels (a, e, i, o, u) in it. Count both uppercase and lowercase vowels.",
            "A single line containing the string.",
            "A single integer — the count of vowels.",
            "1 <= s.length <= 10^5",
            "EASY", 1000, 262144,
            "[\"string\"]",
            "[\"Convert to lowercase and check against vowel set.\"]",
            "Iterate through each character, check if it's a vowel (case-insensitive). Time: O(n).",
            adminId
        );
        insertTestCase(id, "Hello World", "3", true, 0, "e, o, o = 3 vowels");
        insertTestCase(id, "aeiou", "5", true, 1, "All vowels");
        insertTestCase(id, "bcdfg", "0", false, 2, null);
        insertTestCase(id, "AEIOU", "5", false, 3, null);
        insertTestCase(id, "Programming", "3", false, 4, null);
    }

    private void seedFactorial(Long adminId) {
        Long id = insertProblem(
            "Factorial",
            "Given a non-negative integer `n`, compute its factorial (n!).\n\nn! = n × (n-1) × (n-2) × ... × 1\n0! = 1",
            "A single integer n.",
            "A single integer — n!",
            "0 <= n <= 20",
            "EASY", 1000, 262144,
            "[\"math\", \"recursion\"]",
            "[\"Base case: 0! = 1.\", \"Use iteration or recursion.\"]",
            "Simple iterative multiplication or recursion. Time: O(n), Space: O(1) iterative.",
            adminId
        );
        insertTestCase(id, "5", "120", true, 0, "5! = 120");
        insertTestCase(id, "0", "1", true, 1, "0! = 1");
        insertTestCase(id, "1", "1", false, 2, null);
        insertTestCase(id, "10", "3628800", false, 3, null);
        insertTestCase(id, "20", "2432902008176640000", false, 4, null);
    }

    private void seedFibonacci(Long adminId) {
        Long id = insertProblem(
            "Fibonacci Number",
            "Given `n`, return the nth Fibonacci number.\n\nThe Fibonacci sequence: F(0) = 0, F(1) = 1, F(n) = F(n-1) + F(n-2) for n > 1.",
            "A single integer n.",
            "A single integer — the nth Fibonacci number.",
            "0 <= n <= 45",
            "EASY", 1000, 262144,
            "[\"math\", \"dynamic-programming\"]",
            "[\"Use iteration instead of naive recursion.\", \"Keep track of only the last two values.\"]",
            "Iterative approach with two variables. Time: O(n), Space: O(1).",
            adminId
        );
        insertTestCase(id, "0", "0", true, 0, "F(0) = 0");
        insertTestCase(id, "1", "1", true, 1, "F(1) = 1");
        insertTestCase(id, "10", "55", false, 2, null);
        insertTestCase(id, "20", "6765", false, 3, null);
        insertTestCase(id, "45", "1134903170", false, 4, null);
    }

    // ---- MEDIUM PROBLEMS ----

    private void seedValidParentheses(Long adminId) {
        Long id = insertProblem(
            "Valid Parentheses",
            "Given a string `s` containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.\n\nA string is valid if:\n1. Open brackets are closed by the same type of brackets.\n2. Open brackets are closed in the correct order.\n3. Every close bracket has a corresponding open bracket.\n\nReturn \"true\" or \"false\".",
            "A single line containing the string of brackets.",
            "\"true\" or \"false\"",
            "1 <= s.length <= 10^4\ns consists of parentheses only '(){}[]'.",
            "MEDIUM", 1000, 262144,
            "[\"stack\", \"string\"]",
            "[\"Use a stack to track opening brackets.\", \"When you see a closing bracket, check the top of the stack.\"]",
            "Use a stack. Push opening brackets, pop and match closing brackets. Valid if stack is empty at end. Time: O(n), Space: O(n).",
            adminId
        );
        insertTestCase(id, "()", "true", true, 0, "Simple valid");
        insertTestCase(id, "()[]{}", "true", true, 1, "All types valid");
        insertTestCase(id, "(]", "false", false, 2, null);
        insertTestCase(id, "([)]", "false", false, 3, null);
        insertTestCase(id, "{[]}", "true", false, 4, null);
        insertTestCase(id, "", "true", false, 5, null);
    }

    private void seedMergeSortedArrays(Long adminId) {
        Long id = insertProblem(
            "Merge Two Sorted Arrays",
            "Given two sorted integer arrays `nums1` and `nums2`, merge them into a single sorted array and return it.",
            "First line: n (size of first array)\nSecond line: n space-separated integers (sorted)\nThird line: m (size of second array)\nFourth line: m space-separated integers (sorted)",
            "Space-separated integers of the merged sorted array.",
            "0 <= n, m <= 10^4\n-10^9 <= nums[i] <= 10^9\nBoth arrays are sorted in non-decreasing order.",
            "MEDIUM", 1000, 262144,
            "[\"array\", \"two-pointers\", \"sorting\"]",
            "[\"Use two pointers, one for each array.\", \"Compare elements and add the smaller one to the result.\"]",
            "Two-pointer technique: compare elements from both arrays and merge. Time: O(n+m), Space: O(n+m).",
            adminId
        );
        insertTestCase(id, "3\n1 2 3\n3\n2 5 6", "1 2 2 3 5 6", true, 0, "Standard merge");
        insertTestCase(id, "1\n1\n0", "1", true, 1, "Empty second array");
        insertTestCase(id, "0\n\n1\n5", "5", false, 2, null);
        insertTestCase(id, "4\n1 3 5 7\n4\n2 4 6 8", "1 2 3 4 5 6 7 8", false, 3, null);
    }

    private void seedBinarySearch(Long adminId) {
        Long id = insertProblem(
            "Binary Search",
            "Given a sorted array of integers `nums` and a target value, return the index of the target if it is in the array. Otherwise, return -1.",
            "First line: n (size of array)\nSecond line: n space-separated sorted integers\nThird line: target integer",
            "A single integer — the index of target, or -1 if not found.",
            "1 <= n <= 10^4\n-10^4 <= nums[i], target <= 10^4\nAll integers in nums are unique.\nnums is sorted in ascending order.",
            "MEDIUM", 1000, 262144,
            "[\"binary-search\", \"array\"]",
            "[\"Maintain left and right boundaries.\", \"Calculate mid and compare with target.\"]",
            "Classic binary search: maintain [left, right], compute mid, shrink the search space. Time: O(log n), Space: O(1).",
            adminId
        );
        insertTestCase(id, "6\n-1 0 3 5 9 12\n9", "4", true, 0, "9 is at index 4");
        insertTestCase(id, "6\n-1 0 3 5 9 12\n2", "-1", true, 1, "2 not found");
        insertTestCase(id, "1\n5\n5", "0", false, 2, null);
        insertTestCase(id, "1\n5\n3", "-1", false, 3, null);
        insertTestCase(id, "5\n1 2 3 4 5\n1", "0", false, 4, null);
        insertTestCase(id, "5\n1 2 3 4 5\n5", "4", false, 5, null);
    }

    private void seedLongestSubstringNoRepeat(Long adminId) {
        Long id = insertProblem(
            "Longest Substring Without Repeating Characters",
            "Given a string `s`, find the length of the longest substring without repeating characters.",
            "A single line containing the string s.",
            "A single integer — the length of the longest substring without repeating characters.",
            "0 <= s.length <= 5 * 10^4\ns consists of English letters, digits, symbols and spaces.",
            "MEDIUM", 2000, 262144,
            "[\"sliding-window\", \"hash-map\", \"string\"]",
            "[\"Use a sliding window with a set to track characters.\", \"When a duplicate is found, shrink the window from the left.\"]",
            "Sliding window with a HashSet. Expand right, when duplicate found shrink left until no duplicate. Time: O(n), Space: O(min(n, charset)).",
            adminId
        );
        insertTestCase(id, "abcabcbb", "3", true, 0, "\"abc\" has length 3");
        insertTestCase(id, "bbbbb", "1", true, 1, "\"b\" has length 1");
        insertTestCase(id, "pwwkew", "3", false, 2, null);
        insertTestCase(id, "", "0", false, 3, null);
        insertTestCase(id, "a", "1", false, 4, null);
        insertTestCase(id, "abcdef", "6", false, 5, null);
    }

    private void seedMatrixSpiralOrder(Long adminId) {
        Long id = insertProblem(
            "Spiral Matrix",
            "Given an m x n matrix, return all elements of the matrix in spiral order.",
            "First line: m n (rows and columns)\nNext m lines: n space-separated integers per row",
            "Space-separated integers in spiral order.",
            "m == matrix.length\nn == matrix[i].length\n1 <= m, n <= 10\n-100 <= matrix[i][j] <= 100",
            "MEDIUM", 1000, 262144,
            "[\"matrix\", \"simulation\"]",
            "[\"Define four boundaries: top, bottom, left, right.\", \"Traverse in order: right, down, left, up, shrinking boundaries.\"]",
            "Layer-by-layer peeling. Maintain top/bottom/left/right bounds and traverse each layer. Time: O(m*n), Space: O(1) extra.",
            adminId
        );
        insertTestCase(id, "3 3\n1 2 3\n4 5 6\n7 8 9", "1 2 3 6 9 8 7 4 5", true, 0, "3x3 spiral");
        insertTestCase(id, "3 4\n1 2 3 4\n5 6 7 8\n9 10 11 12", "1 2 3 4 8 12 11 10 9 5 6 7", true, 1, "3x4 spiral");
        insertTestCase(id, "1 1\n1", "1", false, 2, null);
        insertTestCase(id, "1 4\n1 2 3 4", "1 2 3 4", false, 3, null);
    }

    private void seedGroupAnagrams(Long adminId) {
        Long id = insertProblem(
            "Group Anagrams",
            "Given an array of strings, group the anagrams together. An anagram is a word formed by rearranging the letters of another word.\n\nReturn the groups sorted alphabetically by their first element. Within each group, sort the strings alphabetically.",
            "First line: n (number of strings)\nSecond line: n space-separated strings",
            "Each line contains one group of anagrams (space-separated, sorted). Groups sorted by first element.",
            "1 <= n <= 10^4\n0 <= strs[i].length <= 100\nstrs[i] consists of lowercase English letters.",
            "MEDIUM", 2000, 262144,
            "[\"hash-map\", \"string\", \"sorting\"]",
            "[\"Sort each string's characters to create a key.\", \"Use a HashMap with the sorted key to group anagrams.\"]",
            "For each string, sort its chars as a key, map key -> list of anagrams. Time: O(n * k log k) where k is max string length.",
            adminId
        );
        insertTestCase(id, "6\neat tea tan ate nat bat", "ate eat tea\nbat\nnat tan", true, 0, "3 anagram groups");
        insertTestCase(id, "1\na", "a", true, 1, "Single string");
        insertTestCase(id, "1\n\"\"", "\"\"", false, 2, null);
    }

    private void seedContainerWithMostWater(Long adminId) {
        Long id = insertProblem(
            "Container With Most Water",
            "You are given an integer array `height` of length n. There are n vertical lines drawn such that the two endpoints of the ith line are (i, 0) and (i, height[i]).\n\nFind two lines that together with the x-axis form a container that holds the most water.\n\nReturn the maximum amount of water a container can store.",
            "First line: n\nSecond line: n space-separated integers (heights)",
            "A single integer — the maximum water area.",
            "n == height.length\n2 <= n <= 10^5\n0 <= height[i] <= 10^4",
            "MEDIUM", 1000, 262144,
            "[\"two-pointers\", \"greedy\"]",
            "[\"Use two pointers at both ends.\", \"Move the pointer with the shorter height inward.\"]",
            "Two pointers from both ends. Area = min(height[l], height[r]) * (r - l). Move the shorter side inward. Time: O(n), Space: O(1).",
            adminId
        );
        insertTestCase(id, "9\n1 8 6 2 5 4 8 3 7", "49", true, 0, "Lines at index 1 and 8");
        insertTestCase(id, "2\n1 1", "1", true, 1, "Minimum case");
        insertTestCase(id, "5\n4 3 2 1 4", "16", false, 2, null);
        insertTestCase(id, "3\n1 2 1", "2", false, 3, null);
    }

    private void seedThreeSum(Long adminId) {
        Long id = insertProblem(
            "3Sum",
            "Given an integer array `nums`, return all the triplets [nums[i], nums[j], nums[k]] such that i != j, i != k, and j != k, and nums[i] + nums[j] + nums[k] == 0.\n\nThe solution set must not contain duplicate triplets. Print each triplet sorted, one per line, with triplets sorted lexicographically.",
            "First line: n\nSecond line: n space-separated integers",
            "Each line contains a triplet (space-separated, sorted). Triplets in lexicographic order. Print \"none\" if no triplets found.",
            "3 <= nums.length <= 3000\n-10^5 <= nums[i] <= 10^5",
            "MEDIUM", 2000, 262144,
            "[\"two-pointers\", \"sorting\", \"array\"]",
            "[\"Sort the array first.\", \"Fix one element and use two pointers for the remaining two.\"]",
            "Sort array. For each i, use two-pointer on [i+1, n-1] to find pairs summing to -nums[i]. Skip duplicates. Time: O(n²), Space: O(1) extra.",
            adminId
        );
        insertTestCase(id, "6\n-1 0 1 2 -1 -4", "-1 -1 2\n-1 0 1", true, 0, "Two triplets");
        insertTestCase(id, "3\n0 1 1", "none", true, 1, "No triplet sums to 0");
        insertTestCase(id, "3\n0 0 0", "0 0 0", false, 2, null);
    }

    private void seedRotateMatrix(Long adminId) {
        Long id = insertProblem(
            "Rotate Image",
            "You are given an n x n 2D matrix representing an image. Rotate the image by 90 degrees clockwise.\n\nPrint the rotated matrix.",
            "First line: n\nNext n lines: n space-separated integers per row",
            "n lines of the rotated matrix, space-separated.",
            "1 <= n <= 20\n-1000 <= matrix[i][j] <= 1000",
            "MEDIUM", 1000, 262144,
            "[\"matrix\", \"math\"]",
            "[\"Transpose the matrix, then reverse each row.\", \"Or rotate layer by layer.\"]",
            "Step 1: Transpose (swap matrix[i][j] with matrix[j][i]). Step 2: Reverse each row. Time: O(n²), Space: O(1).",
            adminId
        );
        insertTestCase(id, "3\n1 2 3\n4 5 6\n7 8 9", "7 4 1\n8 5 2\n9 6 3", true, 0, "3x3 rotation");
        insertTestCase(id, "2\n1 2\n3 4", "3 1\n4 2", true, 1, "2x2 rotation");
        insertTestCase(id, "1\n1", "1", false, 2, null);
        insertTestCase(id, "4\n5 1 9 11\n2 4 8 10\n13 3 6 7\n15 14 12 16", "15 13 2 5\n14 3 4 1\n12 6 8 9\n16 7 10 11", false, 3, null);
    }

    private void seedProductExceptSelf(Long adminId) {
        Long id = insertProblem(
            "Product of Array Except Self",
            "Given an integer array `nums`, return an array `answer` such that `answer[i]` is equal to the product of all the elements of `nums` except `nums[i]`.\n\nYou must write an algorithm that runs in O(n) time and without using the division operation.",
            "First line: n\nSecond line: n space-separated integers",
            "Space-separated integers of the product array.",
            "2 <= nums.length <= 10^5\n-30 <= nums[i] <= 30\nThe product of any prefix or suffix fits in a 32-bit integer.",
            "MEDIUM", 1000, 262144,
            "[\"array\", \"prefix-sum\"]",
            "[\"Compute prefix products from left.\", \"Then multiply with suffix products from right.\"]",
            "Two passes: left-to-right prefix products, then right-to-left suffix products multiplied in. Time: O(n), Space: O(1) extra.",
            adminId
        );
        insertTestCase(id, "4\n1 2 3 4", "24 12 8 6", true, 0, "Standard case");
        insertTestCase(id, "5\n-1 1 0 -3 3", "0 0 9 0 0", true, 1, "Contains zero");
        insertTestCase(id, "2\n5 3", "3 5", false, 2, null);
        insertTestCase(id, "3\n0 0 0", "0 0 0", false, 3, null);
    }

    // ---- HARD PROBLEMS ----

    private void seedLongestPalindromicSubstring(Long adminId) {
        Long id = insertProblem(
            "Longest Palindromic Substring",
            "Given a string `s`, return the longest palindromic substring in `s`. If there are multiple with the same length, return the one that appears first.",
            "A single line containing the string s.",
            "A single line containing the longest palindromic substring.",
            "1 <= s.length <= 1000\ns consists of only digits and English letters.",
            "HARD", 2000, 262144,
            "[\"string\", \"dynamic-programming\"]",
            "[\"Expand around center for each character.\", \"Consider both odd and even length palindromes.\"]",
            "Expand around center: for each index, expand outward while characters match. Check both odd/even length. Time: O(n²), Space: O(1).",
            adminId
        );
        insertTestCase(id, "babad", "bab", true, 0, "\"bab\" or \"aba\" — first occurrence");
        insertTestCase(id, "cbbd", "bb", true, 1, "\"bb\"");
        insertTestCase(id, "a", "a", false, 2, null);
        insertTestCase(id, "ac", "a", false, 3, null);
        insertTestCase(id, "racecar", "racecar", false, 4, null);
    }

    private void seedMergeKSortedLists(Long adminId) {
        Long id = insertProblem(
            "Merge K Sorted Arrays",
            "Given k sorted integer arrays, merge them into one sorted array.",
            "First line: k (number of arrays)\nFor each array:\n  First line: n (size)\n  Second line: n space-separated sorted integers",
            "Space-separated integers of the merged sorted array.",
            "1 <= k <= 10^4\n0 <= n <= 500\n-10^4 <= nums[i] <= 10^4\nTotal elements across all arrays <= 10^4.",
            "HARD", 2000, 262144,
            "[\"heap\", \"divide-and-conquer\", \"sorting\"]",
            "[\"Use a min-heap (priority queue) to track the smallest element across arrays.\", \"Alternatively, merge arrays pairwise like merge sort.\"]",
            "Min-heap approach: add first element of each array to heap. Extract min, add next element from that array. Time: O(N log k).",
            adminId
        );
        insertTestCase(id, "3\n3\n1 4 5\n3\n1 3 4\n2\n2 6", "1 1 2 3 4 4 5 6", true, 0, "3 arrays merged");
        insertTestCase(id, "1\n0", "", true, 1, "Single empty array");
        insertTestCase(id, "2\n1\n1\n1\n2", "1 2", false, 2, null);
        insertTestCase(id, "1\n5\n1 2 3 4 5", "1 2 3 4 5", false, 3, null);
    }

    private void seedTrappingRainWater(Long adminId) {
        Long id = insertProblem(
            "Trapping Rain Water",
            "Given n non-negative integers representing an elevation map where the width of each bar is 1, compute how much water it can trap after raining.",
            "First line: n\nSecond line: n space-separated non-negative integers (heights)",
            "A single integer — the total trapped water.",
            "n == height.length\n1 <= n <= 2 * 10^4\n0 <= height[i] <= 10^5",
            "HARD", 1000, 262144,
            "[\"two-pointers\", \"dynamic-programming\", \"stack\"]",
            "[\"For each bar, water above it = min(maxLeft, maxRight) - height[i].\", \"Use two pointers for O(1) space.\"]",
            "Two-pointer approach: maintain leftMax and rightMax. Move the pointer with smaller max inward. Time: O(n), Space: O(1).",
            adminId
        );
        insertTestCase(id, "12\n0 1 0 2 1 0 1 3 2 1 2 1", "6", true, 0, "Classic example: 6 units");
        insertTestCase(id, "6\n4 2 0 3 2 5", "9", true, 1, "9 units trapped");
        insertTestCase(id, "3\n1 2 3", "0", false, 2, null);
        insertTestCase(id, "3\n3 2 1", "0", false, 3, null);
        insertTestCase(id, "1\n5", "0", false, 4, null);
    }

    private void seedNQueens(Long adminId) {
        Long id = insertProblem(
            "N-Queens",
            "The n-queens puzzle is the problem of placing n queens on an n x n chessboard such that no two queens attack each other.\n\nGiven an integer n, return the number of distinct solutions to the n-queens puzzle.",
            "A single integer n.",
            "A single integer — the number of distinct solutions.",
            "1 <= n <= 9",
            "HARD", 5000, 262144,
            "[\"backtracking\", \"recursion\"]",
            "[\"Place queens row by row.\", \"Track attacked columns and diagonals.\"]",
            "Backtracking: place a queen in each row, track columns and diagonals using sets. Time: O(n!), Space: O(n).",
            adminId
        );
        insertTestCase(id, "4", "2", true, 0, "4-Queens has 2 solutions");
        insertTestCase(id, "1", "1", true, 1, "Trivial case");
        insertTestCase(id, "8", "92", false, 2, null);
        insertTestCase(id, "5", "10", false, 3, null);
        insertTestCase(id, "9", "352", false, 4, null);
    }

    private void seedWordBreak(Long adminId) {
        Long id = insertProblem(
            "Word Break",
            "Given a string `s` and a list of words `wordDict`, determine if `s` can be segmented into a space-separated sequence of one or more dictionary words.\n\nReturn \"true\" or \"false\".",
            "First line: the string s\nSecond line: n (number of words in dictionary)\nThird line: n space-separated words",
            "\"true\" or \"false\"",
            "1 <= s.length <= 300\n1 <= wordDict.length <= 1000\n1 <= wordDict[i].length <= 20\nAll strings consist of lowercase English letters.",
            "HARD", 2000, 262144,
            "[\"dynamic-programming\", \"string\"]",
            "[\"Use DP: dp[i] = true if s[0..i-1] can be segmented.\", \"For each position, check all words that could end there.\"]",
            "DP approach: dp[i] means s[0..i-1] can be segmented. For each i, check all j < i where dp[j] is true and s[j..i] is in dict. Time: O(n² * m), Space: O(n).",
            adminId
        );
        insertTestCase(id, "leetcode\n2\nleet code", "true", true, 0, "\"leet\" + \"code\"");
        insertTestCase(id, "applepenapple\n2\napple pen", "true", true, 1, "\"apple\" + \"pen\" + \"apple\"");
        insertTestCase(id, "catsandog\n5\ncats dog sand and cat", "false", false, 2, null);
        insertTestCase(id, "a\n1\na", "true", false, 3, null);
    }

    private void seedSlidingWindowMaximum(Long adminId) {
        Long id = insertProblem(
            "Sliding Window Maximum",
            "You are given an array of integers `nums` and a sliding window of size `k` which moves from the left to the right. You can only see the k numbers in the window. Each time the window moves right by one position.\n\nReturn the max value in each window position.",
            "First line: n k (array size and window size)\nSecond line: n space-separated integers",
            "Space-separated integers — the maximum in each window position.",
            "1 <= nums.length <= 10^5\n-10^4 <= nums[i] <= 10^4\n1 <= k <= nums.length",
            "HARD", 2000, 262144,
            "[\"deque\", \"sliding-window\"]",
            "[\"Use a deque to maintain indices of potential maximums.\", \"Remove elements outside the window and smaller than current.\"]",
            "Monotonic deque: maintain a decreasing deque of indices. For each new element, remove smaller elements from back, remove out-of-window from front. Time: O(n), Space: O(k).",
            adminId
        );
        insertTestCase(id, "8 3\n1 3 -1 -3 5 3 6 7", "3 3 5 5 6 7", true, 0, "Window size 3");
        insertTestCase(id, "1 1\n1", "1", true, 1, "Single element");
        insertTestCase(id, "5 5\n1 2 3 4 5", "5", false, 2, null);
        insertTestCase(id, "5 1\n5 4 3 2 1", "5 4 3 2 1", false, 3, null);
    }

    private void seedMinimumWindowSubstring(Long adminId) {
        Long id = insertProblem(
            "Minimum Window Substring",
            "Given two strings `s` and `t`, return the minimum window substring of `s` such that every character in `t` (including duplicates) is included in the window. If no such substring exists, return an empty string.\n\nIf there are multiple minimum windows, return the one that appears first.",
            "First line: string s\nSecond line: string t",
            "The minimum window substring, or empty line if none exists.",
            "1 <= s.length, t.length <= 10^5\ns and t consist of uppercase and lowercase English letters.",
            "HARD", 2000, 262144,
            "[\"sliding-window\", \"hash-map\", \"string\"]",
            "[\"Use two pointers and a frequency map.\", \"Expand right to include all chars of t, then shrink left to minimize.\"]",
            "Sliding window with char frequency maps. Expand right until all t chars are covered, then shrink left to find minimum. Time: O(n), Space: O(charset).",
            adminId
        );
        insertTestCase(id, "ADOBECODEBANC\nABC", "BANC", true, 0, "Min window containing A,B,C");
        insertTestCase(id, "a\na", "a", true, 1, "Exact match");
        insertTestCase(id, "a\naa", "", false, 2, null);
        insertTestCase(id, "aa\naa", "aa", false, 3, null);
    }
}