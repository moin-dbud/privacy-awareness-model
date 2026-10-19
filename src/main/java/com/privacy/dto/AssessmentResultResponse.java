package com.privacy.dto;

import com.privacy.entity.AssessmentResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssessmentResultResponse {
    private Long id;
    private Integer totalScore;
    private Integer maxPossibleScore;
    private Double percentageScore;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private String riskLevel;
    private String recommendations;
    private LocalDateTime takenAt;
    private List<AnswerFeedback> answerFeedbacks;

    @Data
    public static class AnswerFeedback {
        private String questionText;
        private String selectedAnswer;
        private String correctAnswer;
        private boolean correct;
        private Integer pointsEarned;
        private String explanation;
    }

    public static AssessmentResultResponse from(AssessmentResult r) {
        AssessmentResultResponse dto = new AssessmentResultResponse();
        dto.setId(r.getId());
        dto.setTotalScore(r.getTotalScore());
        dto.setMaxPossibleScore(r.getMaxPossibleScore());
        dto.setPercentageScore(r.getPercentageScore());
        dto.setCorrectAnswers(r.getCorrectAnswers());
        dto.setTotalQuestions(r.getTotalQuestions());
        dto.setRiskLevel(r.getRiskLevel().name());
        dto.setRecommendations(r.getRecommendations());
        dto.setTakenAt(r.getTakenAt());
        dto.setAnswerFeedbacks(r.getAnswers().stream().map(a -> {
            AnswerFeedback f = new AnswerFeedback();
            f.setQuestionText(a.getQuestion().getQuestionText());
            f.setSelectedAnswer(a.getSelectedOption().getOptionText());
            f.setCorrect(a.isCorrect());
            f.setPointsEarned(a.getPointsEarned());
            f.setExplanation(a.getQuestion().getExplanation());
            // Find the correct option text
            a.getQuestion().getOptions().stream()
                    .filter(o -> o.isCorrect())
                    .findFirst()
                    .ifPresent(o -> f.setCorrectAnswer(o.getOptionText()));
            return f;
        }).toList());
        return dto;
    }
}
