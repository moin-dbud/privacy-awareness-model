package com.privacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreTrendResponse {
    private Long assessmentId;
    private LocalDateTime takenAt;
    private int score;
    private int maxScore;
    private double percentage;
    private String riskLevel;
}