# PROJECT REPORT

## Privacy Guard — Social Media Privacy Awareness and Security Management System

**Academic Discipline:** Computer Science & Engineering / Information Security  
**Application Type:** Full-Stack Web Application  
**Technology Stack:** Java 21 · Spring Boot 3.2.5 · MySQL 8 · Spring Security · JPA/Hibernate · OpenPDF · HTML5/CSS3/JavaScript  

---

## 1. Abstract

Online social networks expose everyday users to targeted phishing, account impersonation, credential harvesting, and accidental oversharing. While technical countermeasures such as firewalls and spam filters catch many automated attacks, human vulnerability remains the primary point of failure. Traditional cybersecurity training is frequently theoretical, compliance-focused, and disconnected from personal social media habits.

This project implements **Privacy Guard**, an educational web application designed to evaluate, educate, and support social media users. The platform provides an interactive 15-question privacy assessment with dynamic recommendations, a realistic phishing simulator with hands-on red flag identification, categorized learning modules, a client-side password strength evaluator, and a user incident reporting center. An administrative portal enables user management, incident triage, educational content administration, and report exports to CSV and PDF. 

The application is built using Java 21, Spring Boot 3.2.5, Spring Security for session-based authentication and CSRF defense, Hibernate with MySQL 8 for data persistence, OpenPDF for document generation, and a responsive vanilla HTML/CSS/JavaScript frontend with a dark theme. The system has been validated through 15 unit and repository tests on an in-memory database and an automated 40-test integration suite executed against a live MySQL database.

---

## 2. Introduction & Background

Social media platforms have transformed personal communication, professional networking, and content distribution. However, the open nature of social networks also makes them ideal environments for social engineering. Attackers gather personal details from public posts, craft convincing messages impersonating friends or service providers, and exploit common behavioral tendencies such as urgency, curiosity, and authority.

Most computer users receive little to no practical privacy education. Where training is mandated—such as in corporate or university settings—it typically consists of annual video presentations or generic multiple-choice quizzes that do not offer targeted feedback. Users rarely have a safe environment to practice identifying phishing lures or testing password resiliency without consequence.

Privacy Guard was developed as an educational project to bridge this gap. By combining diagnostic assessment with targeted learning, realistic simulations, and reporting tools, the platform provides an interactive environment where users can evaluate their habits and learn defensive practices.

---

## 3. Problem Statement

Three primary issues characterize common social media security practices:

1. **Lack of Personalized Diagnostics:** Users are generally unaware of their specific privacy weaknesses. A user might maintain strong passwords but routinely fall for urgent direct-message phishing links or share sensitive location data.
2. **Absence of Practical Identification Practice:** Reading guidelines on phishing is substantially less effective than inspecting an actual suspicious message, evaluating sender addresses, and spotting subtle discrepancies.
3. **Missing Incident Reporting and Guidance Channels:** When a non-technical user experiences an incident—such as a suspicious message or an attempted compromise—they frequently do not know how to document it or what immediate remediation steps to take.

---

## 4. Project Objectives

The project aims to achieve the following specific goals:

1. Build a secure, multi-user web application with distinct `USER` and `ADMIN` roles.
2. Develop an interactive privacy assessment quiz that scores user responses across six key privacy categories and delivers personalized recommendations.
3. Provide six categorized educational learning modules with reading-time estimates and completion tracking.
4. Create a phishing simulation laboratory where users analyze realistic email, SMS, and direct message lures to identify specific red flags.
5. Provide a client-side password strength checker that calculates entropy, identifies common dictionary patterns, and estimates crack times.
6. Implement an incident reporting portal allowing users to report suspicious events and track administrative review status.
7. Develop an administrative management console for system analytics, user access governance, incident triage, and educational content administration.
8. Support secure reporting with incident exports to RFC 4180 CSV (shielded against formula injection) and formatted PDF.
9. Enforce robust security practices: BCrypt password hashing, CSRF tokens, active session invalidation filters, and relational deletion safeguards.
10. Validate the implementation through automated unit and live database integration testing.

---

## 5. Scope & Limitations

