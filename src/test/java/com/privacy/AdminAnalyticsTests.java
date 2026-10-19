package com.privacy;

import com.privacy.dto.AdminAnalyticsResponse;
import com.privacy.entity.IncidentReport;
import com.privacy.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb_admin_analytics;DB_CLOSE_DELAY=-1",
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
class AdminAnalyticsTests {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void testAdminAnalyticsReturnsCompletePayload() {
        AdminAnalyticsResponse response = dashboardService.getAdminAnalytics();
        assertNotNull(response, "Admin analytics response must not be null");

        // Average score & total assessments
        assertTrue(response.getAverageScore() >= 0.0, "Average score must be non-negative");
        assertTrue(response.getTotalAssessments() >= 0, "Total assessments count must be non-negative");

        // Incidents by Category
        assertNotNull(response.getIncidentsByCategory(), "Incidents by category map must not be null");
        for (IncidentReport.IncidentType type : IncidentReport.IncidentType.values()) {
            assertTrue(response.getIncidentsByCategory().containsKey(type.name()),
                    "Incidents by category should contain key: " + type.name());
        }

        // Incidents by Status
        assertNotNull(response.getIncidentsByStatus(), "Incidents by status map must not be null");
        for (IncidentReport.ReportStatus status : IncidentReport.ReportStatus.values()) {
            assertTrue(response.getIncidentsByStatus().containsKey(status.name()),
                    "Incidents by status should contain key: " + status.name());
        }

        // New Users per Week
        assertNotNull(response.getNewUsersPerWeek(), "New users per week list must not be null");
        assertEquals(6, response.getNewUsersPerWeek().size(), "Should have exactly 6 weeks in user growth timeline");
        for (AdminAnalyticsResponse.UserGrowthDto growth : response.getNewUsersPerWeek()) {
            assertNotNull(growth.getWeekLabel(), "Week label must not be null");
            assertTrue(growth.getUserCount() >= 0, "User count must be non-negative");
        }

        // Most-Missed Questions
        assertNotNull(response.getMostMissedQuestions(), "Most missed questions list must not be null");
        assertFalse(response.getMostMissedQuestions().isEmpty(), "Most missed questions should list seeded questions");
        for (AdminAnalyticsResponse.MissedQuestionDto q : response.getMostMissedQuestions()) {
            assertNotNull(q.getQuestionId(), "Question ID must not be null");
            assertNotNull(q.getQuestionText(), "Question text must not be null");
            assertNotNull(q.getCategory(), "Category must not be null");
            assertTrue(q.getMissedPercentage() >= 0.0 && q.getMissedPercentage() <= 100.0,
                    "Missed percentage must be within 0-100%");
        }
    }
}
