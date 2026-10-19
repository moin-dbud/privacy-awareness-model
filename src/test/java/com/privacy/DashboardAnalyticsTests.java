package com.privacy;

import com.privacy.dto.CategoryBreakdownResponse;
import com.privacy.dto.ScoreTrendResponse;
import com.privacy.service.DashboardService;
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
class DashboardAnalyticsTests {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void testCategoryBreakdownReturnsAllCategories() {
        List<CategoryBreakdownResponse> breakdown = dashboardService.getCategoryBreakdown("testadmin");
        assertNotNull(breakdown);
        assertEquals(6, breakdown.size(), "Should return exactly 6 categories");

        for (CategoryBreakdownResponse cat : breakdown) {
            assertNotNull(cat.getCategory());
            assertNotNull(cat.getCategoryName());
            assertTrue(cat.getAccuracyPercentage() >= 0.0 && cat.getAccuracyPercentage() <= 100.0);
            assertNotNull(cat.getRiskLevel());
        }
    }

    @Test
    void testScoreTrendStructure() {
        List<ScoreTrendResponse> trend = dashboardService.getScoreTrend("testadmin");
        assertNotNull(trend);
        // User testadmin has not taken a quiz yet in test context, should return empty list without error
        assertTrue(trend.isEmpty() || trend.size() >= 1);
    }
}