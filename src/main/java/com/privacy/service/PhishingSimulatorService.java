package com.privacy.service;

import com.privacy.dto.PhishingScenarioResponse;
import com.privacy.dto.PhishingSubmitRequest;
import com.privacy.dto.SimulationResultResponse;
import com.privacy.entity.PhishingRedFlag;
import com.privacy.entity.PhishingScenario;
import com.privacy.entity.SimulationResult;
import com.privacy.entity.User;
import com.privacy.exception.BadRequestException;
import com.privacy.exception.ResourceNotFoundException;
import com.privacy.repository.PhishingRedFlagRepository;
import com.privacy.repository.PhishingScenarioRepository;
import com.privacy.repository.SimulationResultRepository;
import com.privacy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PhishingSimulatorService {

    private final PhishingScenarioRepository scenarioRepository;
    private final PhishingRedFlagRepository redFlagRepository;
    private final SimulationResultRepository resultRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PhishingScenarioResponse> getAllScenarios() {
        return scenarioRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(PhishingScenarioResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PhishingScenarioResponse getScenarioById(Long id) {
        PhishingScenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phishing scenario not found"));
        return PhishingScenarioResponse.from(scenario);
    }

    @Transactional
    public SimulationResultResponse submitSimulation(String username, PhishingSubmitRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PhishingScenario scenario = scenarioRepository.findById(request.getScenarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Phishing scenario not found"));

        List<PhishingRedFlag> allFlags = scenario.getRedFlags();
        if (allFlags.isEmpty()) {
            throw new BadRequestException("Scenario has no evaluation indicators configured");
        }

        Set<Long> selectedIds = new HashSet<>(request.getSelectedFlagIds() != null ? request.getSelectedFlagIds() : Collections.emptyList());

        int totalActualFlags = 0;
        int truePositives = 0;
        int falsePositives = 0;

        List<SimulationResultResponse.FlagDetailDto> details = new ArrayList<>();

        for (PhishingRedFlag flag : allFlags) {
            boolean isActual = flag.isCorrect();
            boolean selected = selectedIds.contains(flag.getId());

            if (isActual) {
                totalActualFlags++;
                if (selected) {
                    truePositives++;
                }
            } else {
                if (selected) {
                    falsePositives++;
                }
            }

            details.add(SimulationResultResponse.FlagDetailDto.builder()
                    .id(flag.getId())
                    .flagText(flag.getFlagText())
                    .description(flag.getDescription())
                    .isActualRedFlag(isActual)
                    .selectedByUser(selected)
                    .build());
        }

        // Calculate score percentage (deduct for false alarms, reward for spotted indicators)
        double rawScore = totalActualFlags > 0 ?
                (((double) truePositives - (falsePositives * 0.4)) / totalActualFlags) * 100.0 : 0.0;
        int finalScore = Math.max(0, Math.min(100, (int) Math.round(rawScore)));
        boolean passed = finalScore >= 70;

        String feedback = buildFeedback(finalScore, truePositives, totalActualFlags, falsePositives);

        SimulationResult result = SimulationResult.builder()
                .user(user)
                .scenario(scenario)
                .score(finalScore)
                .totalPossibleFlags(totalActualFlags)
                .flagsIdentified(truePositives)
                .falsePositives(falsePositives)
                .passed(passed)
                .feedback(feedback)
                .build();

        SimulationResult saved = resultRepository.save(result);
        return SimulationResultResponse.from(saved, scenario.getExplanation(), details);
    }

    @Transactional(readOnly = true)
    public List<SimulationResultResponse> getUserHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return resultRepository.findByUserIdOrderByTakenAtDesc(user.getId()).stream()
                .map(r -> SimulationResultResponse.from(r, r.getScenario().getExplanation(), Collections.emptyList()))
                .toList();
    }

    private String buildFeedback(int score, int spotted, int total, int falseAlarms) {
        StringBuilder sb = new StringBuilder();
        if (score >= 90) {
            sb.append("Outstanding threat detection! You identified ").append(spotted).append(" of ").append(total).append(" red flags with keen precision.");
        } else if (score >= 70) {
            sb.append("Well done! You spotted key warning signs (").append(spotted).append("/").append(total).append(").");
        } else if (score >= 40) {
            sb.append("Caution: You missed critical phishing indicators (spotted ").append(spotted).append(" of ").append(total).append("). Review the explanations below.");
        } else {
            sb.append("Vulnerable to attack: You missed most indicators in this simulation. Attackers rely on these techniques to compromise credentials.");
        }
        if (falseAlarms > 0) {
            sb.append(" Note: You flagged ").append(falseAlarms).append(" item(s) that were actually benign.");
        }
        return sb.toString();
    }
}