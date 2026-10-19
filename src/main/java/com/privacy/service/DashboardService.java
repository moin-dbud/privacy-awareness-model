package com.privacy.service;

import com.privacy.dto.AdminAnalyticsResponse;
import com.privacy.dto.CategoryBreakdownResponse;
import com.privacy.dto.ScoreTrendResponse;
import com.privacy.entity.AssessmentAnswer;
import com.privacy.entity.AssessmentResult;
import com.privacy.entity.QuizQuestion;
import com.privacy.entity.User;
import com.privacy.exception.ResourceNotFoundException;
import com.privacy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final AssessmentResultRepository resultRepository;
    private final ModuleProgressRepository progressRepository;
    private final IncidentReportRepository incidentReportRepository;
    private final LearningModuleRepository moduleRepository;
    private final QuizQuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Object> summary = new HashMap<>();
        summary.put("username", user.getUsername());
        summary.put("fullName", user.getFullName());
        summary.put("role", user.getRole().name());

        // Assessment stats
        long totalAssessments = resultRepository.countByUserId(user.getId());
        summary.put("totalAssessments", totalAssessments);

        resultRepository.findTopByUserIdOrderByTakenAtDesc(user.getId()).ifPresent(r -> {
            summary.put("latestScore", r.getTotalScore());
            summary.put("latestMaxScore", r.getMaxPossibleScore());
            summary.put("latestPercentage", r.getPercentageScore());
            summary.put("latestRiskLevel", r.getRiskLevel().name());
            summary.put("latestTakenAt", r.getTakenAt());
        });

        // Learning progress
        long totalModules = moduleRepository.findByActiveTrueOrderByDisplayOrderAsc().size();
        long completedModules = progressRepository.countCompletedByUserId(user.getId());
        summary.put("totalModules", totalModules);
        summary.put("completedModules", completedModules);
        summary.put("learningProgress", totalModules > 0 ? (int)((double) completedModules / totalModules * 100) : 0);

        // Incident reports
        long totalReports = incidentReportRepository.findByUserIdOrderByReportedAtDesc(user.getId()).size();
        summary.put("totalIncidentReports", totalReports);

        return summary;
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownResponse> getCategoryBreakdown(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<AssessmentResult> results = resultRepository.findByUserIdOrderByTakenAtDesc(user.getId());

        Map<QuizQuestion.Category, int[]> categoryStats = new EnumMap<>(QuizQuestion.Category.class);
        for (QuizQuestion.Category cat : QuizQuestion.Category.values()) {
            categoryStats.put(cat, new int[]{0, 0});
        }

        for (AssessmentResult res : results) {
            if (res.getAnswers() != null) {
                for (AssessmentAnswer ans : res.getAnswers()) {
                    if (ans.getQuestion() != null && ans.getQuestion().getCategory() != null) {
                        QuizQuestion.Category cat = ans.getQuestion().getCategory();
                        int[] stats = categoryStats.get(cat);
                        if (stats != null) {
                            stats[0]++;
                            if (ans.isCorrect()) {
                                stats[1]++;
                            }
                        }
                    }
                }
            }
        }

        List<CategoryBreakdownResponse> response = new ArrayList<>();
        for (QuizQuestion.Category cat : QuizQuestion.Category.values()) {
            int[] stats = categoryStats.get(cat);
            int total = stats[0];
            int correct = stats[1];
            double accuracy = total > 0 ? ((double) correct / total) * 100.0 : 0.0;
            double riskScore = total > 0 ? (100.0 - accuracy) : 0.0;
            String riskLevel;
            if (total == 0) {
                riskLevel = "UNTESTED";
            } else if (accuracy >= 80.0) {
                riskLevel = "LOW";
            } else if (accuracy >= 60.0) {
                riskLevel = "MEDIUM";
            } else if (accuracy >= 40.0) {
                riskLevel = "HIGH";
            } else {
                riskLevel = "CRITICAL";
            }

            response.add(CategoryBreakdownResponse.builder()
                    .category(cat.name())
                    .categoryName(formatCategoryName(cat))
                    .totalQuestions(total)
                    .correctCount(correct)
                    .accuracyPercentage(Math.round(accuracy * 10.0) / 10.0)
                    .riskScore(Math.round(riskScore * 10.0) / 10.0)
                    .riskLevel(riskLevel)
                    .build());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<ScoreTrendResponse> getScoreTrend(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<AssessmentResult> results = resultRepository.findByUserIdOrderByTakenAtAsc(user.getId());

        return results.stream().map(r -> ScoreTrendResponse.builder()
                .assessmentId(r.getId())
                .takenAt(r.getTakenAt())
                .score(r.getTotalScore())
                .maxScore(r.getMaxPossibleScore())
                .percentage(Math.round(r.getPercentageScore() * 10.0) / 10.0)
                .riskLevel(r.getRiskLevel().name())
                .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUsers", userRepository.count());
        summary.put("totalAssessments", resultRepository.count());
        summary.put("totalIncidentReports", incidentReportRepository.count());
        summary.put("openIncidents", incidentReportRepository
                .findByStatusOrderByReportedAtDesc(com.privacy.entity.IncidentReport.ReportStatus.OPEN).size());
        summary.put("totalLearningModules", moduleRepository.count());
        return summary;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAdminAnalytics() {
        // 1. Average score and total assessments
        List<AssessmentResult> allResults = resultRepository.findAll();
        long totalAssessments = allResults.size();
        double avgScore = 0.0;
        if (totalAssessments > 0) {
            double sum = allResults.stream().mapToDouble(AssessmentResult::getPercentageScore).sum();
            avgScore = Math.round((sum / totalAssessments) * 10.0) / 10.0;
        }

        // 2. Most missed questions
        Map<Long, int[]> questionStats = new HashMap<>(); // [totalAttempts, missedCount]
        for (AssessmentResult res : allResults) {
            if (res.getAnswers() != null) {
                for (AssessmentAnswer ans : res.getAnswers()) {
                    if (ans.getQuestion() != null) {
                        Long qId = ans.getQuestion().getId();
                        int[] stats = questionStats.computeIfAbsent(qId, k -> new int[]{0, 0});
                        stats[0]++;
                        if (!ans.isCorrect()) {
                            stats[1]++;
                        }
                    }
                }
            }
        }

        List<QuizQuestion> allQuestions = questionRepository.findAll();
        Map<Long, QuizQuestion> questionMap = new HashMap<>();
        for (QuizQuestion q : allQuestions) {
            questionMap.put(q.getId(), q);
        }

        List<AdminAnalyticsResponse.MissedQuestionDto> missedQuestions = new ArrayList<>();
        for (Map.Entry<Long, int[]> entry : questionStats.entrySet()) {
            Long qId = entry.getKey();
            int[] stats = entry.getValue();
            int total = stats[0];
            int missed = stats[1];
            double pct = total > 0 ? Math.round(((double) missed / total) * 1000.0) / 10.0 : 0.0;
            QuizQuestion q = questionMap.get(qId);
            String text = q != null ? q.getQuestionText() : "Question #" + qId;
            String cat = q != null && q.getCategory() != null ? q.getCategory().name() : "GENERAL";
            missedQuestions.add(AdminAnalyticsResponse.MissedQuestionDto.builder()
                    .questionId(qId)
                    .questionText(text)
                    .category(cat)
                    .totalAttempts(total)
                    .missedCount(missed)
                    .missedPercentage(pct)
                    .build());
        }

        missedQuestions.sort((a, b) -> {
            int cmp = Double.compare(b.getMissedPercentage(), a.getMissedPercentage());
            if (cmp != 0) return cmp;
            return Integer.compare(b.getMissedCount(), a.getMissedCount());
        });

        if (missedQuestions.isEmpty()) {
            for (QuizQuestion q : allQuestions) {
                missedQuestions.add(AdminAnalyticsResponse.MissedQuestionDto.builder()
                        .questionId(q.getId())
                        .questionText(q.getQuestionText())
                        .category(q.getCategory() != null ? q.getCategory().name() : "GENERAL")
                        .totalAttempts(0)
                        .missedCount(0)
                        .missedPercentage(0.0)
                        .build());
            }
        }

        // 3. Incidents by Category
        Map<String, Long> incidentsByCategory = new LinkedHashMap<>();
        for (com.privacy.entity.IncidentReport.IncidentType type : com.privacy.entity.IncidentReport.IncidentType.values()) {
            incidentsByCategory.put(type.name(), 0L);
        }
        for (com.privacy.entity.IncidentReport report : incidentReportRepository.findAll()) {
            if (report.getIncidentType() != null) {
                String typeName = report.getIncidentType().name();
                incidentsByCategory.put(typeName, incidentsByCategory.getOrDefault(typeName, 0L) + 1L);
            }
        }

        // 4. Incidents by Status
        Map<String, Long> incidentsByStatus = new LinkedHashMap<>();
        for (com.privacy.entity.IncidentReport.ReportStatus status : com.privacy.entity.IncidentReport.ReportStatus.values()) {
            incidentsByStatus.put(status.name(), 0L);
        }
        for (com.privacy.entity.IncidentReport report : incidentReportRepository.findAll()) {
            if (report.getStatus() != null) {
                String statusName = report.getStatus().name();
                incidentsByStatus.put(statusName, incidentsByStatus.getOrDefault(statusName, 0L) + 1L);
            }
        }

        // 5. New Users Per Week
        LocalDate today = LocalDate.now();
        LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, Long> weekMap = new TreeMap<>();
        for (int i = 5; i >= 0; i--) {
            weekMap.put(currentMonday.minusWeeks(i), 0L);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        for (User u : userRepository.findAll()) {
            if (u.getCreatedAt() != null) {
                LocalDate userMonday = u.getCreatedAt().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                if (weekMap.containsKey(userMonday)) {
                    weekMap.put(userMonday, weekMap.get(userMonday) + 1L);
                } else if (userMonday.isBefore(currentMonday.minusWeeks(5))) {
                    LocalDate earliest = currentMonday.minusWeeks(5);
                    weekMap.put(earliest, weekMap.get(earliest) + 1L);
                } else {
                    weekMap.put(userMonday, weekMap.getOrDefault(userMonday, 0L) + 1L);
                }
            } else {
                weekMap.put(currentMonday, weekMap.get(currentMonday) + 1L);
            }
        }

        List<AdminAnalyticsResponse.UserGrowthDto> newUsersPerWeek = weekMap.entrySet().stream()
                .map(entry -> AdminAnalyticsResponse.UserGrowthDto.builder()
                        .weekLabel(entry.getKey().format(formatter))
                        .userCount(entry.getValue())
                        .build())
                .toList();

        return AdminAnalyticsResponse.builder()
                .averageScore(avgScore)
                .totalAssessments(totalAssessments)
                .mostMissedQuestions(missedQuestions)
                .incidentsByCategory(incidentsByCategory)
                .incidentsByStatus(incidentsByStatus)
                .newUsersPerWeek(newUsersPerWeek)
                .build();
    }

    private String formatCategoryName(QuizQuestion.Category cat) {
        return switch (cat) {
            case PHISHING -> "Phishing Attacks";
            case PASSWORD_SAFETY -> "Password Safety";
            case PRIVACY_SETTINGS -> "Privacy Settings";
            case IDENTITY_THEFT -> "Identity Theft";
            case CYBERBULLYING -> "Cyberbullying";
            case OVERSHARING -> "Oversharing Risks";
        };
    }
}