package com.privacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBreakdownResponse {
    private String category;
    private String categoryName;
    private int totalQuestions;
    private int correctCount;
    private double accuracyPercentage;
    private double riskScore;
    private String riskLevel;
}