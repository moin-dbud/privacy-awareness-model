package com.privacy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssessmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer totalScore;

    @Column(nullable = false)
    private Integer maxPossibleScore;

    @Column(nullable = false)
    private Integer correctAnswers;

    @Column(nullable = false)
    private Integer totalQuestions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime takenAt;

    @OneToMany(mappedBy = "assessmentResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssessmentAnswer> answers = new ArrayList<>();

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public double getPercentageScore() {
        if (maxPossibleScore == 0) return 0;
        return (double) totalScore / maxPossibleScore * 100;
    }
}