### 5.1 In-Scope Features
- User registration, login, logout, and authenticated password changes.
- Immediate session termination when an account is disabled or deleted.
- 15 diagnostic quiz questions across six threat categories with scoring, risk tiers, and recommendations.
- Six structured learning modules with completion persistence.
- Ten phishing scenarios across three communication channels (email, SMS, direct message) with red flag evaluation.
- Client-side password strength tool.
- Incident report submission and administrator review workflow (`OPEN`, `UNDER_REVIEW`, `RESOLVED`, `DISMISSED`).
- Incident report exports in CSV and PDF formats.
- Full administrative CRUD operations for quiz questions and learning modules with relational deletion protections.
- Real-time database analytics on user and administrator dashboards.

### 5.2 Out-of-Scope Limitations
- **No Third-Party Social Media Integration:** The application does not connect to live APIs (e.g., Meta Graph API, X API) or collect actual social media account credentials. All questions, scenarios, and demonstrations are self-contained.
- **In-Memory HTTP Sessions:** Active sessions are maintained in server memory (`HttpSession`). Multi-node horizontal scaling would require external session stores such as Redis.
- **Internal Notification Only:** Incident status updates and password changes do not dispatch automated emails via SMTP. All notifications occur within the application interface.

---

## 6. Existing Systems vs. Proposed System

| Dimension | Typical Existing Training | Privacy Guard (Proposed System) |
|---|---|---|
| **Format** | Static slides, long videos, or compliance questionnaires | Interactive quiz, hands-on phishing lab, and categorized modules |
| **Feedback** | Pass/Fail binary score with no category breakdown | Granular accuracy percentage per category and targeted study advice |
| **Phishing Practice** | Rare, often limited to surprise simulated IT emails | Interactive simulator with 10 multi-channel scenarios and red flag identification |
| **Incident Management**| Disconnected external ticketing system or IT inbox | Integrated reporting portal with status tracking and administrative notes |
| **Content Administration**| Fixed curriculum requiring database admin changes | Web-based administrative CRUD interface with deletion safeguards |
| **Export Capabilities** | Basic text logs or manual copy-pasting | Filterable exports to RFC 4180 CSV (formula-shielded) and formatted PDF |

---

## 7. Requirements Analysis

### 7.1 Functional Requirements

| ID | Requirement | Implementation Details |
|---|---|---|
| **FR-01** | User Registration | Validates unique username, email, and password complexity before creating user. |
| **FR-02** | User Authentication | Session-based login issuing `JSESSIONID`; validates password hash using BCrypt. |
| **FR-03** | Password Change | Authenticated users can update passwords by verifying their current password. |
| **FR-04** | Session Invalidation | Deactivating or deleting an account terminates active sessions on the next request. |
| **FR-05** | Diagnostic Quiz | Displays active questions across 6 categories; evaluates answers and stores score. |
| **FR-06** | Risk Scoring | Calculates percentage score and assigns risk tier (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). |
| **FR-07** | Learning Modules | Displays categorized reading modules; users mark completion to track progress. |
| **FR-08** | Phishing Simulator | Presents scenarios; users identify red flags and receive immediate scoring. |
| **FR-09** | Password Checker | Evaluates password length, character diversity, patterns, entropy, and crack time. |
| **FR-10** | Incident Reporting | Users submit reports; administrators update status and record resolution notes. |
| **FR-11** | Report Exports | Administrators can export filtered incident reports to CSV and PDF. |
| **FR-12** | Admin Question CRUD | Administrators create, edit, toggle, or delete quiz questions with deletion safeguards. |
| **FR-13** | Admin Module CRUD | Administrators create, edit, toggle, or delete learning modules with deletion safeguards. |
| **FR-14** | Admin Analytics | Real-time calculation of active users, average scores, and category accuracy. |

### 7.2 Non-Functional Requirements

