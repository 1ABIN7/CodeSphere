package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.common.PageResponse;
import com.CodeSphere.backend.dto.problem.ProblemRequest;
import com.CodeSphere.backend.dto.problem.ProblemResponse;
import com.CodeSphere.backend.model.Difficulty;
import com.CodeSphere.backend.model.Problem;
import com.CodeSphere.backend.repository.ProblemRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.SubmissionRepository;
import com.CodeSphere.backend.repository.TestCaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock private ProblemRepository problemRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private QuestionBankRepository questionBankRepository;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private ProblemService problemService;

    private ProblemRequest request;
    private Problem savedProblem;

    @BeforeEach
    void setUp() {
        request = new ProblemRequest();
        request.setTitle("Two Sum");
        request.setDescription("Given an array of integers, return indices of the two numbers that add up to target.");
        request.setDifficulty("EASY");

        savedProblem = Problem.builder()
                .id(1L)
                .title("Two Sum")
                .description(request.getDescription())
                .difficulty(Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(262144)
                .isPublished(false)
                .build();
    }

    @Test
    void createProblem_Success_WhenTitleIsUnique() {
        when(questionBankRepository.existsByCodingProblemId(anyLong())).thenReturn(false);
        when(problemRepository.existsByTitle("Two Sum")).thenReturn(false);
        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(testCaseRepository.countByProblemId(1L)).thenReturn(0L);

        ProblemResponse response = problemService.createProblem(request, 42L);

        assertEquals("Two Sum", response.getTitle());
        assertEquals("EASY", response.getDifficulty());
        assertFalse(response.getIsPublished());
        verify(questionBankRepository).existsByCodingProblemId(1L);
        verify(questionBankRepository).save(any());

        ArgumentCaptor<Problem> captor = ArgumentCaptor.forClass(Problem.class);
        verify(problemRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getCreatedBy());
        assertFalse(captor.getValue().getIsPublished());
    }

    @Test
    void createProblem_ThrowsException_WhenTitleAlreadyExists() {
        when(problemRepository.existsByTitle("Two Sum")).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> problemService.createProblem(request, 42L));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    void updateProblem_Success_WhenProblemExists() {
        request.setTitle("Two Sum (Updated)");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));
        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(testCaseRepository.countByProblemId(1L)).thenReturn(0L);

        assertDoesNotThrow(() -> problemService.updateProblem(1L, request));

        ArgumentCaptor<Problem> captor = ArgumentCaptor.forClass(Problem.class);
        verify(problemRepository).save(captor.capture());
        assertEquals("Two Sum (Updated)", captor.getValue().getTitle());
    }

    @Test
    void updateProblem_ThrowsException_WhenProblemNotFound() {
        when(problemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> problemService.updateProblem(99L, request));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    void deleteProblem_Success_WhenProblemExists() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));

        assertDoesNotThrow(() -> problemService.deleteProblem(1L));

        verify(problemRepository).delete(savedProblem);
    }

    @Test
    void deleteProblem_ThrowsException_WhenProblemNotFound() {
        when(problemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> problemService.deleteProblem(99L));
        verify(problemRepository, never()).delete(any(Problem.class));
    }

    @Test
    void togglePublish_ThrowsException_WhenNoTestCases() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));
        when(testCaseRepository.countByProblemId(1L)).thenReturn(0L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> problemService.togglePublish(1L, true));

        assertTrue(exception.getMessage().contains("no test cases"));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    void togglePublish_Success_WhenAtLeastOneTestCaseExists() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));
        when(testCaseRepository.countByProblemId(1L)).thenReturn(1L);
        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> problemService.togglePublish(1L, true));

        ArgumentCaptor<Problem> captor = ArgumentCaptor.forClass(Problem.class);
        verify(problemRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsPublished());
    }

    @Test
    void togglePublish_AlwaysAllowsUnpublishing_RegardlessOfTestCaseCount() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));
        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> problemService.togglePublish(1L, false));
    }

    @Test
    void listProblems_ReturnsPagedPublishedProblems_WhenNoFiltersApplied() {
        Page<Problem> page = new PageImpl<>(List.of(savedProblem));
        when(problemRepository.findByIsPublishedTrue(any(Pageable.class))).thenReturn(page);
        when(submissionRepository.existsByProblemIdAndUserIdAndStatus(any(), any(), any())).thenReturn(false);

        PageResponse<?> result = problemService.listProblems(null, null, null, 0, 20, 5L);

        assertEquals(1, result.getContent().size());
        verify(problemRepository).findByIsPublishedTrue(any(Pageable.class));
        verify(problemRepository, never()).searchPublished(any(), any());
    }
}
