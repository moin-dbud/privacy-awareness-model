package com.privacy.controller;

import com.privacy.dto.AssessmentResultResponse;
import com.privacy.dto.QuizQuestionResponse;
import com.privacy.dto.QuizSubmitRequest;
import com.privacy.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/questions")
    public ResponseEntity<List<QuizQuestionResponse>> getQuestions() {
        return ResponseEntity.ok(quizService.getActiveQuestions());
    }

    @PostMapping("/submit")
    public ResponseEntity<AssessmentResultResponse> submitQuiz(
            @Valid @RequestBody QuizSubmitRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(quizService.submitQuiz(authentication.getName(), request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AssessmentResultResponse>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(quizService.getUserHistory(authentication.getName()));
    }

    @GetMapping("/result/{id}")
    public ResponseEntity<AssessmentResultResponse> getResult(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(quizService.getResultById(id, authentication.getName()));
    }
}
