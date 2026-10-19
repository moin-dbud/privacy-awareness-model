package com.privacy.repository;

import com.privacy.entity.PhishingRedFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhishingRedFlagRepository extends JpaRepository<PhishingRedFlag, Long> {
    List<PhishingRedFlag> findByScenarioIdOrderByDisplayOrderAsc(Long scenarioId);
}