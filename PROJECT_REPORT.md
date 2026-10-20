# PROJECT REPORT

## Privacy Guard — Social Media Privacy Awareness and Security Management System

**Course:** Information Security / Advanced Web Application Engineering  
**Technology Stack:** Java 21 · Spring Boot 3.2.5 · MySQL 8 · Spring Security · JPA/Hibernate · OpenPDF · HTML5/Vanilla CSS/JavaScript  

---

## Abstract

This project presents **Privacy Guard** — an enterprise-grade educational web application designed to evaluate, educate, and empower individuals against modern social media cyber threats. The platform addresses critical security gaps including phishing simulations, credential vulnerabilities, social engineering, and algorithmic profile scraping. Users engage in interactive privacy awareness assessments, explore structured learning modules, report active security incidents, and test passwords against real-world breach heuristics. 

The application is engineered with Java 21, Spring Boot 3.2.5, Spring Security 6, Spring Data JPA/Hibernate, MySQL 8 with connection pooling, OpenPDF for automated document generation, and a dark-mode frontend built with modern vanilla CSS and JavaScript. This report documents the system architecture, authentication and authorization safeguards, real-time database analytics engine, administrative management interfaces, incident export capabilities, and automated verification suites.

---

## 1. Problem Statement

Social media platforms are the primary vector for contemporary social engineering, spear-phishing, credential stuffing, and surveillance-based identity theft. Despite the prevalence of these attacks:
1. **Abstract Security Training:** Traditional cybersecurity training is dry, compliance-driven, and detached from daily consumer habits.
2. **Lack of Continuous Feedback:** Users rarely receive actionable feedback regarding their specific behavioral weaknesses.
3. **Absence of Unified Response:** Non-technical users lack simple, secure mechanisms to report active phishing threats and track containment progress.
4. **Administrative Vulnerabilities:** Many academic and enterprise prototypes suffer from flawed authentication lifecycles — such as credentials desynchronizing between `.env` configuration and relational databases, persistent sessions surviving account deactivation, or unshielded CSV exports vulnerable to spreadsheet formula injection.

Privacy Guard directly resolves these challenges by combining gamified assessment, behavioral feedback, interactive simulations, and hardened operational controls.

---

## 2. Core Objectives

1. **Hardened Role-Based Security:** Deliver granular role-based access control (RBAC) separating regular users (`USER`) from administrative staff (`ADMIN`) with active session invalidation filters (`UserAccountStatusFilter`).
2. **Runtime Credential Management:** Implement self-service password modification with BCrypt hashing, eliminating divergence between environment bootstrap variables and persistent database records.
3. **Real MySQL-Backed Analytics:** Replace mock data with real-time relational aggregation across user assessments, incident lifecycles, phishing simulations, and module completions.
4. **Secure Incident Management & Export:** Provide end-to-end incident reporting, review workflows, and multi-format reporting (CSV with formula injection shielding and formatted OpenPDF reports).
5. **Full Educational Content Administration:** Provide full CRUD capabilities for assessment questions and learning modules with relational integrity safeguards preventing orphan records.
6. **Transparent Pedagogical Scoring:** Formalize a mathematically sound, category-weighted scoring model with distinct risk classifications and educational disclaimers.
7. **Empirical Verification:** Validate software stability with automated unit tests and an end-to-end regression test suite executing against a live MySQL 8 database.

---

## 3. System Architecture & Component Design

The platform adheres to a layered enterprise architecture strictly enforcing separation of concerns:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Presentation Tier (Browser)                        │
│   HTML5 · Dark-Themed Glassmorphism CSS · Vanilla JavaScript (Fetch API)    │
│   dashboard.html · admin.html · quiz.html · modules.html · incidents.html  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ HTTPS / REST JSON / XSRF-TOKEN
┌──────────────────────────────────────▼──────────────────────────────────────┐
│                      Spring Boot 3.2.5 Application Tier                     │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                     Security & Filter Chain Pipeline                  │  │
│  │  SecurityContextHolderFilter → UserAccountStatusFilter → CsrfFilter   │  │
│  │  → LogoutFilter → SessionManagementFilter → AuthorizationFilter       │  │
│  └───────────────────────────────────┬───────────────────────────────────┘  │
│                                      │                                      │
│  ┌──────────────────────────┐        │        ┌──────────────────────────┐  │
│  │       Controllers        │◄───────┴───────►│    Services (Domain)     │  │
│  │ AuthController           │                 │ UserService              │  │
│  │ DashboardController      │                 │ DashboardService         │  │
│  │ QuizController           │                 │ QuizService              │  │
│  │ LearningController       │                 │ LearningService          │  │
│  │ IncidentController       │                 │ IncidentService          │  │
│  │ PhishingController       │                 │ PhishingService          │  │
│  │ AdminController          │                 │ PasswordCheckerService   │  │
│  └────────────┬─────────────┘                 └────────────┬─────────────┘  │
│               │                                            │                │
│  ┌────────────▼─────────────┐                 ┌────────────▼─────────────┐  │
│  │  Data Transfer Objects   │                 │     OpenPDF / Export     │  │
│  │ Requests / Responses     │                 │ OpenPdfExportService     │  │
│  └──────────────────────────┘                 └──────────────────────────┘  │
│                                      │                                      │
│  ┌───────────────────────────────────▼───────────────────────────────────┐  │
│  │              Spring Data JPA Persistence Tier (Repositories)          │  │
│  │ UserRepository · AssessmentResultRepo · AssessmentAnswerRepo          │  │
│  │ QuizQuestionRepo · QuizOptionRepo · LearningModuleRepo                │  │
│  │ ModuleProgressRepo · IncidentReportRepo · SimulationResultRepo        │  │
│  └───────────────────────────────────┬───────────────────────────────────┘  │
└──────────────────────────────────────┼──────────────────────────────────────┘
                                       │ HikariCP / JDBC
