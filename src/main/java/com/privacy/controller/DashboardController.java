package com.privacy.controller;

import com.privacy.dto.CategoryBreakdownResponse;
import com.privacy.dto.ScoreTrendResponse;
import com.privacy.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getDashboardSummary(authentication.getName()));
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<List<CategoryBreakdownResponse>> getCategoryBreakdown(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getCategoryBreakdown(authentication.getName()));
    }

    @GetMapping("/score-trend")
    public ResponseEntity<List<ScoreTrendResponse>> getScoreTrend(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getScoreTrend(authentication.getName()));
    }
}