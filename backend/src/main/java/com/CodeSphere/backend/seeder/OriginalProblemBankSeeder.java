package com.CodeSphere.backend.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adds an original practice catalogue. These are CodeSphere-authored exercises,
 * not copies of problems, statements, or test cases from a third-party bank.
 */
@Component
@Profile("dev")
@Order(2)
public class OriginalProblemBankSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public OriginalProblemBankSeeder(JdbcTemplate jdbcTemplate) {
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
        for (PracticeProblem problem : problems()) {
            if (seed(problem, adminId)) added++;
        }
        System.out.printf("[OriginalProblemBankSeeder] %d original practice problems added.%n", added);
    }

    private boolean seed(PracticeProblem p, Long adminId) {
        List<Long> existing = jdbcTemplate.query(
                "SELECT id FROM problems WHERE title = ? LIMIT 1", (rs, rowNum) -> rs.getLong(1), p.title());
        if (!existing.isEmpty()) return false;

        jdbcTemplate.update(
                "INSERT INTO problems (title, description, input_format, output_format, constraints, difficulty, " +
                        "time_limit, memory_limit, tags, hints, editorial, is_published, created_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 1000, 262144, ?::jsonb, ?::jsonb, ?, true, ?)",
                p.title(),
                "Write a program that " + p.goal() + ".\n\nRead from standard input and print only the required output. " +
                        "Whitespace at the beginning or end of output is ignored.",
                p.input(), p.output(), p.constraints(), p.difficulty(), jsonArray(p.tags()),
                "[\"Start with the smallest valid input.\", \"Choose an approach that fits the stated constraints.\"]",
                "Derive the result directly from the definition, then account for edge cases.", adminId
        );
        Long id = jdbcTemplate.queryForObject("SELECT id FROM problems WHERE title = ?", Long.class, p.title());
        jdbcTemplate.update(
                "INSERT INTO test_cases (problem_id, input_data, expected_output, is_sample, order_index, explanation) " +
                        "VALUES (?, ?, ?, true, 0, ?)",
                id, p.sampleInput(), p.sampleOutput(), "A representative example.");
        return true;
    }

    private String jsonArray(String tags) {
        return "[\"" + tags.replace(",", "\",\"") + "\"]";
    }

    private record PracticeProblem(String title, String goal, String input, String output, String constraints,
                                   String difficulty, String tags, String sampleInput, String sampleOutput) { }

    private List<PracticeProblem> problems() {
        return List.of(
                // Arrays and arithmetic
                p("Array Total", "print the sum of all numbers in an array", "n followed by n integers", "One integer: the sum.", "1 <= n <= 100000", "EASY", "array,math", "4\n2 4 1 3", "10"),
                p("Smallest Value", "find the smallest number in an array", "n followed by n integers", "One integer: the minimum.", "1 <= n <= 100000", "EASY", "array", "5\n8 -2 6 0 3", "-2"),
                p("Even Number Count", "count the even values in an array", "n followed by n integers", "One integer: the count.", "1 <= n <= 100000", "EASY", "array,math", "6\n1 2 3 4 5 6", "3"),
                p("Odd Number Sum", "sum only the odd values in an array", "n followed by n integers", "One integer: the odd-value sum.", "1 <= n <= 100000", "EASY", "array,math", "5\n4 7 2 9 1", "17"),
                p("Range Width", "print the difference between the largest and smallest array values", "n followed by n integers", "One integer: max minus min.", "1 <= n <= 100000", "EASY", "array", "4\n10 3 14 8", "11"),
                p("Running Totals", "print the cumulative sum after each element", "n followed by n integers", "n space-separated cumulative totals.", "1 <= n <= 100000", "EASY", "array,prefix-sum", "4\n3 1 4 2", "3 4 8 10"),
                p("Positive Product", "multiply all positive values in an array", "n followed by n integers", "One integer: the product, or 1 when there are no positive values.", "1 <= n <= 20; values fit in 64-bit signed integer", "EASY", "array,math", "5\n-2 3 4 0 2", "24"),
                p("Absolute Difference", "print the absolute difference of two integers", "Two integers a and b", "One non-negative integer.", "|a|, |b| <= 10^9", "EASY", "math", "-4 9", "13"),
                p("Digit Total", "sum the decimal digits of a non-negative integer", "One non-negative integer", "One integer: the digit sum.", "0 <= n <= 10^18", "EASY", "math", "50729", "23"),
                p("Power of Two Check", "determine whether a positive integer is a power of two", "One integer n", "true or false.", "1 <= n <= 2^31 - 1", "EASY", "bit-manipulation,math", "64", "true"),
                // Strings
                p("Word Length", "print the number of characters in a line", "One line of text", "One integer: its length.", "0 <= line length <= 100000", "EASY", "string", "codesphere", "10"),
                p("First Character", "print the first character of a non-empty string", "One non-empty line", "One character.", "1 <= length <= 100000", "EASY", "string", "orbit", "o"),
                p("Last Character", "print the last character of a non-empty string", "One non-empty line", "One character.", "1 <= length <= 100000", "EASY", "string", "orbit", "t"),
                p("Uppercase Count", "count uppercase English letters in a line", "One line of text", "One integer: the uppercase count.", "0 <= length <= 100000", "EASY", "string", "CodeSPHERE 2", "6"),
                p("Lowercase Count", "count lowercase English letters in a line", "One line of text", "One integer: the lowercase count.", "0 <= length <= 100000", "EASY", "string", "CodeSPHERE 2", "4"),
                p("Space Count", "count spaces in a line", "One line of text", "One integer: the number of spaces.", "0 <= length <= 100000", "EASY", "string", "write clean code", "2"),
                p("Swap Letter Case", "swap the case of every English letter", "One line of text", "The transformed line.", "0 <= length <= 100000", "EASY", "string", "CodeSphere", "cODEsPHERE"),
                p("Remove Vowels", "remove a, e, i, o, and u in either case from a line", "One line of text", "The remaining characters.", "0 <= length <= 100000", "EASY", "string", "Assessment", "ssssmnt"),
                p("Repeated Character", "find the first character that appears twice while scanning left to right", "One string of lowercase letters", "That character, or - if none repeats.", "1 <= length <= 100000", "EASY", "string,hash-map", "planet", "a"),
                p("Character Frequency", "count occurrences of a specified character", "A text line, then one character on the next line", "One integer: the count.", "0 <= text length <= 100000", "EASY", "string", "banana\na", "3"),
                // Search, sorting, and matrices
                p("Linear Lookup", "find the first index of a target in an array", "n, n integers, then target", "Zero-based index, or -1.", "1 <= n <= 100000", "EASY", "array,search", "5\n3 8 2 8 1\n8", "1"),
                p("Sorted Lookup", "find a target index in a sorted array", "n, n sorted integers, then target", "Zero-based index, or -1.", "1 <= n <= 100000", "EASY", "array,binary-search", "5\n1 3 5 7 9\n7", "3"),
                p("Is Nondecreasing", "determine whether an array is in nondecreasing order", "n followed by n integers", "true or false.", "1 <= n <= 100000", "EASY", "array", "4\n1 2 2 5", "true"),
                p("Second Largest Distinct", "find the second largest distinct value", "n followed by n integers", "One integer, or -1 if it does not exist.", "1 <= n <= 100000", "MEDIUM", "array", "5\n4 7 7 2 5", "5"),
                p("Move Zeroes Right", "move all zeroes to the end while keeping nonzero order", "n followed by n integers", "The rearranged numbers.", "1 <= n <= 100000", "EASY", "array,two-pointers", "5\n0 1 0 3 12", "1 3 12 0 0"),
                p("Unique Sorted Values", "print distinct values from a sorted array", "n followed by n sorted integers", "Distinct values in order.", "1 <= n <= 100000", "EASY", "array,two-pointers", "6\n1 1 2 2 2 5", "1 2 5"),
                p("Row Sum", "print the sum of each matrix row", "r c followed by r rows of c integers", "r space-separated row sums.", "1 <= r,c <= 200", "EASY", "matrix", "2 3\n1 2 3\n4 5 6", "6 15"),
                p("Main Diagonal Total", "sum the main diagonal of a square matrix", "n followed by n rows of n integers", "One integer: the diagonal sum.", "1 <= n <= 300", "EASY", "matrix", "3\n1 2 3\n4 5 6\n7 8 9", "15"),
                p("Matrix Transpose", "print the transpose of a matrix", "r c followed by r rows of c integers", "c rows of r integers.", "1 <= r,c <= 100", "MEDIUM", "matrix", "2 3\n1 2 3\n4 5 6", "1 4\n2 5\n3 6"),
                p("Count Negative Cells", "count negative integers in a matrix", "r c followed by r rows of c integers", "One integer: the count.", "1 <= r,c <= 300", "EASY", "matrix", "2 3\n-1 0 2\n3 -4 -5", "3"),
                // Number theory and recursion
                p("Greatest Common Divisor", "compute the greatest common divisor of two positive integers", "Two positive integers", "One integer: their GCD.", "1 <= a,b <= 10^9", "EASY", "math", "54 24", "6"),
                p("Least Common Multiple", "compute the least common multiple of two positive integers", "Two positive integers", "One integer: their LCM.", "1 <= a,b <= 10^9; result fits 64-bit", "EASY", "math", "12 18", "36"),
                p("Prime Test", "determine whether an integer is prime", "One integer n", "true or false.", "0 <= n <= 10^9", "EASY", "math", "97", "true"),
                p("Prime Count", "count prime numbers from 2 through n inclusive", "One integer n", "One integer: the count.", "0 <= n <= 10^6", "MEDIUM", "math,sieve", "10", "4"),
                p("Decimal to Binary", "convert a non-negative decimal integer to binary", "One non-negative integer", "Its binary representation.", "0 <= n <= 10^9", "EASY", "math,bit-manipulation", "13", "1101"),
                p("Binary Ones", "count set bits in a non-negative integer", "One non-negative integer", "One integer: the number of 1 bits.", "0 <= n <= 2^31 - 1", "EASY", "bit-manipulation", "13", "3"),
                p("Arithmetic Progression Term", "find the nth term of an arithmetic progression", "First term a, difference d, and positive index n", "One integer: the nth term.", "Values fit 64-bit", "EASY", "math", "3 5 4", "18"),
                p("Triangle Number", "compute 1 + 2 + ... + n", "One non-negative integer n", "One integer: the triangular number.", "0 <= n <= 10^9; result fits 64-bit", "EASY", "math", "8", "36"),
                p("Digit Reverse", "reverse the decimal digits of a non-negative integer, dropping leading zeroes", "One non-negative integer", "The reversed integer.", "0 <= n <= 10^18", "EASY", "math", "12040", "4021"),
                p("Armstrong Check", "determine whether a three-digit number equals the sum of its cubed digits", "One three-digit integer", "true or false.", "100 <= n <= 999", "EASY", "math", "153", "true"),
                // Stack, queue, and intervals
                p("Balanced Square Brackets", "determine whether square brackets are balanced", "One string containing [ and ]", "true or false.", "0 <= length <= 100000", "EASY", "stack,string", "[[]][]", "true"),
                p("Undo Sequence", "process commands PUSH x and POP, then print the stack top", "m commands, one per line", "Top value, or EMPTY.", "1 <= m <= 100000", "MEDIUM", "stack", "4\nPUSH 3\nPUSH 8\nPOP\nPUSH 5", "5"),
                p("Queue Front", "process ENQUEUE x and DEQUEUE commands, then print the queue front", "m commands, one per line", "Front value, or EMPTY.", "1 <= m <= 100000", "MEDIUM", "queue", "3\nENQUEUE 4\nENQUEUE 9\nDEQUEUE", "9"),
                p("Next Greater Value", "for each value, print the next greater value to its right", "n followed by n integers", "n answers; use -1 when none exists.", "1 <= n <= 100000", "MEDIUM", "stack,array", "4\n2 1 2 4", "4 2 4 -1"),
                p("Interval Overlap", "determine whether two closed intervals overlap", "Four integers a b c d for [a,b] and [c,d]", "true or false.", "a <= b and c <= d", "EASY", "intervals", "1 5 5 9", "true"),
                p("Merge Touching Intervals", "merge sorted intervals that overlap or touch", "n followed by n pairs start end sorted by start", "Merged intervals, one per line.", "1 <= n <= 100000", "MEDIUM", "intervals,sorting", "3\n1 3\n3 5\n8 10", "1 5\n8 10"),
                p("Meeting Room Need", "determine whether all meetings can use one room", "n followed by n start/end pairs", "true or false.", "1 <= n <= 100000", "MEDIUM", "intervals,sorting", "3\n0 10\n10 20\n21 30", "true"),
                p("Minimum Meeting Rooms", "find the fewest rooms needed for meetings", "n followed by n start/end pairs", "One integer: number of rooms.", "1 <= n <= 100000", "MEDIUM", "intervals,heap", "3\n0 30\n5 10\n15 20", "2"),
                p("Circular Shift", "rotate an array right by k positions", "n k followed by n integers", "The rotated array.", "1 <= n <= 100000; 0 <= k <= 10^9", "MEDIUM", "array", "5 2\n1 2 3 4 5", "4 5 1 2 3"),
                p("Rotate Text", "rotate a string right by k characters", "A string then integer k", "The rotated string.", "1 <= length <= 100000", "MEDIUM", "string", "codesphere\n3", "erecodesph"),
                // Hashing and two pointers
                p("Distinct Count", "count distinct integers in an array", "n followed by n integers", "One integer: number of distinct values.", "1 <= n <= 100000", "EASY", "hash-map,array", "6\n1 2 2 3 1 4", "4"),
                p("Pair With Difference", "determine whether two array values differ by exactly k", "n k followed by n integers", "true or false.", "1 <= n <= 100000; k >= 0", "MEDIUM", "hash-map,array", "5 3\n1 4 7 2 9", "true"),
                p("Most Frequent Value", "find the most frequent integer, choosing the smaller value on ties", "n followed by n integers", "One integer.", "1 <= n <= 100000", "MEDIUM", "hash-map,array", "6\n4 2 4 2 2 7", "2"),
                p("Common Values", "print distinct values shared by two arrays in ascending order", "n, first array, m, second array", "Shared values separated by spaces, or NONE.", "1 <= n,m <= 100000", "MEDIUM", "hash-map,array", "4\n1 2 2 5\n3\n2 5 7", "2 5"),
                p("Two Array Sum", "determine whether one value from each array sums to a target", "n, first array, m, second array, target", "true or false.", "1 <= n,m <= 100000", "MEDIUM", "hash-map,array", "2\n1 5\n3\n2 4 7\n9", "true"),
                p("Pair Count Target", "count index pairs whose values sum to target", "n, n integers, then target", "One integer: number of pairs.", "1 <= n <= 100000", "MEDIUM", "hash-map,array", "5\n1 5 7 -1 5\n6", "3"),
                p("Longest Equal Run", "find the longest contiguous run of equal values", "n followed by n integers", "One integer: maximum run length.", "1 <= n <= 100000", "EASY", "array", "7\n1 1 2 2 2 3 3", "3"),
                p("Zero Sum Prefix", "determine whether any non-empty subarray sums to zero", "n followed by n integers", "true or false.", "1 <= n <= 100000", "MEDIUM", "prefix-sum,hash-map", "5\n4 2 -3 1 6", "true"),
                p("Subarray Target Count", "count contiguous subarrays with sum equal to target", "n, n integers, then target", "One integer: the count.", "1 <= n <= 100000", "MEDIUM", "prefix-sum,hash-map", "3\n1 1 1\n2", "2"),
                p("Shortest Covering Segment", "find the shortest segment containing all required characters", "A text string then a required-character string", "The length, or -1 if impossible.", "1 <= lengths <= 100000", "HARD", "sliding-window,string", "abacb\nabc", "3"),
                // Linked lists and trees (array input/output representation)
                p("List Middle Value", "print the middle value of a list, using the second middle for even length", "n followed by n values in list order", "One integer: the middle value.", "1 <= n <= 100000", "EASY", "linked-list,two-pointers", "4\n2 4 6 8", "6"),
                p("List Reverse", "reverse a list represented by values in order", "n followed by n integers", "Values in reverse order.", "1 <= n <= 100000", "EASY", "linked-list", "4\n1 2 3 4", "4 3 2 1"),
                p("List Cycle Marker", "determine whether a list description contains a cycle", "n followed by a next-index for every node; -1 means null", "true or false.", "1 <= n <= 100000", "MEDIUM", "linked-list", "3\n1 2 1", "true"),
                p("Binary Tree Node Count", "count non-null nodes in level-order tree input", "n followed by n tokens; # means empty", "One integer: node count.", "1 <= n <= 100000", "EASY", "tree", "7\n1 2 3 # 4 # 5", "5"),
                p("Binary Tree Leaf Count", "count leaves in a level-order binary tree", "n followed by n tokens; # means empty", "One integer: leaf count.", "1 <= n <= 100000", "MEDIUM", "tree", "7\n1 2 3 # 4 # 5", "2"),
                p("Tree Maximum", "find the largest value in a non-empty level-order binary tree", "n followed by n integer/# tokens", "One integer: the maximum.", "1 <= n <= 100000", "EASY", "tree", "7\n5 2 9 # 4 7 12", "12"),
                p("Tree Level Sum", "sum all values at a requested depth in a binary tree", "n, level-order tree tokens, then depth", "One integer: the sum.", "1 <= n <= 100000", "MEDIUM", "tree,bfs", "7\n1 2 3 4 5 # 6\n2", "15"),
                p("Tree Height", "find the height in nodes of a level-order binary tree", "n followed by n integer/# tokens", "One integer: the height.", "1 <= n <= 100000", "MEDIUM", "tree", "7\n1 2 3 4 5 # 6", "3"),
                p("Lowest Shared Ancestor", "find the lowest shared ancestor of two values in a binary search tree", "n sorted-insertion values, then x y", "One integer: ancestor value.", "1 <= n <= 100000", "MEDIUM", "tree,bst", "7\n6 2 8 0 4 7 9\n2 4", "2"),
                p("BST Validity", "determine whether a level-order tree satisfies strict BST ordering", "n followed by n integer/# tokens", "true or false.", "1 <= n <= 100000", "MEDIUM", "tree,bst", "7\n4 2 6 1 3 5 7", "true"),
                // Graphs and dynamic programming
                p("Graph Edge Count", "count undirected graph edges", "n m followed by m endpoint pairs", "One integer: m.", "1 <= n <= 100000; 0 <= m <= 200000", "EASY", "graph", "4 3\n1 2\n2 3\n3 4", "3"),
                p("Graph Neighbor Count", "count neighbors of a requested vertex in an undirected graph", "n m, m edges, then vertex v", "One integer: degree of v.", "1 <= n <= 100000; 0 <= m <= 200000", "EASY", "graph", "4 3\n1 2\n2 3\n2 4\n2", "3"),
                p("Reachability Check", "determine whether a path exists between two graph vertices", "n m, m undirected edges, then source target", "true or false.", "1 <= n <= 100000; 0 <= m <= 200000", "MEDIUM", "graph,bfs", "5 3\n1 2\n2 3\n4 5\n1 3", "true"),
                p("Shortest Unweighted Path", "find the fewest edges between two vertices in an unweighted graph", "n m, m undirected edges, then source target", "One integer, or -1.", "1 <= n <= 100000; 0 <= m <= 200000", "MEDIUM", "graph,bfs", "5 4\n1 2\n2 3\n3 5\n1 4\n4 5", "3"),
                p("Connected Components", "count connected components in an undirected graph", "n m followed by m edges", "One integer: component count.", "1 <= n <= 100000; 0 <= m <= 200000", "MEDIUM", "graph,dfs", "5 2\n1 2\n4 5", "3"),
                p("Course Order Possible", "determine whether directed prerequisites contain a cycle", "n m followed by directed edges prerequisite course", "true or false.", "1 <= n <= 100000; 0 <= m <= 200000", "MEDIUM", "graph,topological-sort", "3 2\n1 2\n2 3", "true"),
                p("Grid Island Count", "count four-direction connected islands of 1s", "r c followed by r strings of 0 and 1", "One integer: island count.", "1 <= r,c <= 300", "MEDIUM", "graph,grid", "3 3\n110\n010\n011", "1"),
                p("Grid Shortest Walk", "find shortest four-direction walk from S to E through open cells", "r c followed by grid rows using S, E, ., #", "One integer, or -1.", "1 <= r,c <= 300", "MEDIUM", "graph,bfs,grid", "3 3\nS..\n##.\n..E", "4"),
                p("Staircase Ways", "count ways to climb n steps using one or two steps at a time", "One non-negative integer n", "One integer: number of ways.", "0 <= n <= 45", "EASY", "dynamic-programming", "4", "5"),
                p("Minimum Coin Count", "find the fewest coins needed to make a target amount", "n coin values, then target", "One integer, or -1.", "1 <= n <= 100; 0 <= target <= 10000", "MEDIUM", "dynamic-programming", "3\n1 3 4\n6", "2"),
                p("Nonadjacent Maximum", "find the maximum sum with no adjacent selected values", "n followed by n non-negative integers", "One integer: the maximum sum.", "1 <= n <= 100000", "MEDIUM", "dynamic-programming", "5\n2 7 9 3 1", "12"),
                p("Grid Minimum Cost", "find the minimum cost path from top-left to bottom-right moving right or down", "r c followed by non-negative costs", "One integer: minimum cost.", "1 <= r,c <= 300", "MEDIUM", "dynamic-programming,grid", "2 3\n1 3 1\n1 5 1", "6"),
                p("Longest Increasing Run", "find the longest strictly increasing contiguous run", "n followed by n integers", "One integer: longest length.", "1 <= n <= 100000", "EASY", "array,dynamic-programming", "6\n1 2 3 2 4 5", "3"),
                p("Edit Distance Lite", "find the minimum insertions or deletions needed to make two strings equal", "Two lowercase strings on separate lines", "One integer: the minimum operations.", "1 <= lengths <= 500", "HARD", "dynamic-programming,string", "cat\ncut", "2"),
                p("Longest Common Subsequence Length", "find the length of a longest common subsequence of two strings", "Two strings on separate lines", "One integer: the LCS length.", "1 <= lengths <= 500", "MEDIUM", "dynamic-programming,string", "stone\nlongest", "3"),
                p("Palindrome Insertions", "find the fewest insertions needed to make a string a palindrome", "One lowercase string", "One integer: minimum insertions.", "1 <= length <= 500", "HARD", "dynamic-programming,string", "abca", "1"),
                p("Subset Total Possible", "determine whether a subset sums exactly to target", "n, n non-negative integers, then target", "true or false.", "1 <= n <= 100; 0 <= target <= 10000", "MEDIUM", "dynamic-programming", "4\n3 34 4 12\n7", "true"),
                p("Knapsack Value", "maximize value within a weight capacity using each item at most once", "n, n weight/value pairs, then capacity", "One integer: maximum value.", "1 <= n <= 100; capacity <= 10000", "HARD", "dynamic-programming", "3\n2 3\n3 4\n4 5\n5", "7"),
                p("String Decode Count", "count valid ways to split digits into values 1 through 26", "A string of digits", "One integer: number of decodings.", "1 <= length <= 100", "MEDIUM", "dynamic-programming,string", "226", "3"),
                p("Longest Alternating Run", "find the longest contiguous run whose adjacent values alternate up and down", "n followed by n integers", "One integer: longest length.", "1 <= n <= 100000", "MEDIUM", "array,dynamic-programming", "5\n1 3 2 4 3", "5"),
                p("Minimum Jumps", "find the fewest jumps to reach the last index, where each value is maximum jump length", "n followed by n non-negative integers", "One integer, or -1 if unreachable.", "1 <= n <= 100000", "MEDIUM", "greedy,array", "5\n2 3 1 1 4", "2"),
                p("Rain Gauge Total", "compute trapped rainwater between non-negative bar heights", "n followed by n heights", "One integer: trapped water.", "1 <= n <= 100000", "HARD", "two-pointers,array", "6\n3 0 2 0 4 1", "7"),
                p("Window Maximum Sum", "find the greatest sum of any consecutive window of size k", "n k followed by n integers", "One integer: maximum window sum.", "1 <= k <= n <= 100000", "MEDIUM", "sliding-window,array", "5 3\n2 1 5 1 3", "8"),
                p("Window Distinct Count", "print the number of distinct values in each window of size k", "n k followed by n integers", "Counts separated by spaces.", "1 <= k <= n <= 100000", "MEDIUM", "sliding-window,hash-map", "5 3\n1 2 1 3 4", "2 3 3"),
                p("String Compression Length", "compute the length of run-length encoding without building it", "One non-empty lowercase string", "One integer: encoded length using count only when greater than one.", "1 <= length <= 100000", "MEDIUM", "string", "aabccc", "5"),
                p("First Missing Positive", "find the smallest positive integer absent from an array", "n followed by n integers", "One integer.", "1 <= n <= 100000", "HARD", "array", "4\n3 4 -1 1", "2"),
                p("Rectangle Union Area", "compute the combined area of two axis-aligned rectangles", "Eight integers x1 y1 x2 y2 x3 y3 x4 y4", "One integer: union area.", "Coordinates fit 32-bit; answer fits 64-bit", "MEDIUM", "math,geometry", "0 0 2 2\n1 1 3 3", "7"),
                p("Clock Angle", "find the smaller angle between hour and minute hands", "Hour h and minute m", "One number with at most one decimal place.", "0 <= h <= 23; 0 <= m <= 59", "MEDIUM", "math", "3 30", "75.0"),
                p("Roman Digit Total", "convert a simplified Roman numeral to an integer", "One valid Roman numeral using I,V,X,L,C,D,M", "One integer.", "1 <= length <= 30", "MEDIUM", "string,math", "MCMIV", "1904"),
                p("Date Day Offset", "add a non-negative number of days to a date in YYYY-MM-DD format", "A date then integer days", "Resulting date in YYYY-MM-DD.", "Dates are Gregorian dates between 2000 and 2099", "MEDIUM", "date,math", "2024-02-27\n3", "2024-03-01")
        );
    }

    private PracticeProblem p(String title, String goal, String input, String output, String constraints,
                              String difficulty, String tags, String sampleInput, String sampleOutput) {
        return new PracticeProblem(title, goal, input, output, constraints, difficulty, tags, sampleInput, sampleOutput);
    }
}
