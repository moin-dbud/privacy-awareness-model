package com.privacy;

import com.privacy.dto.CreateUserRequest;
import com.privacy.dto.UpdateUserRoleRequest;
import com.privacy.dto.UserResponse;
import com.privacy.entity.AssessmentResult;
import com.privacy.entity.User;
import com.privacy.exception.BadRequestException;
import com.privacy.exception.ConflictException;
import com.privacy.repository.AssessmentResultRepository;
import com.privacy.repository.UserRepository;
import com.privacy.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb_admin_users;DB_CLOSE_DELAY=-1",
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
@Transactional
class AdminUserManagementTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testCreateUserSuccess() {
        CreateUserRequest req = CreateUserRequest.builder()
                .username("new_developer")
                .email("developer@domain.test")
                .password("DevSecure@2026!")
                .fullName("Dev Tester")
                .role(User.Role.USER)
                .build();

        UserResponse res = userService.createUserByAdmin(req);
        assertNotNull(res.getId());
        assertEquals("new_developer", res.getUsername());
        assertEquals("developer@domain.test", res.getEmail());
        assertEquals("Dev Tester", res.getFullName());
        assertEquals(User.Role.USER, res.getRole());
        assertTrue(res.isEnabled());

        // Verify password in DB is hashed and not plaintext
        User userInDb = userRepository.findById(res.getId()).orElseThrow();
        assertNotEquals("DevSecure@2026!", userInDb.getPasswordHash());
        assertTrue(passwordEncoder.matches("DevSecure@2026!", userInDb.getPasswordHash()));
    }

    @Test
    void testDuplicateEmailRejected() {
        CreateUserRequest req1 = CreateUserRequest.builder()
                .username("unique_user_1")
                .email("duplicate@domain.test")
                .password("DevSecure@2026!")
                .role(User.Role.USER)
                .build();
        userService.createUserByAdmin(req1);

        CreateUserRequest req2 = CreateUserRequest.builder()
                .username("unique_user_2")
                .email("duplicate@domain.test")
                .password("DevSecure@2026!")
                .role(User.Role.USER)
                .build();

        ConflictException ex = assertThrows(ConflictException.class, () -> userService.createUserByAdmin(req2));
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    void testSelfDeleteBlocked() {
        User admin = userRepository.findByUsername("testadmin").orElseThrow();
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.deleteUserByAdmin(admin.getId(), "testadmin"));
        assertTrue(ex.getMessage().contains("cannot delete your own"));
    }

    @Test
    void testLastAdminDeleteBlocked() {
        // Ensure there is only 1 admin ('testadmin')
        User admin = userRepository.findByUsername("testadmin").orElseThrow();
        assertEquals(1, userRepository.countByRole(User.Role.ADMIN));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.deleteUserByAdmin(admin.getId(), "otheradmin"));
        assertTrue(ex.getMessage().contains("last remaining administrator"));
    }

    @Test
    void testLastAdminDemoteBlocked() {
        User admin = userRepository.findByUsername("testadmin").orElseThrow();
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.updateUserRole(admin.getId(), User.Role.USER, "otheradmin"));
        assertTrue(ex.getMessage().contains("last remaining administrator"));
    }

    @Test
    void testRoleUpdateAndToggleStatus() {
        CreateUserRequest req = CreateUserRequest.builder()
                .username("mod_user")
                .email("mod@domain.test")
                .password("ModSecure@2026!")
                .role(User.Role.USER)
                .build();
        UserResponse created = userService.createUserByAdmin(req);

        // Promote to ADMIN
        UserResponse promoted = userService.updateUserRole(created.getId(), User.Role.ADMIN, "testadmin");
        assertEquals(User.Role.ADMIN, promoted.getRole());

        // Toggle status (Active -> Disabled)
        UserResponse deactivated = userService.toggleUserStatus(created.getId(), "testadmin");
        assertFalse(deactivated.isEnabled());

        // Toggle back (Disabled -> Active)
        UserResponse reactivated = userService.toggleUserStatus(created.getId(), "testadmin");
        assertTrue(reactivated.isEnabled());
    }

    @Test
    void testDeleteUserWithCascade() {
        CreateUserRequest req = CreateUserRequest.builder()
                .username("cascade_user")
                .email("cascade@domain.test")
                .password("Cascade@2026!")
                .role(User.Role.USER)
                .build();
        UserResponse created = userService.createUserByAdmin(req);
        User userEntity = userRepository.findById(created.getId()).orElseThrow();

        // Create an assessment result for this user
        AssessmentResult result = AssessmentResult.builder()
                .user(userEntity)
                .totalScore(80)
                .maxPossibleScore(100)
                .correctAnswers(8)
                .totalQuestions(10)
                .riskLevel(AssessmentResult.RiskLevel.LOW)
                .recommendations("Good job")
                .build();
        userEntity.getAssessmentResults().add(result);
        userRepository.save(userEntity);

        assertEquals(1, assessmentResultRepository.findByUserIdOrderByTakenAtDesc(userEntity.getId()).size());

        // Now delete the user
        userService.deleteUserByAdmin(created.getId(), "testadmin");

        // Verify user is gone
        assertFalse(userRepository.findById(created.getId()).isPresent());

        // Verify assessment result cascaded and is also deleted
        assertEquals(0, assessmentResultRepository.findByUserIdOrderByTakenAtDesc(created.getId()).size());
    }

    @Test
    void testFilteredSearch() {
        CreateUserRequest u1 = CreateUserRequest.builder()
                .username("search_charlie")
                .email("charlie@privacy.local")
                .fullName("Charlie Security")
                .password("CharliePass@123!")
                .role(User.Role.USER)
                .build();
        CreateUserRequest u2 = CreateUserRequest.builder()
                .username("search_diana")
                .email("diana@privacy.local")
                .fullName("Diana Admin")
                .password("DianaPass@123!")
                .role(User.Role.ADMIN)
                .build();
        userService.createUserByAdmin(u1);
        userService.createUserByAdmin(u2);

        List<UserResponse> searchResult = userService.getAllUsersFiltered("charlie", "USER");
        assertEquals(1, searchResult.size());
        assertEquals("search_charlie", searchResult.get(0).getUsername());

        List<UserResponse> adminResult = userService.getAllUsersFiltered("diana", "ADMIN");
        assertEquals(1, adminResult.size());
        assertEquals("search_diana", adminResult.get(0).getUsername());
    }
}
