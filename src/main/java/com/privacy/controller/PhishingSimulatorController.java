package com.privacy.controller;

import com.privacy.dto.PhishingScenarioResponse;
import com.privacy.dto.PhishingSubmitRequest;
import com.privacy.dto.SimulationResultResponse;
import com.privacy.service.PhishingSimulatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
public class PhishingSimulatorController {

    private final PhishingSimulatorService simulatorService;

    @GetMapping("/scenarios")
    public ResponseEntity<List<PhishingScenarioResponse>> getScenarios() {
        return ResponseEntity.ok(simulatorService.getAllScenarios());
    }

    @GetMapping("/scenarios/{id}")
    public ResponseEntity<PhishingScenarioResponse> getScenario(@PathVariable Long id) {
        return ResponseEntity.ok(simulatorService.getScenarioById(id));
    }

    @PostMapping("/submit")
    public ResponseEntity<SimulationResultResponse> submitSimulation(
            @Valid @RequestBody PhishingSubmitRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(simulatorService.submitSimulation(authentication.getName(), request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SimulationResultResponse>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(simulatorService.getUserHistory(authentication.getName()));
    }
}