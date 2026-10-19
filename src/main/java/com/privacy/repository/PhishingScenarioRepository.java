package com.privacy.repository;

import com.privacy.entity.PhishingScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhishingScenarioRepository extends JpaRepository<PhishingScenario, Long> {
    List<PhishingScenario> findByActiveTrueOrderByIdAsc();
    List<PhishingScenario> findByChannelAndActiveTrueOrderByIdAsc(PhishingScenario.Channel channel);
}