package com.privacy.controller;

import com.privacy.dto.LearningModuleResponse;
import com.privacy.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    @GetMapping("/modules")
    public ResponseEntity<List<LearningModuleResponse>> getAllModules(Authentication authentication) {
        return ResponseEntity.ok(learningService.getAllModules(authentication.getName()));
    }

    @GetMapping("/modules/{id}")
    public ResponseEntity<LearningModuleResponse> getModule(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(learningService.getModule(id, authentication.getName()));
    }

    @PostMapping("/modules/{id}/complete")
    public ResponseEntity<Map<String, String>> markComplete(
            @PathVariable Long id,
            Authentication authentication) {
        learningService.markComplete(id, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Module marked as complete"));
    }

    @GetMapping("/progress")
    public ResponseEntity<Map<String, Long>> getProgress(Authentication authentication) {
        return ResponseEntity.ok(learningService.getProgressSummary(authentication.getName()));
    }
}
