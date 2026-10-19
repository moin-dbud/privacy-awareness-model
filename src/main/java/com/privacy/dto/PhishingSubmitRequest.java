package com.privacy.dto;

import jakarta.validation.constraints.NotNull;
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
public class PhishingSubmitRequest {
    @NotNull(message = "Scenario ID is required")
    private Long scenarioId;

    @Builder.Default
    private List<Long> selectedFlagIds = new ArrayList<>();
}