package com.privacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsResponse {
    private double averageScore;
    private long totalAssessments;
    private List<MissedQuestionDto> mostMissedQuestions;
    private Map<String, Long> incidentsByCategory;
    private Map<String, Long> incidentsByStatus;
    private List<UserGrowthDto> newUsersPerWeek;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissedQuestionDto {
        private Long questionId;
        private String questionText;
        private String category;
        private int totalAttempts;
        private int missedCount;
        private double missedPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGrowthDto {
        private String weekLabel;
        private long userCount;
    }
}