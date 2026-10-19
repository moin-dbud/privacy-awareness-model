package com.privacy.service;

import com.privacy.dto.LearningModuleResponse;
import com.privacy.entity.*;
import com.privacy.exception.ResourceNotFoundException;
import com.privacy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningService {

    private final LearningModuleRepository moduleRepository;
    private final ModuleProgressRepository progressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<LearningModuleResponse> getAllModules(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Set<Long> completedIds = progressRepository.findByUserId(user.getId()).stream()
                .filter(ModuleProgress::isCompleted)
                .map(mp -> mp.getModule().getId())
                .collect(Collectors.toSet());

        return moduleRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(m -> {
                    LearningModuleResponse r = LearningModuleResponse.from(m);
                    r.setCompleted(completedIds.contains(m.getId()));
                    return r;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningModuleResponse getModule(Long moduleId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        LearningModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
        LearningModuleResponse r = LearningModuleResponse.from(module);
        progressRepository.findByUserIdAndModuleId(user.getId(), moduleId)
                .ifPresent(mp -> r.setCompleted(mp.isCompleted()));
        return r;
    }

    @Transactional
    public void markComplete(Long moduleId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        LearningModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));

        ModuleProgress progress = progressRepository
                .findByUserIdAndModuleId(user.getId(), moduleId)
                .orElse(ModuleProgress.builder().user(user).module(module).build());

        progress.setCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getProgressSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        long total = moduleRepository.findByActiveTrueOrderByDisplayOrderAsc().size();
        long completed = progressRepository.countCompletedByUserId(user.getId());
        return Map.of("total", total, "completed", completed);
    }
}
