package com.privacy.service;

import com.privacy.dto.AssessmentResultResponse;
import com.privacy.dto.QuizQuestionResponse;
import com.privacy.dto.QuizSubmitRequest;
import com.privacy.entity.*;
import com.privacy.exception.BadRequestException;
import com.privacy.exception.ResourceNotFoundException;
import com.privacy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final AssessmentResultRepository resultRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> getActiveQuestions() {
        return questionRepository.findByActiveTrue().stream()
                .map(QuizQuestionResponse::from)
                .toList();
    }

    @Transactional
    public AssessmentResultResponse submitQuiz(String username, QuizSubmitRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<QuizQuestion> allQuestions = questionRepository.findByActiveTrue();
        if (allQuestions.isEmpty()) {
            throw new BadRequestException("No active quiz questions available");
        }

        Map<Long, QuizQuestion> questionMap = allQuestions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        int totalScore = 0;
        int maxScore = 0;
        int correctCount = 0;
        List<AssessmentAnswer> answers = new ArrayList<>();

        for (QuizSubmitRequest.AnswerDto answerDto : request.getAnswers()) {
            QuizQuestion question = questionMap.get(answerDto.getQuestionId());
            if (question == null) continue;

            QuizOption selectedOption = question.getOptions().stream()
                    .filter(o -> o.getId().equals(answerDto.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "Invalid option ID " + answerDto.getSelectedOptionId()
                                    + " for question " + answerDto.getQuestionId()));

            maxScore += question.getPoints();
            boolean correct = selectedOption.isCorrect();
            int earned = correct ? question.getPoints() : 0;
            if (correct) correctCount++;
            totalScore += earned;

            answers.add(AssessmentAnswer.builder()
                    .question(question)
                    .selectedOption(selectedOption)
                    .correct(correct)
                    .pointsEarned(earned)
                    .build());
        }

        double percentage = maxScore > 0 ? (double) totalScore / maxScore * 100 : 0;
        AssessmentResult.RiskLevel riskLevel = determineRiskLevel(percentage);
        String recommendations = buildRecommendations(riskLevel, answers);

        AssessmentResult result = AssessmentResult.builder()
                .user(user)
                .totalScore(totalScore)
                .maxPossibleScore(maxScore)
                .correctAnswers(correctCount)
                .totalQuestions(answers.size())
                .riskLevel(riskLevel)
                .recommendations(recommendations)
                .build();

        answers.forEach(a -> a.setAssessmentResult(result));
        result.setAnswers(answers);

        AssessmentResult saved = resultRepository.save(result);
        return AssessmentResultResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResultResponse> getUserHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return resultRepository.findByUserIdOrderByTakenAtDesc(user.getId()).stream()
                .map(AssessmentResultResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentResultResponse getResultById(Long resultId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AssessmentResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment result not found"));
        if (!result.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return AssessmentResultResponse.from(result);
    }

    private AssessmentResult.RiskLevel determineRiskLevel(double percentage) {
        if (percentage >= 80) return AssessmentResult.RiskLevel.LOW;
        if (percentage >= 60) return AssessmentResult.RiskLevel.MEDIUM;
        if (percentage >= 40) return AssessmentResult.RiskLevel.HIGH;
        return AssessmentResult.RiskLevel.CRITICAL;
    }

    private String buildRecommendations(AssessmentResult.RiskLevel riskLevel, List<AssessmentAnswer> answers) {
        StringBuilder sb = new StringBuilder();
        sb.append(switch (riskLevel) {
            case LOW -> "Excellent work! Your privacy awareness is strong. Stay vigilant and keep learning.";
            case MEDIUM -> "Good effort! Review the topics below to strengthen your privacy protection.";
            case HIGH -> "Important: Review all learning modules. Your awareness needs improvement to protect yourself online.";
            case CRITICAL -> "Urgent: Your privacy awareness is critically low. Please read all learning modules and review your social media settings immediately.";
        });
        sb.append("\n\nAreas to focus on:\n");
        answers.stream()
                .filter(a -> !a.isCorrect())
                .map(a -> a.getQuestion().getCategory().name().replace("_", " "))
                .distinct()
                .forEach(cat -> sb.append("• ").append(cat).append("\n"));
        return sb.toString().trim();
    }
}