┌──────────────────────────────────────▼──────────────────────────────────────┐
│                           MySQL 8.0 Enterprise Engine                       │
│    privacy_awareness_db · utf8mb4_unicode_ci · Relational Foreign Keys     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Engineering Solutions & Major Implementations

### 4.1. Authentication Architecture & Root Cause Remediations

#### The Divergence Problem
Prior to remediation, changing `ADMIN_PASSWORD` in `.env` resulted in authentication failures because `DataSeeder` only checked `userRepository.existsByUsername(adminUsername)`. Existing database accounts retained their historical BCrypt hash. Furthermore, deleting or disabling a user in MySQL did not terminate active `HttpSession` state, and restarting the application resurrected deleted admins.

#### The Architectural Solution
1. **Separation of Bootstrap vs. Runtime Credentials:**
   - `.env` and `DataSeeder` are strictly designated as initial provisioning mechanisms. The admin account is seeded only when the `users` table is completely unpopulated (`userRepository.count() == 0`).
   - For disaster recovery, an explicit environment flag `ADMIN_RECOVER=true` permits administrators to force-reset the database credentials to the `.env` value upon server restart.
2. **Authenticated Self-Service Password Change:**
   - Added `POST /api/auth/change-password` supported by `ChangePasswordRequest`.
   - Requires verification of the caller's existing password via `passwordEncoder.matches()` before accepting and encoding the new password.
   - Enforces password complexity (minimum 8 characters, uppercase, lowercase, numbers, and symbols).
3. **Immediate Active Session Invalidation (`UserAccountStatusFilter`):**
   - Registered immediately after `SecurityContextHolderFilter` in the Spring Security filter chain.
   - For every incoming authenticated request, the filter queries MySQL via `userRepository.findByUsername(username)`.
   - If the account is absent (deleted) or `enabled == false` (deactivated), the filter invokes `request.getSession().invalidate()`, clears `SecurityContextHolder`, and returns HTTP 401 Unauthorized with a structured JSON error body.

---

### 4.2. Relational Analytics & Empty States Engine

The analytics engine was migrated from hardcoded mock metrics to 100% MySQL-backed relational aggregations:

- **User Dashboard (`GET /api/dashboard`):**
  - **Assessments Completed & Latest Risk:** Derived directly from `assessment_results` ordered by `completed_at DESC`.
  - **Incident Status Breakdown:** Aggregates counts grouped by status (`OPEN`, `UNDER_REVIEW`, `RESOLVED`, `CLOSED`) for the logged-in user.
  - **Simulation Mastery:** Queries `simulation_results` to calculate total phishing simulation attempts, correct identifications, and real-time pass percentage.
  - **Learning Progress:** Calculates completed modules against total active learning modules.
  - **Tailored Guidance:** Evaluates weak categories from recent assessment answers and recommends specific incomplete learning modules.
  - **Empty State UX:** When a new user has zero assessments, incidents, or simulations, the dashboard presents actionable guidance prompts rather than broken charts or null indicators.

- **Admin Analytics (`GET /api/admin/summary`):**
  - Live counts of total registered users, active users, and deactivated accounts.
  - Overall assessment counts, system-wide average quiz percentage, and category-level accuracy breakdowns.
  - Phishing simulation metrics across the entire user base.
  - Incident queue status distribution.

---

### 4.3. Incident Management & Secure Export Subsystem

#### CSV Export (`GET /api/admin/incidents/export/csv`)
- **Spreadsheet Formula Injection Defense (CSV Injection):** Cells beginning with dangerous formula characters (`=`, `+`, `-`, `@`, `\t`, `\r`) are automatically escaped by prefixing an apostrophe (`'`).
- **Standard Formatting:** Complies with RFC 4180; values with commas, quotes, or newlines are quoted, with internal quotes escaped as `""`.
- **UTF-8 Byte Order Mark (BOM):** Emits `\uFEFF` so Microsoft Excel automatically parses UTF-8 characters without encoding artifacts.

#### PDF Export (`GET /api/admin/incidents/export/pdf`)
- Powered by `com.github.librepdf:openpdf:1.3.40`.
- Generates a formal, landscape A4 PDF report with corporate security metadata, summary tables (total records, timestamp, filter parameters), and detailed incident rows with styled status pills.

