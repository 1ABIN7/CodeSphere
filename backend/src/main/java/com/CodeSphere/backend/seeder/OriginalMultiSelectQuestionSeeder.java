package com.CodeSphere.backend.seeder;

import com.CodeSphere.backend.model.ApprovalStatus;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Adds original multiple-select questions with one to four correct labels. */
@Component
@Profile("dev")
@Order(5)
public class OriginalMultiSelectQuestionSeeder implements CommandLineRunner {
    private final QuestionBankRepository questions;
    public OriginalMultiSelectQuestionSeeder(QuestionBankRepository questions) { this.questions = questions; }
    @Override public void run(String... args) { int added = 0; for (Multi item : items()) { if (questions.existsByTitleIgnoreCase(item.title)) continue; Question q = new Question(); q.setTitle(item.title); q.setContent(item.prompt); q.setOptions(item.options); q.setCorrectAnswers(item.answers); q.setQuestionType("MCQ_MULTI"); q.setType("MCQ_MULTI"); q.setDifficulty(item.difficulty); q.setCategory(item.category); q.setPoints(4); q.setNegativeScore(0); q.setStatus(ApprovalStatus.APPROVED); questions.save(q); added++; } System.out.printf("[OriginalMultiSelectQuestionSeeder] %d original multiple-select questions added.%n", added); }
    private Multi q(String title, String prompt, String answers, String category, String difficulty, String... options) { return new Multi(title, prompt, List.of(options), answers, category, difficulty); }
    private record Multi(String title, String prompt, List<String> options, String answers, String category, String difficulty) { }
    private List<Multi> items() { return List.of(
        q("Java Primitive Types", "Select all Java primitive types.", "A, C, D", "Programming basics", "EASY", "int", "String", "boolean", "double"),
        q("Java Collection Interfaces", "Select the interfaces in the Java Collections Framework.", "A, B, D", "Collections", "MEDIUM", "List", "Set", "Integer", "Map"),
        q("Valid Boolean Operators", "Select operators commonly used for Boolean logic in Java.", "A, C, D", "Programming basics", "EASY", "&&", "+", "||", "!"),
        q("Loop Statements", "Select Java loop statements.", "A, B, C", "Control flow", "EASY", "for", "while", "do-while", "switch"),
        q("Conditional Keywords", "Select keywords used for conditional branching in Java.", "A, B, D", "Control flow", "EASY", "if", "else", "repeat", "switch"),
        q("Array Facts", "Select true statements about Java arrays.", "A, C", "Arrays", "EASY", "They use zero-based indexes", "Their length changes automatically", "They have a length field", "They only store strings"),
        q("String Facts", "Select true statements about Java String values.", "A, B, D", "Strings", "EASY", "They are immutable", "equals compares contents", "They are primitive types", "They can be concatenated"),
        q("Method Components", "Select items that can appear in a Java method declaration.", "A, B, C, D", "Methods", "EASY", "Return type", "Method name", "Parameters", "Access modifier"),
        q("Object Oriented Concepts", "Select core object-oriented programming concepts.", "A, B, C, D", "Object oriented programming", "MEDIUM", "Encapsulation", "Inheritance", "Polymorphism", "Abstraction"),
        q("Access Modifiers", "Select Java access modifiers.", "A, B, C", "Object oriented programming", "public", "private", "protected", "visible"),
        q("Exception Blocks", "Select blocks used in Java exception handling.", "A, B, D", "Error handling", "MEDIUM", "try", "catch", "rescue", "finally"),
        q("Runtime Exceptions", "Select examples of runtime exceptions in Java.", "A, C", "Error handling", "MEDIUM", "NullPointerException", "IOException", "ArithmeticException", "SQLException"),
        q("Stack Operations", "Select common stack operations.", "A, B, D", "Data structures", "EASY", "push", "pop", "enqueue", "peek"),
        q("Queue Operations", "Select common queue operations.", "A, C", "Data structures", "EASY", "enqueue", "push", "dequeue", "compile"),
        q("Hash Map Properties", "Select typical hash map properties.", "A, B, D", "Data structures", "MEDIUM", "Stores key-value pairs", "Keys are unique", "Maintains sorted keys by default", "Average lookup can be O(1)"),
        q("Binary Search Facts", "Select true statements about binary search.", "A, C, D", "Algorithms", "MEDIUM", "Input should be sorted", "It always checks every item", "It halves the search range", "Worst case is O(log n)"),
        q("Sorting Algorithms", "Select comparison-based sorting algorithms.", "A, B, C", "Algorithms", "MEDIUM", "Merge sort", "Insertion sort", "Quick sort", "Binary search"),
        q("Recursion Rules", "Select good practices when writing recursive code.", "A, B, D", "Algorithms", "MEDIUM", "Define a base case", "Move toward the base case", "Always use global state", "Test small inputs"),
        q("Big O Linear", "Select operations that are commonly O(n) for an array of n items.", "A, C", "Algorithms", "MEDIUM", "Scan every element", "Read a known index", "Find a value with linear search", "Access the first item"),
        q("SQL Data Commands", "Select SQL commands that change data.", "A, B, C", "Databases", "EASY", "INSERT", "UPDATE", "DELETE", "SELECT"),
        q("SQL Join Types", "Select valid SQL join types.", "A, B, D", "Databases", "EASY", "INNER JOIN", "LEFT JOIN", "MIDDLE JOIN", "RIGHT JOIN"),
        q("Database Constraints", "Select common relational database constraints.", "A, B, C", "Databases", "MEDIUM", "PRIMARY KEY", "FOREIGN KEY", "NOT NULL", "RENDER"),
        q("Index Tradeoffs", "Select likely effects of adding a database index.", "A, B", "Databases", "MEDIUM", "Can speed up reads", "Can add write overhead", "Removes all duplicate data", "Eliminates table scans entirely"),
        q("HTTP Request Methods", "Select common HTTP request methods.", "A, B, C, D", "Web development", "EASY", "GET", "POST", "PUT", "DELETE"),
        q("Successful HTTP Codes", "Select HTTP status codes in the successful 2xx family.", "A, C", "Web development", "EASY", "200", "404", "201", "500"),
        q("Client Error Codes", "Select HTTP status codes that are client errors.", "A, B, D", "Web development", "MEDIUM", "400", "401", "200", "404"),
        q("JSON Value Types", "Select valid JSON value types.", "A, B, C, D", "Web development", "EASY", "String", "Number", "Boolean", "Array"),
        q("REST Design Practices", "Select common REST design practices.", "A, C, D", "Web development", "MEDIUM", "Use resource-oriented URLs", "Put passwords in URLs", "Use HTTP methods meaningfully", "Return suitable status codes"),
        q("Git Working Areas", "Select areas involved in a normal Git commit workflow.", "A, B, C", "Version control", "EASY", "Working directory", "Staging area", "Repository", "Browser cache"),
        q("Git Collaboration Commands", "Select Git commands useful for collaboration.", "A, B, D", "Version control", "EASY", "pull", "push", "commit", "render"),
        q("Good Commit Traits", "Select traits of a good source-control commit.", "A, C, D", "Version control", "MEDIUM", "Focused purpose", "Contains unrelated changes", "Clear message", "Builds or is reviewable"),
        q("Unit Test Qualities", "Select qualities of a useful unit test.", "A, B, D", "Testing", "MEDIUM", "Fast", "Repeatable", "Depends on random external state", "Tests one behavior clearly"),
        q("Test Input Categories", "Select useful test-input categories.", "A, B, C, D", "Testing", "EASY", "Typical values", "Boundary values", "Invalid values", "Empty values"),
        q("Debugging Activities", "Select helpful debugging activities.", "A, B, D", "Debugging", "EASY", "Reproduce the issue", "Inspect error output", "Ignore failing tests", "Use a minimal example"),
        q("Code Review Checks", "Select items reviewers commonly check.", "A, B, C", "Code quality", "MEDIUM", "Correctness", "Readability", "Tests", "Keyboard brand"),
        q("Refactoring Examples", "Select examples of refactoring.", "A, C, D", "Code quality", "MEDIUM", "Extract a method", "Change required behavior", "Rename unclear variables", "Remove duplicated code"),
        q("Secure Password Handling", "Select safer password-handling practices.", "A, B, D", "Security", "MEDIUM", "Hash passwords", "Use a modern password hash", "Store plain text passwords", "Use unique salts"),
        q("Authentication Factors", "Select examples of authentication factors.", "A, B, C", "Security", "MEDIUM", "Something you know", "Something you have", "Something you are", "A page color"),
        q("HTTPS Protections", "Select protections HTTPS helps provide in transit.", "A, B", "Security", "MEDIUM", "Encryption", "Integrity", "Guaranteed bug-free code", "Unlimited storage"),
        q("Clean Function Traits", "Select traits of a clean function.", "A, C, D", "Code quality", "EASY", "Has a focused purpose", "Does many unrelated jobs", "Uses meaningful names", "Has understandable inputs and outputs"),
        q("Readable Variable Names", "Select readable variable names.", "A, B, D", "Code quality", "EASY", "totalPrice", "isLoggedIn", "x1z9", "candidateCount"),
        q("API Response Elements", "Select elements often present in an API response.", "A, B, C", "Web development", "EASY", "Status code", "Headers", "Body", "Keyboard shortcut"),
        q("Database Transaction Traits", "Select common ACID transaction properties.", "A, B, C, D", "Databases", "HARD", "Atomicity", "Consistency", "Isolation", "Durability"),
        q("Algorithm Correctness", "Select ways to build confidence in an algorithm.", "A, B, D", "Algorithms", "MEDIUM", "Reason about edge cases", "Test representative inputs", "Assume it works without testing", "Compare against a simple solution"),
        q("Memory Management", "Select actions that can reduce unnecessary memory use.", "A, C", "Programming basics", "MEDIUM", "Release unused references", "Duplicate large data needlessly", "Stream large input when appropriate", "Store every value twice"),
        q("Concurrency Concerns", "Select concerns when multiple threads share mutable data.", "A, B, D", "Programming basics", "HARD", "Race conditions", "Visibility", "Guaranteed ordering", "Synchronization"),
        q("Input Validation", "Select benefits of validating user input.", "A, B, C", "Security", "EASY", "Prevents malformed data", "Improves error messages", "Reduces some security risks", "Guarantees every request is authorized"),
        q("Deployment Checks", "Select useful checks before deploying an application.", "A, B, D", "DevOps", "MEDIUM", "Run tests", "Review configuration", "Skip monitoring", "Verify migrations"),
        q("Monitoring Signals", "Select useful application monitoring signals.", "A, B, C", "DevOps", "MEDIUM", "Error rate", "Latency", "Resource usage", "Font preference"),
        q("Documentation Benefits", "Select benefits of good technical documentation.", "A, C, D", "Code quality", "EASY", "Easier onboarding", "More runtime memory", "Clearer usage", "Fewer repeated questions"),
        q("Agile Retrospective", "Select useful outcomes of a team retrospective.", "A, B, D", "Teamwork", "EASY", "Identify improvements", "Discuss what worked", "Assign blame", "Agree on actions")
    ); }
}
