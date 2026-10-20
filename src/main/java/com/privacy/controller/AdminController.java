package com.privacy.controller;

import com.privacy.dto.*;
import com.privacy.entity.QuizOption;
import com.privacy.exception.BadRequestException;
import com.privacy.exception.ResourceNotFoundException;
import com.privacy.repository.AssessmentAnswerRepository;
import com.privacy.repository.ModuleProgressRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
import java.util.HashMap;

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
    private final AssessmentAnswerRepository assessmentAnswerRepository;
    private final ModuleProgressRepository progressRepository;

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

    @GetMapping("/incidents/export/csv")
    public ResponseEntity<byte[]> exportIncidentsCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        byte[] csv = incidentService.exportCsv(status, category);
        String filename = "incident-reports-" + java.time.LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }

    @GetMapping("/incidents/export/pdf")
    public ResponseEntity<byte[]> exportIncidentsPdf(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        byte[] pdf = incidentService.exportPdf(status, category);
        String filename = "incident-reports-" + java.time.LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .body(pdf);
    }

    // ─── Quiz Question Management (Task 4) ───────────────────────────────────

    @GetMapping("/questions")
    public ResponseEntity<?> getAllQuestions() {
        return ResponseEntity.ok(questionRepository.findAll().stream()
                .map(q -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", q.getId());
                    m.put("questionText", q.getQuestionText());
                    m.put("category", q.getCategory().name());
                    m.put("points", q.getPoints());
                    m.put("active", q.isActive());
                    m.put("optionCount", q.getOptions().size());
                    m.put("explanation", q.getExplanation());
                    return m;
                })
                .toList());
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<?> getQuestionById(@PathVariable Long id) {
        return questionRepository.findById(id).map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("questionText", q.getQuestionText());
            map.put("category", q.getCategory().name());
            map.put("explanation", q.getExplanation());
            map.put("points", q.getPoints());
            map.put("active", q.isActive());
            map.put("options", q.getOptions().stream()
                    .sorted(java.util.Comparator.comparingInt(QuizOption::getDisplayOrder))
                    .map(opt -> {
                        Map<String, Object> o = new HashMap<>();
                        o.put("id", opt.getId());
                        o.put("optionText", opt.getOptionText());
                        o.put("correct", opt.isCorrect());
                        o.put("displayOrder", opt.getDisplayOrder());
                        return o;
                    }).toList());
            return ResponseEntity.ok((Object) map);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/questions")
    public ResponseEntity<?> createQuestion(@Valid @RequestBody QuestionRequest req) {
        validateQuestionRequest(req);
        QuizQuestion q = QuizQuestion.builder()
                .questionText(req.getQuestionText())
                .category(req.getCategory())
                .explanation(req.getExplanation())
                .points(req.getPoints())
                .active(req.isActive())
                .build();

        for (int i = 0; i < req.getOptions().size(); i++) {
            QuestionRequest.OptionDto optDto = req.getOptions().get(i);
            QuizOption opt = QuizOption.builder()
                    .question(q)
                    .optionText(optDto.getOptionText())
                    .correct(optDto.isCorrect())
                    .displayOrder(optDto.getDisplayOrder() > 0 ? optDto.getDisplayOrder() : i + 1)
                    .build();
            q.getOptions().add(opt);
        }

        QuizQuestion saved = questionRepository.save(q);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "message", "Question created successfully"));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest req) {
        validateQuestionRequest(req);
        QuizQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id " + id));

        q.setQuestionText(req.getQuestionText());
        q.setCategory(req.getCategory());
        q.setExplanation(req.getExplanation());
        q.setPoints(req.getPoints());
        q.setActive(req.isActive());

        q.getOptions().clear();
        for (int i = 0; i < req.getOptions().size(); i++) {
            QuestionRequest.OptionDto optDto = req.getOptions().get(i);
            QuizOption opt = QuizOption.builder()
                    .question(q)
                    .optionText(optDto.getOptionText())
                    .correct(optDto.isCorrect())
                    .displayOrder(optDto.getDisplayOrder() > 0 ? optDto.getDisplayOrder() : i + 1)
                    .build();
            q.getOptions().add(opt);
        }

        questionRepository.save(q);
        return ResponseEntity.ok(Map.of("id", q.getId(), "message", "Question updated successfully"));
    }

    @PatchMapping("/questions/{id}/toggle")
    public ResponseEntity<?> toggleQuestion(@PathVariable Long id) {
        return questionRepository.findById(id).map(q -> {
            q.setActive(!q.isActive());
            questionRepository.save(q);
            return ResponseEntity.ok(Map.of("id", q.getId(), "active", q.isActive()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        QuizQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id " + id));

        if (assessmentAnswerRepository.existsByQuestionId(id)) {
            throw new BadRequestException("Cannot hard-delete question #" + id + 
                    " because it is referenced in past user assessment answers. Deactivate it instead to preserve audit history.");
        }

        questionRepository.delete(q);
        return ResponseEntity.ok(Map.of("message", "Question deleted successfully"));
    }

    private void validateQuestionRequest(QuestionRequest req) {
        if (req.getOptions() == null || req.getOptions().size() < 2) {
            throw new BadRequestException("A quiz question must provide at least two answer options.");
        }
        long correctCount = req.getOptions().stream().filter(QuestionRequest.OptionDto::isCorrect).count();
        if (correctCount != 1) {
            throw new BadRequestException("A quiz question must have exactly one correct answer option designated (found " + correctCount + ").");
        }
    }

    // ─── Learning Module Management (Task 5) ──────────────────────────────────

    @GetMapping("/modules")
    public ResponseEntity<?> getAllModules() {
        return ResponseEntity.ok(moduleRepository.findAll().stream()
                .sorted(java.util.Comparator.comparingInt(com.privacy.entity.LearningModule::getDisplayOrder))
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("title", m.getTitle());
                    map.put("category", m.getCategory().name());
                    map.put("estimatedMinutes", m.getEstimatedMinutes());
                    map.put("displayOrder", m.getDisplayOrder());
                    map.put("active", m.isActive());
                    map.put("description", m.getDescription() != null ? m.getDescription() : "");
                    return map;
                })
                .toList());
    }

    @GetMapping("/modules/{id}")
    public ResponseEntity<?> getModuleById(@PathVariable Long id) {
        return moduleRepository.findById(id).map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("category", m.getCategory().name());
            map.put("description", m.getDescription() != null ? m.getDescription() : "");
            map.put("content", m.getContent() != null ? m.getContent() : "");
            map.put("estimatedMinutes", m.getEstimatedMinutes());
            map.put("displayOrder", m.getDisplayOrder());
            map.put("active", m.isActive());
            return ResponseEntity.ok((Object) map);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/modules")
    public ResponseEntity<?> createModule(@Valid @RequestBody ModuleRequest req) {
        com.privacy.entity.LearningModule m = com.privacy.entity.LearningModule.builder()
                .title(req.getTitle())
                .category(req.getCategory())
                .description(req.getDescription())
                .content(req.getContent())
                .estimatedMinutes(req.getEstimatedMinutes())
                .displayOrder(req.getDisplayOrder())
                .active(req.isActive())
                .build();

        com.privacy.entity.LearningModule saved = moduleRepository.save(m);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(), "message", "Learning module created successfully"));
    }

    @PutMapping("/modules/{id}")
    public ResponseEntity<?> updateModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest req) {
        com.privacy.entity.LearningModule m = moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning module not found with id " + id));

        m.setTitle(req.getTitle());
        m.setCategory(req.getCategory());
        m.setDescription(req.getDescription());
        m.setContent(req.getContent());
        m.setEstimatedMinutes(req.getEstimatedMinutes());
        m.setDisplayOrder(req.getDisplayOrder());
        m.setActive(req.isActive());

        moduleRepository.save(m);
        return ResponseEntity.ok(Map.of("id", m.getId(), "message", "Learning module updated successfully"));
    }

    @PatchMapping("/modules/{id}/toggle")
    public ResponseEntity<?> toggleModule(@PathVariable Long id) {
        return moduleRepository.findById(id).map(m -> {
            m.setActive(!m.isActive());
            moduleRepository.save(m);
            return ResponseEntity.ok(Map.of("id", m.getId(), "active", m.isActive()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/modules/{id}")
    public ResponseEntity<?> deleteModule(@PathVariable Long id) {
        com.privacy.entity.LearningModule m = moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning module not found with id " + id));

        if (progressRepository.existsByModuleId(id)) {
            throw new BadRequestException("Cannot hard-delete module #" + id + 
                    " ('" + m.getTitle() + "') because users have progress history on it. Deactivate it instead to preserve user records.");
        }

        moduleRepository.delete(m);
        return ResponseEntity.ok(Map.of("message", "Learning module deleted successfully"));
    }
}
