package com.privacy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "phishing_red_flags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhishingRedFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private PhishingScenario scenario;

    @Column(nullable = false, length = 255)
    private String flagText;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}