package com.privacy.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {
    @NotEmpty(message = "Answers cannot be empty")
    private List<AnswerDto> answers;

    @Data
    public static class AnswerDto {
        @NotNull(message = "Question ID is required")
        private Long questionId;

        @NotNull(message = "Selected option ID is required")
        private Long selectedOptionId;
    }
}
