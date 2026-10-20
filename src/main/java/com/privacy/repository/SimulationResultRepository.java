package com.privacy.repository;

import com.privacy.entity.SimulationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationResultRepository extends JpaRepository<SimulationResult, Long> {
    List<SimulationResult> findByUserIdOrderByTakenAtDesc(Long userId);
    List<SimulationResult> findByUserIdAndScenarioIdOrderByTakenAtDesc(Long userId, Long scenarioId);
    long countByUserId(Long userId);
    long countByPassedTrue();
    long countByUserIdAndPassedTrue(Long userId);
}