---

### 4.4. Content Administration (Questions & Modules) with Relational Safeguards

Administrators have full CRUD control over the educational curriculum:

1. **Quiz-Question Management:**
   - `GET /api/admin/questions/{id}`: View question details with options.
   - `POST /api/admin/questions`: Create question with multiple options and answer explanations.
   - `PUT /api/admin/questions/{id}`: Update question text, category, points, explanation, and options.
   - `PATCH /api/admin/questions/{id}/toggle`: Activate/deactivate visibility in student quizzes.
   - `DELETE /api/admin/questions/{id}`: **Relational Safeguard:** If a question has historical student answers in `assessment_answers`, hard deletion is rejected with HTTP 400 Bad Request, instructing the administrator to deactivate the question instead to preserve historical audit integrity.

2. **Learning-Module Management:**
   - `GET /api/admin/modules/{id}`: View full educational content.
   - `POST /api/admin/modules`: Author new learning module.
   - `PUT /api/admin/modules/{id}`: Update module metadata, estimated minutes, and body content.
   - `PATCH /api/admin/modules/{id}/toggle`: Toggle active curriculum state.
   - `DELETE /api/admin/modules/{id}`: **Relational Safeguard:** If student progress exists in `module_progress`, deletion is rejected with HTTP 400 Bad Request.

---

## 5. Transparent Pedagogical Scoring Model

### 5.1. Mathematical Formula

The privacy assessment score is computed dynamically based on the active questions answered:

$$\text{Total Score} = \sum_{i=1}^{N} \text{Points earned on Question } i$$

$$\text{Max Possible Score} = \sum_{i=1}^{N} \text{Max points for Question } i$$

$$\text{Percentage Score } (P) = \left( \frac{\text{Total Score}}{\text{Max Possible Score}} \right) \times 100$$

### 5.2. Risk Tier Classification

| Percentage Range | Risk Level | Designation | Pedagogical Action Plan |
|:----------------:|:----------:|:-----------:|:------------------------|
| **80% – 100%** | `LOW` | **Exemplary Awareness** | User demonstrates advanced digital hygiene. Recommended for specialized training modules (e.g., API security, advanced credential rotation). |
| **60% – 79%** | `MEDIUM` | **Moderate Awareness** | User understands basic tenets but exhibits vulnerabilities in social engineering or device sharing. Recommended to review targeted modules. |
| **40% – 59%** | `HIGH` | **Elevated Vulnerability** | Critical gaps in credential management and phishing detection. High susceptibility to account takeover. Immediate module completion required. |
| **0% – 39%** | `CRITICAL` | **Critical Security Deficit** | Severe vulnerability to elementary attack vectors. System flags user for remedial privacy education and active incident monitoring. |

### 5.3. Pedagogical Disclaimer
*The Privacy Guard Assessment is an educational benchmarking instrument designed to identify behavioral tendencies and knowledge gaps regarding digital privacy. It does not constitute a formal security audit, certified penetration test, or guarantee against sophisticated cyber attacks.*

---

## 6. Verification & Quality Assurance

### 6.1. Maven Unit & Integration Tests
- **Framework:** JUnit 5, Spring Boot Test, H2 In-Memory Database.
- **Suite Execution:**
  ```bash
  mvn clean test
  ```
- **Results:**
  - `AdminAnalyticsTests`: 2 tests, 0 failures.
  - `DashboardAnalyticsTests`: 2 tests, 0 failures.
  - `PhishingSimulatorTests`: 2 tests, 0 failures.
  - `PrivacyAwarenessApplicationTests`: 1 test, 0 failures.
  - **Total:** 15 tests executed, **0 failures, 0 errors, 0 skipped**.

### 6.2. Live End-to-End Regression Suite (`test-runner.ps1`)
- **Environment:** Live Spring Boot application on port 8080 connecting to local MySQL 8.0 on port 3306.
- **Execution:**
  ```powershell
  . .\Set-Env.ps1
  .\test-runner.ps1
  ```
- **Results:** **40 Tests Executed — 40 Passed, 0 Failed (100% Pass Rate)**.
  - Validated CSRF handshake, dual-user registration, password omission from JSON.
  - Validated quiz scoring, recommendations, learning module progression, and incident reporting.
  - Validated RBAC isolation (non-admin blocked from admin endpoints; user data strictly isolated).
  - Validated runtime password change, incorrect password rejection, and old password invalidation.
  - Validated `UserAccountStatusFilter` immediate session invalidation on disabled users.
  - Validated CSV export (with formula shielding) and PDF export (magic `%PDF` verification).
  - Validated Question and Module CRUD with relational deletion safeguards.
  - Validated BCrypt hashing across 100% of MySQL database password fields.

---

## 7. Conclusion

Privacy Guard demonstrates a secure, production-grade implementation of an educational cybersecurity system. By resolving authentication desynchronization, enforcing real-time active session validation, backing all dashboard metrics with MySQL queries, shielding exports against injection, and instituting relational integrity safeguards, the platform meets the highest standards of web application engineering and information security.
