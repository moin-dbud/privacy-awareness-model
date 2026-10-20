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
    private final SimulationResultRepository simulationResultRepository;

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

        // Incident reports by status
        List<com.privacy.entity.IncidentReport> userIncidents = incidentReportRepository.findByUserIdOrderByReportedAtDesc(user.getId());
        summary.put("totalIncidentReports", (long) userIncidents.size());
        long openInc = userIncidents.stream().filter(i -> i.getStatus() == com.privacy.entity.IncidentReport.ReportStatus.OPEN).count();
        long underReviewInc = userIncidents.stream().filter(i -> i.getStatus() == com.privacy.entity.IncidentReport.ReportStatus.UNDER_REVIEW).count();
        long resolvedInc = userIncidents.stream().filter(i -> i.getStatus() == com.privacy.entity.IncidentReport.ReportStatus.RESOLVED).count();
        long dismissedInc = userIncidents.stream().filter(i -> i.getStatus() == com.privacy.entity.IncidentReport.ReportStatus.DISMISSED).count();
        summary.put("openIncidentReports", openInc);
        summary.put("underReviewIncidentReports", underReviewInc);
        summary.put("resolvedIncidentReports", resolvedInc);
        summary.put("dismissedIncidentReports", dismissedInc);

        // Phishing simulations
        long userSimulations = simulationResultRepository.countByUserId(user.getId());
        long passedSimulations = simulationResultRepository.countByUserIdAndPassedTrue(user.getId());
        double simPassRate = userSimulations > 0 ? Math.round(((double) passedSimulations / userSimulations) * 1000.0) / 10.0 : 0.0;
        summary.put("totalSimulations", userSimulations);
        summary.put("passedSimulations", passedSimulations);
        summary.put("simulationPassRate", simPassRate);

        // Recent activity
        List<Map<String, Object>> recentAssessments = resultRepository.findByUserIdOrderByTakenAtDesc(user.getId()).stream().limit(3).map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("score", r.getTotalScore());
            m.put("maxScore", r.getMaxPossibleScore());
            m.put("percentage", r.getPercentageScore());
            m.put("riskLevel", r.getRiskLevel().name());
            m.put("takenAt", r.getTakenAt());
            return m;
        }).toList();
        summary.put("recentAssessments", recentAssessments);

        List<Map<String, Object>> recentIncidents = userIncidents.stream().limit(3).map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", i.getId());
            m.put("title", i.getTitle());
            m.put("type", i.getIncidentType() != null ? i.getIncidentType().name() : "OTHER");
            m.put("status", i.getStatus() != null ? i.getStatus().name() : "OPEN");
            m.put("reportedAt", i.getReportedAt());
            return m;
        }).toList();
        summary.put("recentIncidents", recentIncidents);

        // Personalized recommendations
        List<Map<String, Object>> recommendations = generateRecommendations(user, totalAssessments, completedModules, totalModules, userIncidents);
        summary.put("recommendations", recommendations);

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
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        long disabledUsers = userRepository.countByEnabledFalse();
        long totalAssessments = resultRepository.count();

        List<AssessmentResult> results = resultRepository.findAll();
        double avgQuizScore = 0.0;
        if (!results.isEmpty()) {
            double sum = results.stream().mapToDouble(AssessmentResult::getPercentageScore).sum();
            avgQuizScore = Math.round((sum / results.size()) * 10.0) / 10.0;
        }

        long totalIncidents = incidentReportRepository.count();
        long openInc = incidentReportRepository.findByStatusOrderByReportedAtDesc(com.privacy.entity.IncidentReport.ReportStatus.OPEN).size();
        long underReviewInc = incidentReportRepository.findByStatusOrderByReportedAtDesc(com.privacy.entity.IncidentReport.ReportStatus.UNDER_REVIEW).size();
        long resolvedInc = incidentReportRepository.findByStatusOrderByReportedAtDesc(com.privacy.entity.IncidentReport.ReportStatus.RESOLVED).size();
        long dismissedInc = incidentReportRepository.findByStatusOrderByReportedAtDesc(com.privacy.entity.IncidentReport.ReportStatus.DISMISSED).size();

        long totalModules = moduleRepository.count();
        long activeModules = moduleRepository.findByActiveTrueOrderByDisplayOrderAsc().size();
        long totalSimulations = simulationResultRepository.count();
        long passedSimulations = simulationResultRepository.countByPassedTrue();
        double simPassRate = totalSimulations > 0 ? Math.round(((double) passedSimulations / totalSimulations) * 1000.0) / 10.0 : 0.0;

        summary.put("totalUsers", totalUsers);
        summary.put("activeUsers", activeUsers);
        summary.put("disabledUsers", disabledUsers);
        summary.put("totalAssessments", totalAssessments);
        summary.put("averageQuizScore", avgQuizScore);
        summary.put("totalIncidentReports", totalIncidents);
        summary.put("openIncidents", openInc);
        summary.put("underReviewIncidents", underReviewInc);
        summary.put("resolvedIncidents", resolvedInc);
        summary.put("dismissedIncidents", dismissedInc);
        summary.put("totalLearningModules", totalModules);
        summary.put("activeLearningModules", activeModules);
        summary.put("totalSimulations", totalSimulations);
        summary.put("passedSimulations", passedSimulations);
        summary.put("simulationPassRate", simPassRate);
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

        // Phishing simulation metrics
        long totalSimulations = simulationResultRepository.count();
        long passedSimulations = simulationResultRepository.countByPassedTrue();
        double simPassRate = totalSimulations > 0 ? Math.round(((double) passedSimulations / totalSimulations) * 1000.0) / 10.0 : 0.0;
        List<com.privacy.entity.SimulationResult> simResults = simulationResultRepository.findAll();
        double simAvgScore = simResults.isEmpty() ? 0.0 : Math.round((simResults.stream().mapToInt(com.privacy.entity.SimulationResult::getScore).average().orElse(0.0)) * 10.0) / 10.0;

        // Module completion metrics
        Map<String, Long> moduleCompletions = new LinkedHashMap<>();
        for (com.privacy.entity.LearningModule m : moduleRepository.findByActiveTrueOrderByDisplayOrderAsc()) {
            moduleCompletions.put(m.getTitle(), progressRepository.countByModuleIdAndCompletedTrue(m.getId()));
        }

        // Category accuracy breakdown across all users
        Map<String, Double> categoryAccuracy = new LinkedHashMap<>();
        for (QuizQuestion.Category cat : QuizQuestion.Category.values()) {
            long totalCatAnswers = 0;
            long correctCatAnswers = 0;
            for (AssessmentResult res : allResults) {
                if (res.getAnswers() != null) {
                    for (AssessmentAnswer ans : res.getAnswers()) {
                        if (ans.getQuestion() != null && ans.getQuestion().getCategory() == cat) {
                            totalCatAnswers++;
                            if (ans.isCorrect()) correctCatAnswers++;
                        }
                    }
                }
            }
            double catPct = totalCatAnswers > 0 ? Math.round(((double) correctCatAnswers / totalCatAnswers) * 1000.0) / 10.0 : 0.0;
            categoryAccuracy.put(formatCategoryName(cat), catPct);
        }

        return AdminAnalyticsResponse.builder()
                .averageScore(avgScore)
                .totalAssessments(totalAssessments)
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByEnabledTrue())
                .disabledUsers(userRepository.countByEnabledFalse())
                .totalSimulations(totalSimulations)
                .simulationPassRate(simPassRate)
                .simulationAverageScore(simAvgScore)
                .moduleCompletions(moduleCompletions)
                .categoryAccuracy(categoryAccuracy)
                .mostMissedQuestions(missedQuestions)
                .incidentsByCategory(incidentsByCategory)
                .incidentsByStatus(incidentsByStatus)
                .newUsersPerWeek(newUsersPerWeek)
                .build();
    }

    private List<Map<String, Object>> generateRecommendations(User user, long totalAssessments, long completedModules, long totalModules, List<com.privacy.entity.IncidentReport> incidents) {
        List<Map<String, Object>> list = new ArrayList<>();

        if (totalAssessments == 0) {
            list.add(Map.of(
                "category", "ASSESSMENT",
                "priority", "HIGH",
                "title", "Take Privacy Awareness Assessment",
                "description", "Evaluate your risk profile across 6 key privacy dimensions.",
                "actionUrl", "/quiz.html",
                "actionText", "Start Quiz"
            ));
        } else {
            List<CategoryBreakdownResponse> breakdown = getCategoryBreakdown(user.getUsername());
            CategoryBreakdownResponse weakest = breakdown.stream()
                .filter(b -> b.getTotalQuestions() > 0 && b.getAccuracyPercentage() < 75.0)
                .min(Comparator.comparingDouble(CategoryBreakdownResponse::getAccuracyPercentage))
                .orElse(null);

            if (weakest != null) {
                list.add(Map.of(
                    "category", weakest.getCategory(),
                    "priority", "HIGH",
                    "title", "Improve " + weakest.getCategoryName() + " (" + weakest.getAccuracyPercentage() + "% Accuracy)",
                    "description", "Review learning materials to understand key vulnerability vectors and defensive measures.",
                    "actionUrl", "/learning.html",
                    "actionText", "Explore Modules"
                ));
            }
        }

        if (completedModules < totalModules) {
            list.add(Map.of(
                "category", "LEARNING",
                "priority", "MEDIUM",
                "title", "Complete Privacy Learning Curricula (" + completedModules + "/" + totalModules + " completed)",
                "description", "Deepen your knowledge of password safety, identity theft, and privacy settings.",
                "actionUrl", "/learning.html",
                "actionText", "Continue Learning"
            ));
        }

        long unresolved = incidents.stream().filter(i -> i.getStatus() == com.privacy.entity.IncidentReport.ReportStatus.OPEN || i.getStatus() == com.privacy.entity.IncidentReport.ReportStatus.UNDER_REVIEW).count();
        if (unresolved > 0) {
            list.add(Map.of(
                "category", "INCIDENT",
                "priority", "HIGH",
                "title", "Active Security Incident Pending (" + unresolved + " unresolved)",
                "description", "Review your reported incidents and ensure you have applied security countermeasures.",
                "actionUrl", "/incidents.html",
                "actionText", "View Incident"
            ));
        }

        long simCount = simulationResultRepository.countByUserId(user.getId());
        if (simCount == 0) {
            list.add(Map.of(
                "category", "SIMULATION",
                "priority", "MEDIUM",
                "title", "Practice in the Phishing Simulator",
                "description", "Test your detection skills against realistic email, SMS, and DM phishing lures.",
                "actionUrl", "/simulator.html",
                "actionText", "Launch Simulator"
            ));
        }

        return list;
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