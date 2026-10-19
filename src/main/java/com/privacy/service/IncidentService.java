package com.privacy.service;

import com.privacy.dto.IncidentReportRequest;
import com.privacy.dto.IncidentReportResponse;
import com.privacy.entity.IncidentReport;
import com.privacy.entity.User;
import com.privacy.exception.ResourceNotFoundException;
import com.privacy.repository.IncidentReportRepository;
import com.privacy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public IncidentReportResponse createReport(String username, IncidentReportRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        IncidentReport report = IncidentReport.builder()
                .user(user)
                .incidentType(request.getIncidentType())
                .title(request.getTitle())
                .description(request.getDescription())
                .platform(request.getPlatform())
                .status(IncidentReport.ReportStatus.OPEN)
                .build();
        return IncidentReportResponse.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<IncidentReportResponse> getUserReports(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return reportRepository.findByUserIdOrderByReportedAtDesc(user.getId()).stream()
                .map(IncidentReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentReportResponse getUserReport(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return reportRepository.findByIdAndUserId(id, user.getId())
                .map(IncidentReportResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    // ─── Admin Methods ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IncidentReportResponse> getAllReports() {
        return reportRepository.findAllByOrderByReportedAtDesc().stream()
                .map(IncidentReportResponse::from)
                .toList();
    }

    @Transactional
    public IncidentReportResponse updateStatus(Long id, IncidentReport.ReportStatus status, String adminNotes) {
        IncidentReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setStatus(status);
        if (adminNotes != null && !adminNotes.isBlank()) {
            report.setAdminNotes(adminNotes);
        }
        return IncidentReportResponse.from(reportRepository.save(report));
    }
}
