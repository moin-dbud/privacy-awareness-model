package com.privacy.dto;

import com.privacy.entity.QuizQuestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {

    @NotBlank(message = "Question text is required")
    @Size(min = 10, max = 500, message = "Question text must be between 10 and 500 characters")
    private String questionText;

    @NotNull(message = "Category is required")
    private QuizQuestion.Category category;

    @NotBlank(message = "Explanation is required")
    private String explanation;

    @Min(value = 1, message = "Points must be at least 1")
    @Builder.Default
    private int points = 10;

    @Builder.Default
    private boolean active = true;

    @NotNull(message = "Options are required")
    @Size(min = 2, max = 6, message = "Question must have between 2 and 6 options")
    @Valid
    @Builder.Default
    private List<OptionDto> options = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private Long id;

        @NotBlank(message = "Option text is required")
        private String optionText;

        private boolean correct;

        private int displayOrder;
    }
}
