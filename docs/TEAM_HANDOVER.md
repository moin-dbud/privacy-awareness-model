# Privacy Guard — Teammate Handover & Developer Guide

This document is a practical onboarding guide for teammates and new developers contributing to **Privacy Guard**. It explains where files are located, how the application works under the hood, how to add new features, and how to debug common issues.

---

## 1. Codebase Directory Map

```
Phishing System/
├── pom.xml                                 # Maven dependencies and build settings
├── .env.example                            # Template for local environment variables
├── Set-Env.ps1                             # Helper to load .env into PowerShell session
├── test-runner.ps1                         # 40-test live MySQL integration test runner
│
├── src/main/java/com/privacy/
│   ├── PrivacyAwarenessApplication.java   # Spring Boot application entry point
│   │
│   ├── config/                             # Security and initialization
│   │   ├── SecurityConfig.java             # Spring Security filter chain, RBAC, CSRF
│   │   ├── UserAccountStatusFilter.java    # Checks DB state to revoke deactivated sessions
│   │   ├── CustomUserDetailsService.java   # Loads user from DB for Spring Security
│   │   └── DataSeeder.java                 # Seeds admin, questions, modules, scenarios
│   │
│   ├── controller/                         # REST API controllers
│   │   ├── AuthController.java             # Login, register, me, change-password
│   │   ├── DashboardController.java        # User metrics, trends, category breakdowns
│   │   ├── QuizController.java             # Question delivery, answer submission, history
│   │   ├── LearningController.java         # Modules, completion tracking, progress
│   │   ├── IncidentController.java         # Incident filing and user list
│   │   ├── PhishingSimulatorController.java# Scenarios, lure evaluation, feedback
│   │   └── AdminController.java            # Admin analytics, user ops, triage, exports, CRUD
│   │
│   ├── service/                            # Core business logic
│   │   ├── UserService.java                # Password hashing, registration, role governance
│   │   ├── DashboardService.java           # Real MySQL aggregations for user & admin
│   │   ├── QuizService.java                # Score calculations, risk tiers, recommendations
│   │   ├── LearningService.java            # Module management and progress records
│   │   ├── IncidentService.java            # Incident updates, CSV formula shielding, OpenPDF
│   │   └── PhishingSimulatorService.java   # Red flag evaluation and feedback
│   │
│   ├── entity/                             # JPA Hibernate entities (11 tables)
│   ├── repository/                         # Spring Data JPA repositories (11 repos)
│   ├── dto/                                # Request & Response Data Transfer Objects
│   └── exception/                          # Global exception handler & custom exceptions
│
├── src/main/resources/
│   ├── application.properties              # Database URL, HikariCP, server port
│   └── static/                             # Frontend HTML, CSS, JavaScript
│       ├── index.html                      # Landing page
│       ├── login.html & register.html      # Authentication
│       ├── dashboard.html                  # User overview, score trends, recommendations
│       ├── quiz.html                       # 15-question privacy assessment
│       ├── learning.html                   # 6 educational modules
│       ├── simulator.html                  # 10 phishing scenario exercises
│       ├── password-checker.html           # Password strength testing
│       ├── incidents.html                  # User incident reporting
│       ├── admin.html                      # Admin dashboard (KPIs, triage, CRUD, exports)
│       ├── css/style.css                   # Custom dark-theme glassmorphism styling
│       └── js/app.js                       # Shared frontend client (fetch, CSRF, modals)
│
├── docs/                                   # Architectural diagrams and presentations
│   ├── SYSTEM_ARCHITECTURE.md              # Request lifecycle & layer breakdown
│   ├── ER_DIAGRAM.md                       # Mermaid entity-relationship diagram
│   ├── DEMO_GUIDE.md                       # Viva demonstration walkthrough
│   ├── TEAM_HANDOVER.md                    # This developer guide
│   └── Privacy_Guard_Presentation.pptx     # 15-slide presentation deck
```

---

## 2. Daily Development Workflow

### Step 1: Start MySQL
Ensure your local MySQL 8.0 service is active:
```powershell
Get-Service MySQL80
```

### Step 2: Export Environment Variables & Run
Always export your environment variables into your PowerShell terminal before launching:
```powershell
cd "C:\Users\my\Downloads\Phishing System"
. .\Set-Env.ps1
mvn spring-boot:run
```
*(Notice the leading dot and space: `. .\Set-Env.ps1`).*

