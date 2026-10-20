package com.privacy.controller;

import com.privacy.dto.*;
import com.privacy.entity.IncidentReport;
import com.privacy.entity.QuizQuestion;
import com.privacy.repository.LearningModuleRepository;
import com.privacy.repository.QuizQuestionRepository;
import com.privacy.repository.UserRepository;
import com.privacy.service.DashboardService;
import com.privacy.service.IncidentService;
import com.privacy.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final IncidentService incidentService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final QuizQuestionRepository questionRepository;
    private final LearningModuleRepository moduleRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAdminSummary() {
        return ResponseEntity.ok(dashboardService.getAdminSummary());
    }

    @GetMapping("/analytics")
    public ResponseEntity<AdminAnalyticsResponse> getAdminAnalytics() {
        return ResponseEntity.ok(dashboardService.getAdminAnalytics());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(userService.getAllUsersFiltered(search, role));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUserByAdmin(request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        String adminUsername = authentication != null ? authentication.getName() : "admin";
        userService.deleteUserByAdmin(id, adminUsername);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully", "id", id.toString()));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {
        String adminUsername = authentication != null ? authentication.getName() : "admin";
        return ResponseEntity.ok(userService.updateUserRole(id, request.getRole(), adminUsername));
    }

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @PathVariable Long id,
            Authentication authentication) {
        String adminUsername = authentication != null ? authentication.getName() : "admin";
        return ResponseEntity.ok(userService.toggleUserStatus(id, adminUsername));
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentReportResponse>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllReports());
    }

    @PatchMapping("/incidents/{id}/status")
    public ResponseEntity<IncidentReportResponse> updateIncidentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        String notes = body.get("adminNotes");
        IncidentReport.ReportStatus status = IncidentReport.ReportStatus.valueOf(statusStr);
        return ResponseEntity.ok(incidentService.updateStatus(id, status, notes));
    }

    @GetMapping("/questions")
    public ResponseEntity<?> getAllQuestions() {
        return ResponseEntity.ok(questionRepository.findAll().stream()
                .map(q -> Map.of(
                        "id", q.getId(),
                        "questionText", q.getQuestionText(),
                        "category", q.getCategory().name(),
                        "points", q.getPoints(),
                        "active", q.isActive(),
                        "optionCount", q.getOptions().size()
                ))
                .toList());
    }

    @PatchMapping("/questions/{id}/toggle")
    public ResponseEntity<?> toggleQuestion(@PathVariable Long id) {
        return questionRepository.findById(id).map(q -> {
            q.setActive(!q.isActive());
            questionRepository.save(q);
            return ResponseEntity.ok(Map.of("id", q.getId(), "active", q.isActive()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/modules")
    public ResponseEntity<?> getAllModules() {
        return ResponseEntity.ok(moduleRepository.findAll().stream()
                .sorted(java.util.Comparator.comparingInt(com.privacy.entity.LearningModule::getDisplayOrder))
                .map(m -> Map.of(
                        "id", m.getId(),
                        "title", m.getTitle(),
                        "category", m.getCategory().name(),
                        "estimatedMinutes", m.getEstimatedMinutes(),
                        "displayOrder", m.getDisplayOrder(),
                        "active", m.isActive(),
                        "description", m.getDescription() != null ? m.getDescription() : ""
                ))
                .toList());
    }

    @PatchMapping("/modules/{id}/toggle")
    public ResponseEntity<?> toggleModule(@PathVariable Long id) {
        return moduleRepository.findById(id).map(m -> {
            m.setActive(!m.isActive());
            moduleRepository.save(m);
            return ResponseEntity.ok(Map.of("id", m.getId(), "active", m.isActive()));
        }).orElse(ResponseEntity.notFound().build());
    }
}