- **Security:** Passwords hashed with BCrypt (cost factor 12). Mutating requests protected with CSRF double-submit cookies. Role-based authorization enforced server-side.
- **Data Integrity:** Foreign key constraints prevent orphaned records. Questions and modules referenced in user history cannot be deleted.
- **Performance:** Database indexing on foreign keys and unique fields. Connection pooling via HikariCP (max pool size 10).
- **Usability:** Responsive, dark-themed user interface styled with vanilla CSS. Intuitive modal dialogs and toast notifications.
- **Maintainability:** Standard layered architecture separating controllers, services, repositories, and data transfer objects.

---

## 8. Technology Selection & Justification

1. **Java 21:** Modern long-term support (LTS) release providing strong type safety, robust standard libraries, and high performance.
2. **Spring Boot 3.2.5:** Mature framework offering dependency injection, embedded Tomcat server, declarative transaction management, and production-ready configuration.
3. **Spring Security 6:** Industry-standard security framework providing session management, CSRF protection, and method-level access controls.
4. **MySQL 8.0:** Widely adopted relational database with ACID compliance, robust InnoDB transactional support, and full UTF-8 Unicode capabilities (`utf8mb4`).
5. **Spring Data JPA / Hibernate 6:** Reduces boilerplate data access code, provides object-relational mapping, and simplifies schema updates.
6. **OpenPDF (1.3.40):** Lightweight open-source LGPL library for programmatic PDF document generation, used for administrative reporting.
7. **Vanilla HTML5, CSS3, and JavaScript:** Chosen deliberately to eliminate heavyweight frontend frameworks (React, Angular), keeping the application fast, lightweight, and easy to run without Node.js dependencies.

---

## 9. System Design & Architecture

The application implements a layered architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                 Client Tier (Web Browser)                   │
│   HTML5 · Dark-Theme Vanilla CSS · Asynchronous Fetch API   │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP / JSON / XSRF-TOKEN
┌──────────────────────────────▼──────────────────────────────┐
│                  Spring Security Filter Chain               │
│   SecurityContextHolderFilter → UserAccountStatusFilter      │
│   → CsrfFilter → LogoutFilter → AuthorizationFilter         │
└──────────────────────────────┬──────────────────────────────┘
                               │ DispatcherServlet
┌──────────────────────────────▼──────────────────────────────┐
│                    REST Controller Layer                    │
│   AuthController · DashboardController · QuizController     │
│   LearningController · IncidentController · AdminController │
└──────────────────────────────┬──────────────────────────────┘
                               │ Service Method Calls
┌──────────────────────────────▼──────────────────────────────┐
│                    Domain Service Layer                     │
│   UserService · DashboardService · QuizService              │
│   LearningService · IncidentService · PhishingSimulator     │
└──────────────────────────────┬──────────────────────────────┘
                               │ JPA Repositories