### Step 3: Run the Test Suites
Before committing changes, run both suites:
```powershell
# 1. Fast unit & slice tests on H2:
mvn clean test

# 2. Live end-to-end regression tests on MySQL (while server is running):
.\test-runner.ps1
```

---

## 3. How Core Subsystems Work

### 3.1 Security & Session Management
1. **Login Flow:** User posts `{username, password}` to `/api/auth/login`. Spring Security's `AuthenticationManager` uses `CustomUserDetailsService` to verify the BCrypt hash. On success, an `HttpSession` is created and `JSESSIONID` is returned as a cookie.
2. **CSRF Protection:** Spring Security issues a cookie named `XSRF-TOKEN`. The frontend (`app.js`) reads this cookie and adds it as the `X-XSRF-TOKEN` header on all `POST`, `PUT`, `PATCH`, and `DELETE` requests.
3. **Session Invalidation Filter (`UserAccountStatusFilter`):** On every incoming request, this filter queries `userRepository.findByUsername(username)`. If the user is missing (deleted) or `enabled == false`, it immediately calls `session.invalidate()` and returns HTTP 401.

### 3.2 Real MySQL Dashboard Metrics
All statistics are computed dynamically using Spring Data JPA queries in `DashboardService.java`:
- User metrics count assessments, incidents, completed modules, and simulation pass rates for `user.id`.
- If a user has zero records, empty-state prompts guide them to the quiz or simulator.
- Admin metrics aggregate counts across the entire `users`, `assessment_results`, `incident_reports`, and `simulation_results` tables.

### 3.3 Relational Deletion Safeguards
- When an administrator deletes a question or module in `AdminController`, the controller first checks if records exist in `assessment_answers` or `module_progress`.
- If records exist, the controller throws a `BadRequestException` (HTTP 400), prompting staff to deactivate the item using the toggle switch instead of breaking relational integrity.

---

## 4. How to Extend the Application

### Adding a New Quiz Question
1. You can add questions directly via the **Admin Panel** (`admin.html`) without touching code.
2. If you want to seed it automatically on fresh installs, add an entry to the `seedQuizQuestions()` method in [DataSeeder.java](../src/main/java/com/privacy/config/DataSeeder.java):
   ```java
   buildQ("Your question prompt here?",
       QuizQuestion.Category.PASSWORD_SAFETY,
       "Educational explanation here.",
       new String[]{"Choice 1", "Choice 2", "Choice 3", "Choice 4"}, 0) // 0 is index of correct answer
   ```

### Adding a New REST Endpoint
1. Define a request DTO with Jakarta Bean Validation annotations in `com.privacy.dto`.
2. Define a response DTO in `com.privacy.dto`.
3. Add the business method to the appropriate Service in `com.privacy.service`.
4. Expose the endpoint in the relevant Controller in `com.privacy.controller`.
5. Update `SecurityConfig.java` if the endpoint requires specific role authorization.
6. Document the endpoint in [API_DOCUMENTATION.md](../API_DOCUMENTATION.md).

### Frontend Standards (`app.js` and `style.css`)
- **API Calls:** Use the helper functions in `app.js` (`apiGet`, `apiPost`, `apiPatch`, `apiDelete`). They automatically attach the CSRF header, handle JSON parsing, and redirect to `login.html` if an HTTP 401 response is received.
- **XSS Prevention:** Always escape user-supplied strings using `escapeHtml(text)` before inserting into `innerHTML`.
- **UI Feedback:** Use `showToast('Message', 'success' | 'error')` for user feedback.

---

## 5. Common Debugging Tips

| Problem | Cause | Fix |
|---|---|---|
| `Port 8080 was already in use` | A previous instance is still running | Find PID using `Get-NetTCPConnection -LocalPort 8080` and stop it via `Stop-Process -Id <PID> -Force`. |
| `Access denied for user 'privacy_user'` | Password mismatch in `.env` | Verify `DB_PASSWORD` in `.env` matches what was set in MySQL. |
| Admin login fails with 401 | Database hash differs from `.env` | Set `ADMIN_RECOVER=true` in `.env`, run `. .\Set-Env.ps1`, and restart to sync the admin credentials. |
| CSRF 403 Forbidden | Missing `X-XSRF-TOKEN` header | Ensure you made a `GET` request first to acquire the cookie, and that your client includes the `X-XSRF-TOKEN` header. |
