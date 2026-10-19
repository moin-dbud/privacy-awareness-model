package com.privacy.dto;

import com.privacy.entity.PhishingScenario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhishingScenarioResponse {
    private Long id;
    private String title;
    private String channel;
    private String sender;
    private String subject;
    private String content;
    private String difficulty;
    private List<RedFlagOptionDto> redFlagOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedFlagOptionDto {
        private Long id;
        private String flagText;
        private Integer displayOrder;
        // isCorrect is intentionally omitted here during the challenge
    }

    public static PhishingScenarioResponse from(PhishingScenario s) {
        return PhishingScenarioResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .channel(s.getChannel().name())
                .sender(s.getSender())
                .subject(s.getSubject())
                .content(s.getContent())
                .difficulty(s.getDifficulty().name())
                .redFlagOptions(s.getRedFlags().stream().map(f -> RedFlagOptionDto.builder()
                        .id(f.getId())
                        .flagText(f.getFlagText())
                        .displayOrder(f.getDisplayOrder())
                        .build()
                ).toList())
                .build();
    }
}