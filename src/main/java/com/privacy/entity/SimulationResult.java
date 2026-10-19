package com.privacy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimulationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private PhishingScenario scenario;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer totalPossibleFlags;

    @Column(nullable = false)
    private Integer flagsIdentified;

    @Column(nullable = false)
    private Integer falsePositives;

    @Column(nullable = false)
    private boolean passed;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime takenAt;
}