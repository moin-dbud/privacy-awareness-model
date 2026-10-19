package com.privacy.controller;

import com.privacy.dto.IncidentReportRequest;
import com.privacy.dto.IncidentReportResponse;
import com.privacy.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentReportResponse> createReport(
            @Valid @RequestBody IncidentReportRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.createReport(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<IncidentReportResponse>> getMyReports(Authentication authentication) {
        return ResponseEntity.ok(incidentService.getUserReports(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentReportResponse> getReport(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(incidentService.getUserReport(id, authentication.getName()));
    }
}
