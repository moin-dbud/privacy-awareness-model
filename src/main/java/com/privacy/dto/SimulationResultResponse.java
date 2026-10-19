package com.privacy.dto;

import com.privacy.entity.SimulationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResultResponse {
    private Long id;
    private Long scenarioId;
    private String scenarioTitle;
    private String channel;
    private int score;
    private int totalPossibleFlags;
    private int flagsIdentified;
    private int falsePositives;
    private boolean passed;
    private String feedback;
    private String generalExplanation;
    private LocalDateTime takenAt;
    private List<FlagDetailDto> flagDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlagDetailDto {
        private Long id;
        private String flagText;
        private String description;
        private boolean isActualRedFlag;
        private boolean selectedByUser;
    }

    public static SimulationResultResponse from(SimulationResult r, String generalExplanation, List<FlagDetailDto> details) {
        return SimulationResultResponse.builder()
                .id(r.getId())
                .scenarioId(r.getScenario().getId())
                .scenarioTitle(r.getScenario().getTitle())
                .channel(r.getScenario().getChannel().name())
                .score(r.getScore())
                .totalPossibleFlags(r.getTotalPossibleFlags())
                .flagsIdentified(r.getFlagsIdentified())
                .falsePositives(r.getFalsePositives())
                .passed(r.isPassed())
                .feedback(r.getFeedback())
                .generalExplanation(generalExplanation)
                .takenAt(r.getTakenAt())
                .flagDetails(details)
                .build();
    }
}