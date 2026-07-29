package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.AssessmentAnswer;
import com.CodeSphere.backend.model.AssessmentSection;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.Question;

import java.util.List;

public interface AssessmentSessionService {

    AssessmentSession startSession(String username, int durationMinutes);

    AssessmentAnswer autoSaveAnswer(Long sessionId, Long questionId, String answerContent);

    AssessmentSession resumeSession(Long sessionId);

    AssessmentSession submitSession(Long sessionId);

    List<Question> getSectionQuestions(Long sessionId, int sectionIndex, int pageSize);

    /**
     * Moves an in-progress session to its next section.  This is deliberately
     * server-side: the browser timer is only a display, while this method owns
     * the start time and expiry of every section.
     */
    AssessmentSession navigateToSection(Long sessionId, int targetSectionIndex);

    List<AssessmentSection> getAssessmentSectionsForSession(Long sessionId);

    void enforceExpirationTimers();
}
