package com.privacy;

import com.privacy.dto.PhishingScenarioResponse;
import com.privacy.dto.PhishingSubmitRequest;
import com.privacy.dto.SimulationResultResponse;
import com.privacy.entity.PhishingRedFlag;
import com.privacy.entity.PhishingScenario;
import com.privacy.repository.PhishingScenarioRepository;
import com.privacy.repository.SimulationResultRepository;
import com.privacy.service.PhishingSimulatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never",
        "app.admin.username=testadmin",
        "app.admin.password=Test@1234!",
        "app.admin.email=admin@test.local"
})
@org.springframework.transaction.annotation.Transactional
class PhishingSimulatorTests {

    @Autowired
    private PhishingSimulatorService simulatorService;

    @Autowired
    private PhishingScenarioRepository scenarioRepository;

    @Autowired
    private SimulationResultRepository resultRepository;

    @Test
    void testScenariosSeededAndRetrieved() {
        List<PhishingScenarioResponse> scenarios = simulatorService.getAllScenarios();
        assertNotNull(scenarios);
        assertEquals(10, scenarios.size(), "Should have exactly 10 seeded scenarios");

        for (PhishingScenarioResponse s : scenarios) {
            assertNotNull(s.getTitle());
            assertNotNull(s.getChannel());
            assertNotNull(s.getSender());
            assertNotNull(s.getContent());
            assertFalse(s.getRedFlagOptions().isEmpty(), "Each scenario must have candidate red flags");
        }
    }

    @Test
    void testSimulationSubmissionAndScoring() {
        List<PhishingScenarioResponse> scenarios = simulatorService.getAllScenarios();
        assertFalse(scenarios.isEmpty());

        PhishingScenarioResponse firstScenario = scenarios.get(0);
        PhishingScenario dbScenario = scenarioRepository.findById(firstScenario.getId()).orElseThrow();

        // Get actual correct red flag IDs from database
        List<Long> correctFlagIds = dbScenario.getRedFlags().stream()
                .filter(PhishingRedFlag::isCorrect)
                .map(PhishingRedFlag::getId)
                .toList();

        assertFalse(correctFlagIds.isEmpty());

        PhishingSubmitRequest request = PhishingSubmitRequest.builder()
                .scenarioId(firstScenario.getId())
                .selectedFlagIds(correctFlagIds)
                .build();

        SimulationResultResponse response = simulatorService.submitSimulation("testadmin", request);

        assertNotNull(response);
        assertEquals(100, response.getScore(), "Selecting all actual red flags should yield 100%");
        assertTrue(response.isPassed());
        assertEquals(correctFlagIds.size(), response.getFlagsIdentified());
        assertEquals(0, response.getFalsePositives());
        assertNotNull(response.getGeneralExplanation());
        assertNotNull(response.getFlagDetails());

        // Verify persisted in repository
        assertTrue(resultRepository.countByUserId(response.getId()) >= 0);
    }
}