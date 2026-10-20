# Privacy Guard — System Testing and Verification Report

**Project Name:** Privacy Guard — Social Media Privacy Awareness and Security Management System  
**Test Date:** October 20, 2026  
**Operating System:** Windows 11 Enterprise (x64)  
**Runtime Environment:** Eclipse Adoptium OpenJDK 21.0.12  
**Framework & ORM:** Spring Boot 3.2.5, Spring Security 6.2.4, Hibernate 6.4.4  
**Database:** MySQL Community Server 8.0.46 (Local, InnoDB, `utf8mb4_unicode_ci`)  
**Build Tool:** Apache Maven 3.10.0  
**Overall Test Results:** 15 / 15 Maven Tests Passed | 40 / 40 Live MySQL Integration Tests Passed (100% Pass Rate)

---

## 1. Test Strategy Overview

The testing strategy for Privacy Guard incorporates two distinct tiers:

1. **Unit and Slice Tests (H2 In-Memory Database):** Executed during the Maven build phase using JUnit 5, Mockito, and Spring Boot Test (`@SpringBootTest`). An embedded in-memory H2 database isolates tests from local database state, ensuring repeatable tests for service calculations, user management logic, phishing scoring, and administrative analytics.
2. **End-to-End Regression Test Suite (Live MySQL 8):** Executed via `test-runner.ps1` against the running Spring Boot server on port 8080 and local MySQL database on port 3306. This suite tests actual HTTP network communication, session cookie handling, CSRF tokens, database persistence, session invalidation filters, and relational deletion safeguards.

---

## 2. Maven Unit & Slice Test Suite (H2)

- **Execution Command:**
  ```powershell
  mvn clean test
  ```
- **Execution Time:** ~1 minute 9 seconds
- **Database Used:** Embedded H2 in-memory (`jdbc:h2:mem:testdb`)

### Test Suite Breakdown:

| Test Class | Focus Area | Tests Executed | Passed | Failed |
|---|---|:---:|:---:|:---:|
| `AdminAnalyticsTests` | Metric calculations, missed question rankings, category percentages | 2 | 2 | 0 |
| `AdminUserManagementTests` | User creation, role updates, status toggling, deletion cascades, admin safeguards | 8 | 8 | 0 |
| `DashboardAnalyticsTests` | User dashboard metric aggregation and category accuracy calculations | 2 | 2 | 0 |
| `PhishingSimulatorTests` | Scenario retrieval, red flag identification scoring, simulation persistence | 2 | 2 | 0 |
| `PrivacyAwarenessApplicationTests` | Spring application context startup, bean injection, JPA entity mappings | 1 | 1 | 0 |
| **Total** | | **15** | **15** | **0** |

**Maven Build Output:**
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 3. Live MySQL End-to-End Regression Test Suite

- **Execution Command:**
  ```powershell
  . .\Set-Env.ps1
  .\test-runner.ps1
  ```
- **Server Under Test:** Spring Boot running at `http://localhost:8080`
- **Database Under Test:** MySQL Community Server 8.0 on port 3306 (`privacy_awareness_db`)
- **Execution Summary:** 40 Tests Executed — **40 Passed, 0 Failed (100% Success Rate)**

### Detailed Test Results Log:

