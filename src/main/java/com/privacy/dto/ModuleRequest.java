package com.privacy.dto;

import com.privacy.entity.QuizQuestion;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @NotNull(message = "Category is required")
    private QuizQuestion.Category category;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    private String description;

    @NotBlank(message = "Content is required")
    private String content;

    @Min(value = 1, message = "Estimated minutes must be at least 1")
    @Builder.Default
    private int estimatedMinutes = 5;

    @Min(value = 1, message = "Display order must be at least 1")
    @Builder.Default
    private int displayOrder = 1;

    @Builder.Default
    private boolean active = true;
}
