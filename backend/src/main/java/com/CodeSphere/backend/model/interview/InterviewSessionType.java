package com.CodeSphere.backend.model.interview;

/**
 * Type of interview session determining scoring behavior.
 */
public enum InterviewSessionType {
    PRACTICE,       // Untimed, shows immediate feedback after each answer
    MOCK_INTERVIEW, // Timed, feedback shown at end only (simulates real interview)
    TIMED_TEST      // Timed, scored like a real test with no hints
}
