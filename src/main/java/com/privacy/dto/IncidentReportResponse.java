package com.privacy.dto;

import com.privacy.entity.IncidentReport;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IncidentReportResponse {
    private Long id;
    private String incidentType;
    private String title;
    private String description;
    private String platform;
    private String status;
    private String adminNotes;
    private LocalDateTime reportedAt;
    private LocalDateTime updatedAt;
    private String reportedBy;

    public static IncidentReportResponse from(IncidentReport r) {
        IncidentReportResponse dto = new IncidentReportResponse();
        dto.setId(r.getId());
        dto.setIncidentType(r.getIncidentType().name());
        dto.setTitle(r.getTitle());
        dto.setDescription(r.getDescription());
        dto.setPlatform(r.getPlatform());
        dto.setStatus(r.getStatus().name());
        dto.setAdminNotes(r.getAdminNotes());
        dto.setReportedAt(r.getReportedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        dto.setReportedBy(r.getUser().getUsername());
        return dto;
    }
}