┌──────────────────────────────▼──────────────────────────────┐
│                 Persistence Layer (Hibernate)               │
│   11 Repositories · HikariCP Connection Pool                │
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC
┌──────────────────────────────▼──────────────────────────────┐
│                  Data Tier (MySQL 8.0)                      │
│   privacy_awareness_db · utf8mb4_unicode_ci                 │
└─────────────────────────────────────────────────────────────┘
```

For detailed architectural flow specifications, see [docs/SYSTEM_ARCHITECTURE.md](./docs/SYSTEM_ARCHITECTURE.md).

---

## 10. Database Design

The database schema consists of 11 normalized tables in `privacy_awareness_db`:

1. `users`: Stores user identity, BCrypt password hashes, roles (`USER`/`ADMIN`), and account status.
2. `quiz_questions`: Assessment question prompts, threat categories, points, and active flags.
3. `quiz_options`: Multiple-choice answers linked to questions.
4. `assessment_results`: Overall assessment submissions, total scores, percentages, and risk tiers.
5. `assessment_answers`: Item-level answers recording user choices, correctness, and points awarded.
6. `learning_modules`: Educational content, estimated reading durations, and catalog display orders.
7. `module_progress`: Per-user module completion tracking with unique constraint `(user_id, module_id)`.
8. `incident_reports`: User-filed incident reports, category types, statuses, and administrative notes.
9. `phishing_scenarios`: Simulated attack lures with channel types, difficulty levels, and debrief text.
10. `phishing_red_flags`: Specific warning indicators and distractors associated with scenarios.
11. `simulation_results`: User simulation outcomes, scores, and pass/fail evaluations.

For the complete visual schema and field types, refer to [docs/ER_DIAGRAM.md](./docs/ER_DIAGRAM.md) and [DATABASE.md](./DATABASE.md).

---

## 11. Main Workflows

### 11.1 User Assessment & Remediation Workflow
1. User logs in and accesses the **Privacy Quiz**.
2. The server delivers active questions with option text (concealing correct answer flags).
3. User selects answers and submits the questionnaire.
4. The server validates answers, calculates earned points, assigns a risk tier, generates category-level recommendations, and persists the record.
5. The result screen immediately displays the percentage score, risk tier, correct answers, and detailed explanations.
6. The user dashboard updates its category breakdown and highlights unfinished learning modules matching weak categories.

### 11.2 Phishing Simulation Workflow
1. User navigates to the **Phishing Simulator** and selects a scenario (e.g., banking alert email or streaming suspension SMS).
2. The interface renders the realistic lure.
3. User checks suspected red flags (e.g., mismatched sender domain, artificial urgency, deceptive hyperlink).
4. On submission, the server computes detection accuracy, evaluates whether distractors were chosen, and returns an educational debrief.

### 11.3 Incident Reporting & Administrator Triage
1. User files an incident report specifying the title, description, and incident type.
2. The report enters the administrator incident queue with status `OPEN`.
3. An administrator inspects the report in the **Admin Panel**, updates the status to `UNDER_REVIEW`, and subsequently to `RESOLVED` with guidance notes.
4. The user views the updated status and resolution notes on their dashboard.
5. The administrator can export all or filtered incidents to CSV or PDF for record-keeping.

---

## 12. Quiz Scoring Model & Interpretation

### 12.1 Mathematical Formula
Each question carries a designated point value (default: 10 points). When a user completes an assessment:

$$\text{Total Score} = \sum_{i=1}^{N} \text{Points earned on Question } i$$

$$\text{Max Possible Score} = \sum_{i=1}^{N} \text{Points available on Question } i$$

$$\text{Percentage Score } (P) = \left( \frac{\text{Total Score}}{\text{Max Possible Score}} \right) \times 100$$

### 12.2 Risk Tier Classifications

| Percentage Range | Risk Level | Interpretation | Remedial Action |
|:---:|:---:|---|---|
| **80% – 100%** | `LOW` | Strong understanding of social media privacy principles and threat indicators. | Maintain awareness; complete remaining learning modules. |
| **60% – 79%** | `MEDIUM` | Moderate awareness; user identifies common risks but misses subtle indicators. | Review modules corresponding to incorrectly answered categories. |
| **40% – 59%** | `HIGH` | Vulnerable habits detected; user is susceptible to common phishing and oversharing risks. | Recommended to review all curriculum modules and update social media privacy settings. |
| **0% – 39%** | `CRITICAL` | Severe privacy knowledge gaps; high risk of credential harvesting or account compromise. | Immediate review of essential privacy modules and credential hygiene guidelines. |

### 12.3 Pedagogical Disclaimer
*The Privacy Guard Assessment is an educational benchmarking exercise designed to identify knowledge gaps and promote defensive habits. It does not measure the statistical probability of being compromised, nor does it replace professional security audits or certified penetration testing.*

---

## 13. Security Architecture & Defenses

1. **Password Protection:** Stored using BCrypt with a cost factor of 12 (`BCryptPasswordEncoder(12)`). Plaintext passwords and BCrypt hashes are strictly omitted from API responses via DTO separation.
2. **Immediate Session Revocation (`UserAccountStatusFilter`):** Unlike standard session setups that continue honoring active cookies after account changes, this custom filter queries MySQL on every authenticated request. If an account is disabled or deleted, the session is invalidated immediately, and the caller receives HTTP 401.
3. **CSRF Protection:** Implemented using Spring Security's `CookieCsrfTokenRepository` with double-submit cookie validation on all mutating HTTP methods.
4. **Role-Based Authorization:** Server-side access control using `@PreAuthorize("hasRole('ADMIN')")` and `SecurityFilterChain` patterns. Normal users cannot access `/admin.html` or `/api/admin/**`.
5. **Horizontal Tenant Isolation:** Users can only retrieve their own incident reports and quiz attempts. Requesting another user's incident report ID returns HTTP 404 to prevent resource enumeration.
6. **Spreadsheet Formula Injection Defense:** When exporting incidents to CSV, fields starting with dangerous formula characters (`=`, `+`, `-`, `@`, `\t`, `\r`) are prefixed with an apostrophe (`'`) to neutralize command execution in spreadsheet applications.
7. **Relational Deletion Safeguards:** Hard deletion of questions or modules linked to historical user records is blocked with descriptive HTTP 400 errors, prompting administrators to use deactivation toggles instead.
8. **Error Sanitization:** Stack traces and internal package names are disabled in production configuration (`server.error.include-stacktrace=never`).

---

## 14. Testing Methodology & Verified Results

### 14.1 Automated Maven Unit & Slice Tests (In-Memory H2)
Run via `mvn clean test`:
- **`AdminAnalyticsTests` (2 tests):** Verifies metric calculation, missed question ranking, and category breakdown.
- **`AdminUserManagementTests` (8 tests):** Verifies user creation, role modification, status toggling, deletion cascades, and safeguard protections.
- **`DashboardAnalyticsTests` (2 tests):** Verifies user dashboard metric aggregation and category accuracy calculations.
- **`PhishingSimulatorTests` (2 tests):** Verifies scenario retrieval, red flag scoring, and simulation result persistence.
- **`PrivacyAwarenessApplicationTests` (1 test):** Verifies application context loading and bean wiring.
- **Outcome:** **15 Tests Executed — 15 Passed, 0 Failed, 0 Skipped.**

### 14.2 Live End-to-End Regression Test Suite (MySQL 8)
Run via `. .\Set-Env.ps1; .\test-runner.ps1` against the running server connected to MySQL:
- Verifies CSRF generation, dual-user registration, session establishment, and profile inspection.
- Verifies quiz retrieval, submission evaluation, score calculation, and history retrieval.
- Verifies learning module completion and progress tracking.
- Verifies incident report creation, user report isolation, and status updates.
- Verifies RBAC defenses (regular users blocked with HTTP 403 on `/api/admin/**` and `admin.html`).
- Verifies authenticated password changes and immediate rejection of old passwords.
- Verifies `UserAccountStatusFilter` terminating sessions of disabled accounts.
- Verifies CSV exports (headers, MIME type, formula escaping) and PDF exports (valid `%PDF` header bytes).
- Verifies full Question CRUD and Module CRUD with relational deletion safeguards.
- Verifies database record persistence and 100% BCrypt hashing in MySQL.
- **Outcome:** **40 Tests Executed — 40 Passed, 0 Failed (100% Pass Rate).**

For the full test execution log and environment details, see [TEST_REPORT.md](./TEST_REPORT.md).

---

## 15. Limitations & Future Improvements

1. **Email Integration:** Implement Spring Mail integration to notify users when an incident report is updated or resolved by an administrator.
2. **Stateless JWT Option:** Add JSON Web Token (JWT) authentication support alongside session cookies to enable future mobile client integrations.
3. **Category Filtering in Quizzes:** Allow users to take targeted mini-quizzes focused on a single topic (e.g., a 5-question test solely on password safety).
4. **User Assessment PDF Reports:** Allow users to download a personalized assessment certificate and summary report in PDF format.
5. **Containerization:** Provide a `docker-compose.yml` file to package the Spring Boot backend and MySQL database into containerized services for one-command deployment.

---

## 16. Conclusion

Privacy Guard demonstrates a complete, secure, full-stack implementation of an educational cybersecurity platform. The application successfully addresses the human factor in social media security through diagnostic assessment, practical phishing simulation, and incident management. By implementing robust security controls—including BCrypt encryption, CSRF protection, active session invalidation, formula injection shielding, and relational deletion safeguards—the project fulfills its academic and technical objectives.
