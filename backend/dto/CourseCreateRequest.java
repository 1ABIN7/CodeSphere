package com.codesphere.backend.dto;

import com.codesphere.backend.validation.SanitizedHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "Course title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @NotBlank(message = "Course description is required")
    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    @SanitizedHtml(message = "Unsafe HTML script or iframe tag found in your content description.")
    private String description;
}