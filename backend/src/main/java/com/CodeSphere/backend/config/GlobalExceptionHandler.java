package com.CodeSphere.backend.config;

import com.CodeSphere.backend.dto.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Global exception handler that converts exceptions into standardized
 * {@link ApiErrorResponse} objects with proper HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- Validation Errors (400) ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ApiErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .rejectedValue(fe.getRejectedValue())
                        .build())
                .toList();

        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .status(400)
                .error("Validation Failed")
                .message("One or more fields have validation errors")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(
            BindException ex, HttpServletRequest request) {

        List<ApiErrorResponse.FieldError> fieldErrors = ex.getFieldErrors()
                .stream()
                .map(fe -> ApiErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .rejectedValue(fe.getRejectedValue())
                        .build())
                .toList();

        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .status(400)
                .error("Binding Failed")
                .message("Request parameter binding errors")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build());
    }

    // ---- Not Found (404) ----

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoSuchElementException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.builder()
                .status(404)
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    // ---- Business Logic Errors (400) ----

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            IllegalStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrorResponse.builder()
                .status(409)
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    // ---- File Upload Size Exceeded (413) ----

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiErrorResponse.builder()
                .status(413)
                .error("Payload Too Large")
                .message("File size exceeds the maximum allowed upload size")
                .path(request.getRequestURI())
                .build());
    }

    // ---- Security / Access Denied (403) ----

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            SecurityException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiErrorResponse.builder()
                .status(403)
                .error("Forbidden")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    // ---- Catch-All (500) ----

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntime(
            RuntimeException ex, HttpServletRequest request) {
        ex.printStackTrace(); // ADDED FOR DEBUGGING
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder()
                .status(500)
                .error("Internal Server Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }
}
