package com.privacy.dto;

import com.privacy.entity.QuizOption;
import com.privacy.entity.QuizQuestion;
import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionResponse {
    private Long id;
    private String questionText;
    private String category;
    private Integer points;
    private List<OptionDto> options;

    // Explanation is NOT included in quiz-time responses to avoid leaking answers
    // It is returned in the result response after submission

    @Data
    public static class OptionDto {
        private Long id;
        private String optionText;
        private Integer displayOrder;
        // 'correct' is intentionally omitted here
    }

    public static QuizQuestionResponse from(QuizQuestion q) {
        QuizQuestionResponse r = new QuizQuestionResponse();
        r.setId(q.getId());
        r.setQuestionText(q.getQuestionText());
        r.setCategory(q.getCategory().name());
        r.setPoints(q.getPoints());
        r.setOptions(q.getOptions().stream().map(o -> {
            OptionDto d = new OptionDto();
            d.setId(o.getId());
            d.setOptionText(o.getOptionText());
            d.setDisplayOrder(o.getDisplayOrder());
            return d;
        }).toList());
        return r;
    }
}
