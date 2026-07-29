package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.submission.JudgeResultResponse;
import com.CodeSphere.backend.dto.submission.SubmissionRequest;
import com.CodeSphere.backend.dto.submission.SubmissionResponse;
import com.CodeSphere.backend.model.Difficulty;
import com.CodeSphere.backend.model.Problem;
import com.CodeSphere.backend.model.Submission;
import com.CodeSphere.backend.model.SubmissionStatus;
import com.CodeSphere.backend.model.TestCase;
import com.CodeSphere.backend.repository.ProblemRepository;
import com.CodeSphere.backend.repository.SubmissionRepository;
import com.CodeSphere.backend.repository.SubmissionResultRepository;
import com.CodeSphere.backend.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private SubmissionResultRepository submissionResultRepository;
    @Mock private ProblemRepository problemRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private JudgeEngineService judgeEngineService;
    @Mock private CodeAnalysisService codeAnalysisService;
    @Mock private SkillScoreService skillScoreService;

    @InjectMocks
    private SubmissionService submissionService;

    private SubmissionRequest request;
    private Problem publishedProblem;
    private TestCase sampleTestCase;

    @BeforeEach
    void setUp() {
        request = new SubmissionRequest();
        request.setProblemId(1L);
        request.setLanguage("java");
        request.setCode("public class Main { public static void main(String[] a) {} }");

        publishedProblem = Problem.builder()
                .id(1L)
                .title("Two Sum")
                .difficulty(Difficulty.EASY)
                .isPublished(true)
                .timeLimit(1000)
                .memoryLimit(262144)
                .totalSubmissions(0)
                .acceptedSubmissions(0)
                .build();

        sampleTestCase = TestCase.builder()
                .id(200L)
                .isSample(true)
                .inputData("1 2")
                .expectedOutput("3")
                .build();
    }

    // ---- Validation guards ----

    @Test
    void submitCode_ThrowsException_WhenProblemNotFound() {
        when(problemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> submissionService.submitCode(request, 10L));
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void submitCode_ThrowsException_WhenProblemNotPublished() {
        publishedProblem.setIsPublished(false);
        when(problemRepository.findById(1L)).thenReturn(Optional.of(publishedProblem));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> submissionService.submitCode(request, 10L));

        assertTrue(exception.getMessage().contains("unpublished"));
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void submitCode_ThrowsException_WhenLanguageNotSupported() {
        request.setLanguage("rust");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(publishedProblem));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> submissionService.submitCode(request, 10L));

        assertTrue(exception.getMessage().contains("Unsupported language"));
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void submitCode_ThrowsException_WhenNoTestCasesConfigured() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(publishedProblem));
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> submissionService.submitCode(request, 10L));

        assertTrue(exception.getMessage().contains("no test cases"));
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    // ---- Judging flow (happy path) ----

    @Test
    void submitCode_Success_AcceptedUpdatesStatsAndSkillScore() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(publishedProblem));
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(sampleTestCase));

        // First save returns the RUNNING submission with an assigned ID; second save is the post-judge update
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(500L);
            }
            return s;
        });

        JudgeEngineService.TestCaseExecResult tcResult = new JudgeEngineService.TestCaseExecResult(
                200L, SubmissionStatus.ACCEPTED, "3", 50, 1024, null);
        JudgeEngineService.JudgeResult judgeResult = new JudgeEngineService.JudgeResult(
                SubmissionStatus.ACCEPTED, List.of(tcResult), 1, 1, 100, 50, 1024, null);

        when(judgeEngineService.judge(anyString(), anyString(), anyList(), anyInt(), anyInt()))
                .thenReturn(judgeResult);

        JudgeResultResponse analysis = JudgeResultResponse.builder()
                .codeQualityScore(80)
                .codeQualityIssues(List.of())
                .antiPatterns(List.of())
                .optimizationSuggestions(List.of())
                .performanceHints(List.of())
                .estimatedTimeComplexity("O(n)")
                .estimatedSpaceComplexity("O(1)")
                .build();
        when(codeAnalysisService.analyze(anyString(), anyString(), any(), anyInt(), anyInt()))
                .thenReturn(analysis);

        when(submissionResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResponse response = submissionService.submitCode(request, 10L);

        assertEquals("ACCEPTED", response.getStatus());
        assertEquals(1, response.getTestCasesPassed());

        // Problem stats must be updated on an accepted submission
        ArgumentCaptor<Problem> problemCaptor = ArgumentCaptor.forClass(Problem.class);
        verify(problemRepository).save(problemCaptor.capture());
        assertEquals(1, problemCaptor.getValue().getTotalSubmissions());
        assertEquals(1, problemCaptor.getValue().getAcceptedSubmissions());

        // Skill score only updates on acceptance, and only when a user is attached
        verify(skillScoreService).updateScoresOnAccepted(10L, publishedProblem);
    }

    @Test
    void submitCode_DoesNotUpdateSkillScore_WhenNotAccepted() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(publishedProblem));
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(sampleTestCase));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            if (s.getId() == null) s.setId(501L);
            return s;
        });

        JudgeEngineService.TestCaseExecResult tcResult = new JudgeEngineService.TestCaseExecResult(
                200L, SubmissionStatus.WRONG_ANSWER, "4", 50, 1024, null);
        JudgeEngineService.JudgeResult judgeResult = new JudgeEngineService.JudgeResult(
                SubmissionStatus.WRONG_ANSWER, List.of(tcResult), 0, 1, 0, 50, 1024, null);

        when(judgeEngineService.judge(anyString(), anyString(), anyList(), anyInt(), anyInt()))
                .thenReturn(judgeResult);
        when(codeAnalysisService.analyze(anyString(), anyString(), any(), anyInt(), anyInt()))
                .thenReturn(JudgeResultResponse.builder().build());
        when(submissionResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResponse response = submissionService.submitCode(request, 10L);

        assertEquals("WRONG_ANSWER", response.getStatus());
        verify(skillScoreService, never()).updateScoresOnAccepted(any(), any());
    }

    // ---- Owner-only access ----

    @Test
    void getSubmission_ThrowsSecurityException_WhenNotOwner() {
        Submission submission = Submission.builder().id(1L).userId(10L).problemId(1L).build();
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        assertThrows(SecurityException.class, () -> submissionService.getSubmission(1L, 99L));
        verify(problemRepository, never()).findById(any());
    }

    @Test
    void getSubmission_Success_WhenRequestedByOwner() {
        Submission submission = Submission.builder()
                .id(1L).userId(10L).problemId(1L)
                .status(SubmissionStatus.ACCEPTED)
                .build();
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
        when(problemRepository.findById(1L)).thenReturn(Optional.of(publishedProblem));
        when(submissionResultRepository.findBySubmissionIdOrderByTestCaseIdAsc(1L)).thenReturn(List.of());
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        SubmissionResponse response = submissionService.getSubmission(1L, 10L);

        assertEquals(1L, response.getId());
    }

    @Test
    void getAnalysis_ThrowsSecurityException_WhenNotOwner() {
        Submission submission = Submission.builder().id(1L).userId(10L).problemId(1L).build();
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        assertThrows(SecurityException.class, () -> submissionService.getAnalysis(1L, 99L));
        verify(codeAnalysisService, never()).analyze(any(), any(), any(), anyInt(), anyInt());
    }
}
