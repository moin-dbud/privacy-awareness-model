# Privacy Guard — System Architecture

This document describes the multi-tiered software architecture of **Privacy Guard — Social Media Privacy Awareness and Security Management System**, explaining the request lifecycle, security filters, backend layers, and database integration.

---

## 1. System Architecture Diagram

```mermaid
graph TB
    subgraph Client Tier ["Client Tier (Browser)"]
        UI_User["User Interface<br/>• index.html (Home)<br/>• login.html & register.html<br/>• dashboard.html (User Dashboard)<br/>• quiz.html (Privacy Assessment)<br/>• learning.html (Curriculum Center)<br/>• simulator.html (Phishing Lab)<br/>• password-checker.html (Strength Tool)<br/>• incidents.html (Incident Center)"]
        UI_Admin["Administrator Interface<br/>• admin.html (Admin Portal)<br/>  - Analytics & Charts<br/>  - User Management<br/>  - Incident Triage & Exports<br/>  - Quiz Question CRUD<br/>  - Learning Module CRUD"]
        JS_App["Frontend JavaScript<br/>• app.js (API Fetch Client, CSRF, Modals, Toasts)"]
    end

    subgraph Security Tier ["Security Filter Chain (Spring Security 6)"]
        SecContext["SecurityContextHolderFilter"]
        StatusFilter["UserAccountStatusFilter<br/>(Validates DB existence & active status on each request)"]
        CSRF["CsrfFilter (CookieCsrfTokenRepository)"]
        LogoutFilter["LogoutFilter"]
        AuthFilter["AuthorizationFilter<br/>• /api/auth/** (Public)<br/>• /api/admin/** & admin.html (ROLE_ADMIN)<br/>• /api/** (ROLE_USER or ROLE_ADMIN)"]
    end

    subgraph Application Tier ["Application Tier (Spring Boot 3.2.5)"]
        subgraph Controllers ["REST Controllers"]
            AuthController["AuthController (/api/auth)"]
            DashboardController["DashboardController (/api/dashboard)"]
            QuizController["QuizController (/api/quiz)"]
            LearningController["LearningController (/api/learning)"]
            IncidentController["IncidentController (/api/incidents)"]
            SimulatorController["PhishingSimulatorController (/api/simulator)"]
            AdminController["AdminController (/api/admin)"]
        end

        subgraph Services ["Domain Services"]
            UserService["UserService (User registration, roles, password hashing)"]
            DashboardService["DashboardService (Real-time MySQL metric aggregation)"]
            QuizService["QuizService (Question retrieval, answer scoring, risk tiering)"]
            LearningService["LearningService (Curriculum retrieval & completion tracking)"]
            IncidentService["IncidentService (Incident triage, CSV formula shielding, OpenPDF)"]
            PhishingSimulatorService["PhishingSimulatorService (Scenario evaluation & feedback)"]
        end
    end

    subgraph Persistence Tier ["Persistence Tier (Spring Data JPA / Hibernate)"]
        UserRepo["UserRepository"]
        QuizRepo["QuizQuestionRepository & QuizOptionRepository"]
        AnswerRepo["AssessmentAnswerRepository"]
        ResultRepo["AssessmentResultRepository"]
        ModuleRepo["LearningModuleRepository"]
        ProgressRepo["ModuleProgressRepository"]
        IncidentRepo["IncidentReportRepository"]
        ScenarioRepo["PhishingScenarioRepository & PhishingRedFlagRepository"]
        SimResultRepo["SimulationResultRepository"]
    end

    subgraph Database Tier ["Database Tier (MySQL 8)"]
        MySQL[("MySQL 8 Database<br/>`privacy_awareness_db`<br/>InnoDB Engine / UTF-8MB4")]
    end

    UI_User --> JS_App
    UI_Admin --> JS_App
    JS_App -->|HTTP Fetch + XSRF-TOKEN| SecContext
    SecContext --> StatusFilter
    StatusFilter --> CSRF
    CSRF --> LogoutFilter
    LogoutFilter --> AuthFilter

    AuthFilter --> AuthController
    AuthFilter --> DashboardController
    AuthFilter --> QuizController
    AuthFilter --> LearningController
    AuthFilter --> IncidentController
    AuthFilter --> SimulatorController
    AuthFilter --> AdminController

    AuthController --> UserService
    DashboardController --> DashboardService
    QuizController --> QuizService
    LearningController --> LearningService
    IncidentController --> IncidentService
    SimulatorController --> PhishingSimulatorService
    AdminController --> UserService
    AdminController --> DashboardService
    AdminController --> IncidentService
    AdminController --> QuizRepo
    AdminController --> AnswerRepo
    AdminController --> ModuleRepo
    AdminController --> ProgressRepo

    UserService --> UserRepo
    DashboardService --> UserRepo
    DashboardService --> ResultRepo
    DashboardService --> IncidentRepo
    DashboardService --> ModuleRepo
    DashboardService --> ProgressRepo
    DashboardService --> SimResultRepo
    QuizService --> UserRepo
    QuizService --> QuizRepo
    QuizService --> ResultRepo
    LearningService --> ModuleRepo
    LearningService --> ProgressRepo
    LearningService --> UserRepo
    IncidentService --> IncidentRepo
    IncidentService --> UserRepo
    PhishingSimulatorService --> ScenarioRepo
    PhishingSimulatorService --> SimResultRepo
    PhishingSimulatorService --> UserRepo

    UserRepo --> MySQL
    QuizRepo --> MySQL
    AnswerRepo --> MySQL
    ResultRepo --> MySQL
    ModuleRepo --> MySQL
    ProgressRepo --> MySQL
    IncidentRepo --> MySQL
    ScenarioRepo --> MySQL
    SimResultRepo --> MySQL
```

