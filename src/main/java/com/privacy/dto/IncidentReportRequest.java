package com.privacy.dto;

import com.privacy.entity.IncidentReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IncidentReportRequest {
    @NotNull(message = "Incident type is required")
    private IncidentReport.IncidentType incidentType;

    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 200, message = "Title must be 10–200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, message = "Description must be at least 20 characters")
    private String description;

    @Size(max = 200)
    private String platform;
}
