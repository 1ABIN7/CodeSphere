package com.CodeSphere.backend.model.interview;

/**
 * Types of interview questions supported by the platform.
 */
public enum InterviewQuestionType {
    MCQ,            // Multiple choice, single correct answer
    MCQ_MULTI,      // Multiple choice, multiple correct answers
    FILL_BLANK,     // Fill in the blank
    TRUE_FALSE,     // True or False question
    SHORT_ANSWER,   // Open-ended short text answer
    CODING,         // Code snippet question (evaluated manually or via judge)
    CASE_STUDY      // Scenario-based question with rubric evaluation
}