---

## 2. Request Lifecycle & Layer Breakdown

### 2.1 Client Tier (Browser)
- **Structure:** Static HTML5 pages styled with custom, dark-theme CSS (`style.css`).
- **Communication:** `app.js` issues asynchronous `fetch()` calls exchanging JSON payloads with the backend.
- **CSRF Token Handling:** On page load, `app.js` extracts the `XSRF-TOKEN` cookie issued by Spring Security and includes it as the `X-XSRF-TOKEN` header on every `POST`, `PUT`, `PATCH`, and `DELETE` request.

### 2.2 Security & Gateway Tier (Spring Security 6)
Every incoming HTTP request passes through the configured `SecurityFilterChain`:
1. **`SecurityContextHolderFilter`**: Restores the user's `Authentication` object from the current `HttpSession`.
2. **`UserAccountStatusFilter`**: Queries MySQL to verify that the authenticated account has not been deleted or disabled (`enabled == false`). If the account is deactivated or missing, it immediately invalidates the session and returns HTTP 401 Unauthorized.
3. **`CsrfFilter`**: Validates the `X-XSRF-TOKEN` header on mutating requests.
4. **`AuthorizationFilter`**: Enforces URL-level permissions:
   - `/api/auth/register` and `/api/auth/login`: Publicly accessible.
   - `/api/admin/**` and `admin.html`: Strictly requires `ROLE_ADMIN`.
   - `/api/**`: Requires an authenticated session (`ROLE_USER` or `ROLE_ADMIN`).

### 2.3 Application & Domain Service Tier (Spring Boot 3.2.5)
- **Controllers:** Map REST endpoints, deserialize incoming JSON into validated Request DTOs, invoke business services, and return Response DTOs.
- **Services:** Contain core domain logic:
  - **`UserService`**: Handles user registration, profile retrieval, password modification with BCrypt verification, and user management.
  - **`QuizService`**: Retrieves active diagnostic questions, calculates assessment points and percentage scores, determines risk tiers, and generates targeted study recommendations.
  - **`DashboardService`**: Aggregates real-time relational counts across assessments, incidents, learning progress, and phishing simulations for user and administrative views.
  - **`IncidentService`**: Manages incident report submissions, status workflows, CSV exports with spreadsheet formula injection protection, and OpenPDF report generation.
  - **`PhishingSimulatorService`**: Evaluates student selections against known attack red flags and distractors.
  - **`LearningService`**: Tracks user completion state per educational module.

### 2.4 Persistence Tier (Spring Data JPA / Hibernate)
- Entities map directly to MySQL tables with relational annotations (`@OneToMany`, `@ManyToOne`, `@JoinColumn`).
- Repositories extend `JpaRepository`, providing query methods and transaction management (`@Transactional`).

### 2.5 Data Tier (MySQL 8)
- Stores all data in `privacy_awareness_db` using the InnoDB engine with `utf8mb4_unicode_ci` encoding.
- Foreign key constraints enforce relational integrity.