| # | Test Name | Target Endpoint | Category | Verification Objective | Observed Result | Status |
|---|---|---|---|---|---|:---:|
| 1 | CSRF Token Generation | `GET /index.html` | Security | Receives `XSRF-TOKEN` cookie from server | Cookie received (`0b3e7e9c...`) | **PASS** |
| 2 | User Alpha Registration | `POST /api/auth/register` | Auth | Registers new user; password omitted from JSON | User ID created, password omitted | **PASS** |
| 3 | User Beta Registration | `POST /api/auth/register` | Auth | Registers distinct second user | User ID created (`user_beta_...`) | **PASS** |
| 4 | User Alpha Login | `POST /api/auth/login` | Auth | Authenticates with valid credentials; sets session | Session cookie `JSESSIONID` issued | **PASS** |
| 5 | Active User Profile | `GET /api/auth/me` | Auth | Returns authenticated user profile | Username and role `USER` returned | **PASS** |
| 6 | Quiz Questions Retrieval | `GET /api/quiz/questions` | Quiz | Retrieves active questions; hides correct flags | 15 questions returned, answers hidden | **PASS** |
| 7 | Quiz Submission & Evaluation | `POST /api/quiz/submit` | Quiz | Evaluates choices, calculates points and risk | Score calculated, risk assigned | **PASS** |
| 8 | User Dashboard Aggregation | `GET /api/dashboard` | Dashboard | Aggregates real MySQL counts and latest score | Assessments: 1, Score: 10/150 | **PASS** |
| 9 | Quiz History Retrieval | `GET /api/quiz/history` | Quiz | Retrieves chronological assessment records | 1 record returned with breakdown | **PASS** |
| 10 | Learning Modules Catalog | `GET /api/learning/modules` | Learning | Returns 6 modules with user completion status | 6 modules returned; completed: false | **PASS** |
| 11 | Module Completion | `POST /api/learning/modules/1/complete` | Learning | Marks module complete in `module_progress` | Message: `"Module marked as complete"` | **PASS** |
| 12 | Learning Progress Summary | `GET /api/learning/progress` | Learning | Increments completed module count | Completed: 1, Total: 6 | **PASS** |
| 13 | Incident Report Submission | `POST /api/incidents` | Incidents | Creates incident report with initial `OPEN` status | Report ID created with status `OPEN` | **PASS** |
| 14 | User Incident List | `GET /api/incidents` | Incidents | Returns reports submitted by active user | 1 report returned | **PASS** |
| 15 | RBAC: Admin Summary Access | `GET /api/admin/summary` | Security | Rejects non-admin user with HTTP 403 Forbidden | Blocked with HTTP 403 Forbidden | **PASS** |
| 16 | RBAC: Admin Users Directory | `GET /api/admin/users` | Security | Rejects non-admin user with HTTP 403 Forbidden | Blocked with HTTP 403 Forbidden | **PASS** |
| 17 | RBAC: Admin Page HTTP Defense| `GET /admin.html` | Security | Rejects non-admin user with HTTP 403 Forbidden | Blocked with HTTP 403 Forbidden | **PASS** |
| 18 | Cross-User Incident Isolation| `GET /api/incidents` | Privacy | User Beta cannot see User Alpha's incidents | User Beta receives 0 reports | **PASS** |
| 19 | Cross-User Direct Incident Access| `GET /api/incidents/{id}` | Privacy | Direct request to other user's report is blocked | Blocked with HTTP 404 Not Found | **PASS** |
| 20 | Cross-User Quiz Result Access| `GET /api/quiz/result/{id}` | Privacy | Direct request to other user's result is blocked | Blocked with HTTP 403 Forbidden | **PASS** |
| 21 | User Logout & Session Invalidation | `POST /api/auth/logout` | Auth | Invalidates session; subsequent call rejected | Logout: 200, `/api/auth/me`: 401 | **PASS** |
| 22 | Admin Authentication | `POST /api/auth/login` | Admin | Logs in as administrator with role `ADMIN` | Logged in as `admin`, role `ADMIN` | **PASS** |
| 23 | Admin Summary Metrics | `GET /api/admin/summary` | Admin | Real-time database summary counts returned | Metrics returned from MySQL | **PASS** |
| 24 | Admin Users Directory | `GET /api/admin/users` | Admin | User list returned; zero password hashes exposed | User list returned, passwords omitted | **PASS** |
| 25 | Admin Incident Status Workflow| `PATCH /api/admin/incidents/{id}/status` | Admin | Status transition: `OPEN` -> `UNDER_REVIEW` -> `RESOLVED` | Status updated with notes | **PASS** |
| 26 | Admin Question Toggle | `PATCH /api/admin/questions/{id}/toggle` | Admin | Toggles question active status (`true` <-> `false`) | Status toggled successfully | **PASS** |
| 27 | Admin Learning Modules Retrieval | `GET /api/admin/modules` | Admin | Returns 6 modules with order & active metadata | 6 modules returned | **PASS** |
| 28 | Admin Learning Module Toggle | `PATCH /api/admin/modules/{id}/toggle` | Admin | Toggles module active status (`true` <-> `false`) | Status toggled successfully | **PASS** |
| 29 | Stack Trace Prevention | `GET /api/nonexistent` | Error Hygiene | Returns sanitized error JSON; no stack frames | 0 traces of `org.springframework` | **PASS** |
| 30 | Database Password Hashing | Direct MySQL Query | Security | All stored passwords are BCrypt hashes | 0 non-BCrypt passwords in DB | **PASS** |
| 31 | Database Row Persistence | Direct MySQL Query | Persistence | Records confirmed in MySQL tables | Records confirmed in MySQL tables | **PASS** |
| 32 | Password Change: Wrong Current | `POST /api/auth/change-password` | Auth | Rejects invalid current password with HTTP 400 | Rejected with HTTP 400 Bad Request | **PASS** |
| 33 | Password Change: Valid Update | `POST /api/auth/change-password` | Auth | Updates password hash in MySQL | Returned `"Password changed successfully"` | **PASS** |
| 34 | Password Change: Old Password | `POST /api/auth/login` | Auth | Login attempt with former password fails | Rejected with HTTP 401 Unauthorized | **PASS** |
| 35 | Password Change: New Password | `POST /api/auth/login` | Auth | Login attempt with updated password succeeds | Successfully logged in | **PASS** |
| 36 | Session Revocation Filter | `UserAccountStatusFilter` | Security | Immediate session invalidation on disabled user | Session terminated with HTTP 401 | **PASS** |
| 37 | Incident CSV Export | `GET /api/admin/incidents/export/csv` | Exports | CSV exported with headers and formula shielding | Returned `text/csv;charset=UTF-8` | **PASS** |
| 38 | Incident PDF Export | `GET /api/admin/incidents/export/pdf` | Exports | Landscape PDF generated with `%PDF` magic header | Returned `application/pdf` (`%PDF`) | **PASS** |
| 39 | Question CRUD & Safeguard | `/api/admin/questions` | Admin | Create, edit, delete; safeguard blocks referenced Q | Safeguard blocked deletion of Q1 | **PASS** |
| 40 | Module CRUD & Safeguard | `/api/admin/modules` | Admin | Create, edit, delete; safeguard blocks referenced M | Safeguard blocked deletion of M1 | **PASS** |

---

## 4. Key Security Findings & Verifications

1. **Zero Plaintext Passwords:** Direct inspection of the `users` table via `SELECT password_hash FROM users;` confirmed that 100% of password hashes adhere to the `$2a$12$` format.
2. **Active Session Invalidation (`UserAccountStatusFilter`):** In Test 36, an active session was obtained for a test user. The administrator then deactivated the account (`enabled = false`). On the very next request using that active session cookie, the filter intercepted the request, invalidated the `HttpSession`, and returned HTTP 401 Unauthorized.
3. **Spreadsheet Formula Injection Defense:** In Test 37, the incident CSV export was inspected. All incident descriptions and titles were checked for leading formula characters (`=`, `+`, `-`, `@`, `\t`, `\r`), confirming they are prefixed with `'` to neutralize formula execution in Microsoft Excel.
4. **Relational Deletion Protection:** In Tests 39 and 40, attempts to delete questions answered in user assessments or modules marked complete by users were blocked with HTTP 400 Bad Request, preventing foreign key integrity violations.