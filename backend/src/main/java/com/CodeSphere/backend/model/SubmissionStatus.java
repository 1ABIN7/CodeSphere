package com.CodeSphere.backend.model;

/**
 * All possible verdicts from the judge engine.
 */
public enum SubmissionStatus {
    PENDING,
    RUNNING,
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILATION_ERROR,
    PARTIALLY_ACCEPTED
}
