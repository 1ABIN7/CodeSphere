package com.CodeSphere.backend.seeder;

import com.CodeSphere.backend.model.ApprovalStatus;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Adds original, single-answer MCQs in the same shape as the Question Bank editor. */
@Component
@Profile("dev")
@Order(4)
public class OriginalMcqQuestionSeeder implements CommandLineRunner {
    private final QuestionBankRepository questions;

    public OriginalMcqQuestionSeeder(QuestionBankRepository questions) { this.questions = questions; }

    @Override public void run(String... args) {
        int added = 0;
        for (Mcq item : items()) {
            if (questions.existsByTitleIgnoreCase(item.title)) continue;
            Question question = new Question();
            question.setTitle(item.title); question.setContent(item.prompt); question.setOptions(item.options);
            question.setCorrectAnswers(item.answer); question.setQuestionType("MCQ_SINGLE"); question.setType("MCQ_SINGLE");
            question.setDifficulty(item.difficulty); question.setCategory(item.category); question.setPoints(3); question.setNegativeScore(0);
            question.setStatus(ApprovalStatus.APPROVED); questions.save(question); added++;
        }
        System.out.printf("[OriginalMcqQuestionSeeder] %d original MCQs added.%n", added);
    }

    private Mcq q(String title, String prompt, String answer, String category, String difficulty, String... options) { return new Mcq(title, prompt, List.of(options), answer, category, difficulty); }
    private record Mcq(String title, String prompt, List<String> options, String answer, String category, String difficulty) { }
    private List<Mcq> items() { return List.of(
        q("Variable Naming Rule", "Which is a valid Java variable name?", "B", "Programming basics", "EASY", "2ndValue", "totalCount", "class", "user-name"),
        q("Boolean Value", "Which literal represents a true Boolean value in Java?", "C", "Programming basics", "EASY", "TRUE", "True", "true", "1"),
        q("Integer Division", "What is the result of 7 / 2 using integer variables?", "A", "Programming basics", "EASY", "3", "3.5", "4", "Error"),
        q("Remainder Operator", "Which operator returns the remainder after division?", "D", "Programming basics", "EASY", "/", "*", "//", "%"),
        q("Array First Index", "What is the first valid index of a Java array?", "A", "Arrays", "EASY", "0", "1", "-1", "Array length"),
        q("Array Length", "Which expression returns the number of elements in an array named values?", "B", "Arrays", "EASY", "values.size()", "values.length", "length(values)", "values.count"),
        q("Loop Repetition", "Which loop is most suitable when the number of repetitions is known?", "C", "Control flow", "EASY", "switch", "while", "for", "try"),
        q("Conditional Branch", "Which keyword introduces an alternative branch after an if statement?", "B", "Control flow", "EASY", "then", "else", "case", "catch"),
        q("String Equality", "In Java, which method compares the contents of two strings?", "D", "Strings", "EASY", "==", "same", "compare", "equals"),
        q("String Immutability", "What happens when a Java String is modified?", "A", "Strings", "EASY", "A new String value is created", "The original memory is overwritten", "The program always errors", "It becomes null"),
        q("Method Return Type", "Which return type is used when a method returns no value?", "C", "Methods", "EASY", "empty", "null", "void", "none"),
        q("Method Parameter", "What is a parameter in a method definition?", "B", "Methods", "EASY", "A value returned by the method", "A named input accepted by the method", "A runtime error", "A class field only"),
        q("Class Blueprint", "What is a class primarily used for?", "A", "Object oriented programming", "EASY", "Defining objects and their behavior", "Running only loops", "Storing one integer", "Replacing a database"),
        q("Object Instance", "An object is best described as what?", "C", "Object oriented programming", "EASY", "A keyword", "A package", "An instance of a class", "A compiler warning"),
        q("Encapsulation Goal", "Which access modifier hides a field from other classes?", "D", "Object oriented programming", "EASY", "public", "protected", "static", "private"),
        q("Inheritance Keyword", "Which Java keyword creates a subclass relationship?", "B", "Object oriented programming", "EASY", "implements", "extends", "inherits", "super"),
        q("Interface Contract", "What does a Java interface mainly provide?", "A", "Object oriented programming", "MEDIUM", "A contract of methods", "A database table", "A loop counter", "A file extension"),
        q("Exception Handling", "Which block runs whether an exception occurs or not?", "C", "Error handling", "EASY", "try", "catch", "finally", "throw"),
        q("Throwing an Exception", "Which keyword explicitly raises an exception?", "B", "Error handling", "EASY", "catch", "throw", "throws", "final"),
        q("List Add Operation", "Which method adds an item to the end of a Java ArrayList?", "D", "Collections", "EASY", "push", "insert", "append", "add"),
        q("Hash Map Lookup", "What is the average lookup time for a value in a hash map?", "A", "Data structures", "MEDIUM", "O(1)", "O(n)", "O(log n)", "O(n²)"),
        q("Stack Behavior", "Which order does a stack use?", "B", "Data structures", "EASY", "First in, first out", "Last in, first out", "Random order", "Sorted order"),
        q("Queue Behavior", "Which order does a queue use?", "C", "Data structures", "EASY", "Last in, first out", "Random order", "First in, first out", "Alphabetical order"),
        q("Binary Search Requirement", "Binary search requires the input data to be what?", "D", "Algorithms", "EASY", "Unique", "Reversed", "Small", "Sorted"),
        q("Linear Search Complexity", "What is the worst-case time complexity of linear search?", "B", "Algorithms", "EASY", "O(1)", "O(n)", "O(log n)", "O(n log n)"),
        q("Merge Sort Complexity", "What is the typical time complexity of merge sort?", "A", "Algorithms", "MEDIUM", "O(n log n)", "O(n²)", "O(1)", "O(log n)"),
        q("Recursion Base Case", "Why does a recursive method need a base case?", "C", "Algorithms", "MEDIUM", "To make it slower", "To sort data", "To stop recursive calls", "To create an array"),
        q("SQL Row Filter", "Which SQL clause filters rows before grouping?", "B", "Databases", "EASY", "ORDER BY", "WHERE", "HAVING", "SELECT"),
        q("SQL Group Filter", "Which SQL clause filters groups after GROUP BY?", "D", "Databases", "MEDIUM", "WHERE", "LIMIT", "ORDER BY", "HAVING"),
        q("Primary Key", "What is the main purpose of a primary key?", "A", "Databases", "EASY", "Uniquely identify each row", "Store duplicate rows", "Sort every query", "Encrypt a table"),
        q("HTTP Success Code", "Which HTTP status code means a request succeeded?", "C", "Web development", "EASY", "404", "500", "200", "401"),
        q("HTTP Not Found", "Which HTTP status code means a resource was not found?", "B", "Web development", "EASY", "201", "404", "302", "503"),
        q("REST Resource", "In REST, a URL usually identifies what?", "D", "Web development", "MEDIUM", "A CSS rule", "A compiler", "A password", "A resource"),
        q("JSON Object", "Which character pair encloses a JSON object?", "A", "Web development", "EASY", "{ }", "[ ]", "( )", "< >"),
        q("Version Control Commit", "What does a Git commit record?", "C", "Version control", "EASY", "A deleted repository", "A remote server", "A saved snapshot of changes", "A new programming language"),
        q("Version Control Branch", "Why create a Git branch?", "B", "Version control", "EASY", "To erase history", "To work on changes independently", "To remove files permanently", "To publish an application"),
        q("Unit Test Purpose", "What is the main purpose of a unit test?", "A", "Testing", "EASY", "Test a small piece of behavior", "Deploy to production", "Replace all manual review", "Create a database"),
        q("Boundary Test", "Which input is most useful for a boundary-value test?", "D", "Testing", "MEDIUM", "A random sentence", "Only a typical input", "A duplicate test", "The smallest or largest allowed value"),
        q("Time Complexity Meaning", "What does Big-O notation describe?", "B", "Algorithms", "EASY", "A variable name", "How resource use grows with input size", "A Java package", "A database key"),
        q("Space Complexity", "What does space complexity measure?", "C", "Algorithms", "EASY", "Number of tests", "Network speed", "Extra memory used", "Lines of code"),
        q("Null Check", "Why check for null before calling a method on an object?", "A", "Programming basics", "EASY", "To avoid a null reference error", "To make code compile faster", "To sort an array", "To close a file"),
        q("Immutable Collection", "Which practice helps avoid unintended changes to shared data?", "D", "Programming basics", "MEDIUM", "Use global variables", "Skip tests", "Always use arrays", "Use immutable values when practical"),
        q("API Authentication", "What is the purpose of an authentication token?", "B", "Web development", "MEDIUM", "To style a web page", "To prove a caller's identity", "To compress JSON", "To replace HTTPS"),
        q("HTTPS Benefit", "What does HTTPS add to HTTP?", "C", "Web development", "EASY", "A database", "A compiler", "Encrypted transport", "A local file"),
        q("Database Index", "What is a common benefit of a database index?", "A", "Databases", "MEDIUM", "Faster lookups for indexed queries", "Automatic backups", "Smaller source files", "No need for keys"),
        q("Normalization Goal", "Why normalize relational database tables?", "B", "Databases", "MEDIUM", "To make every table identical", "To reduce redundant data", "To eliminate all joins", "To remove primary keys"),
        q("Dead Code", "What is dead code?", "D", "Code quality", "EASY", "Code that runs twice", "Code with no comments", "Code in a loop", "Code that can never execute"),
        q("Code Review Value", "What is a key benefit of code review?", "A", "Code quality", "EASY", "Finding issues and sharing knowledge", "Replacing version control", "Avoiding all tests", "Making code private"),
        q("Refactoring Purpose", "What is refactoring intended to improve?", "C", "Code quality", "EASY", "Only application colors", "The number of bugs reported", "Code structure without changing behavior", "Database size only"),
        q("Debugging First Step", "What is a sensible first debugging step?", "B", "Debugging", "EASY", "Rewrite the entire program", "Reproduce and observe the problem", "Delete all tests", "Ignore error messages")
    ); }
}
