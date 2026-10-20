# Privacy Guard — REST API Reference

Base URL: `http://localhost:8080`

The Privacy Guard REST API uses session-based authentication managed by Spring Security. State-changing requests (`POST`, `PUT`, `PATCH`, `DELETE`) require an active CSRF token. The server issues a cookie named `XSRF-TOKEN` upon your first request; clients must include this value in the `X-XSRF-TOKEN` HTTP header for mutating requests.

---

## Table of Contents
1. [Authentication Endpoints](#1-authentication-endpoints)
2. [User Dashboard Endpoints](#2-user-dashboard-endpoints)
3. [Privacy Quiz Endpoints](#3-privacy-quiz-endpoints)
4. [Learning Center Endpoints](#4-learning-center-endpoints)
5. [Incident Reporting Endpoints](#5-incident-reporting-endpoints)
6. [Phishing Simulator Endpoints](#6-phishing-simulator-endpoints)
7. [Admin Management Endpoints](#7-admin-management-endpoints)
8. [Standard Error Format](#8-standard-error-format)

---

## 1. Authentication Endpoints

### 1.1 Register User
- **Method:** `POST`
- **Path:** `/api/auth/register`
- **Access:** Public
- **CSRF Required:** Yes

**Request Body (`RegisterRequest`):**
```json
{
  "username": "alex_turner",
  "email": "alex@example.com",
  "password": "SecurePassword@123",
  "fullName": "Alex Turner"
}
```

**Validation Constraints:**
- `username`: 3 to 50 characters, letters, digits, and underscores only.
- `email`: Valid email format, maximum 100 characters.
- `password`: Minimum 8 characters, must contain at least 1 uppercase letter, 1 lowercase letter, 1 digit, and 1 special character.
- `fullName`: Optional, maximum 100 characters.

**Success Response (`201 Created`):**
```json
{
  "id": 14,
  "username": "alex_turner",
  "email": "alex@example.com",
  "fullName": "Alex Turner",
  "role": "USER",
  "enabled": true,
  "createdAt": "2026-10-20T10:15:30"
}
```

**Error Responses:**
- `400 Bad Request`: Validation failure on input fields.
- `409 Conflict`: Username or email address already registered.

---

### 1.2 User Login
- **Method:** `POST`
- **Path:** `/api/auth/login`
- **Access:** Public
- **CSRF Required:** Yes

**Request Body (`LoginRequest`):**
```json
{
  "username": "alex_turner",
  "password": "SecurePassword@123"
}
```

**Success Response (`200 OK`):**
Sets the `JSESSIONID` session cookie.
```json
{
  "id": 14,
  "username": "alex_turner",
  "email": "alex@example.com",
  "fullName": "Alex Turner",
  "role": "USER",
  "enabled": true,
  "createdAt": "2026-10-20T10:15:30"
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid username or password (`"message": "Invalid username or password"`).
- `403 Forbidden`: Account is disabled (`"message": "Account is disabled"`).

---

### 1.3 Current User Profile
- **Method:** `GET`
- **Path:** `/api/auth/me`
- **Access:** Authenticated (`USER` or `ADMIN`)
- **CSRF Required:** No

**Success Response (`200 OK`):**
Returns the profile of the user associated with the active session.
```json
{
  "id": 14,
  "username": "alex_turner",
  "email": "alex@example.com",
  "fullName": "Alex Turner",
  "role": "USER",
  "enabled": true,
  "createdAt": "2026-10-20T10:15:30"
}
```

**Error Responses:**
- `401 Unauthorized`: Unauthenticated request or expired session.

---

### 1.4 Change Password
- **Method:** `POST`
- **Path:** `/api/auth/change-password`
- **Access:** Authenticated (`USER` or `ADMIN`)
- **CSRF Required:** Yes

**Request Body (`ChangePasswordRequest`):**
```json
{
  "currentPassword": "SecurePassword@123",
  "newPassword": "NewStrongPassword@888!",
  "confirmPassword": "NewStrongPassword@888!"
}
```

**Validation Constraints:**
- `currentPassword`: Must match the authenticated user's current password.
- `newPassword`: Minimum 8 characters, must include uppercase, lowercase, digit, and symbol. Cannot match `currentPassword`.
- `confirmPassword`: Must match `newPassword`.

**Success Response (`200 OK`):**
```json
{
  "message": "Password changed successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Incorrect current password or password validation failure.
- `401 Unauthorized`: Session missing or expired.

---

### 1.5 User Logout
- **Method:** `POST`
- **Path:** `/api/auth/logout`
- **Access:** Authenticated
- **CSRF Required:** Yes

**Success Response (`200 OK`):**
Invalidates the HTTP session and clears the session context. Subsequent requests will return `401 Unauthorized`.

---

## 2. User Dashboard Endpoints

### 2.1 Dashboard Summary
- **Method:** `GET`
- **Path:** `/api/dashboard`
- **Access:** Authenticated (`USER` or `ADMIN`)
- **CSRF Required:** No

**Success Response (`200 OK`):**
Returns real-time aggregated metrics from MySQL:
```json
{
  "username": "alex_turner",
  "fullName": "Alex Turner",
  "role": "USER",
  "totalAssessments": 1,
  "latestScore": 120,
  "latestMaxScore": 150,
  "latestPercentage": 80.0,
  "latestRiskLevel": "LOW",
  "latestTakenAt": "2026-10-20T14:22:10",
  "totalModules": 6,
  "completedModules": 3,
  "learningProgress": 50,
  "totalIncidentReports": 2,
  "openIncidentReports": 1,
  "underReviewIncidentReports": 0,
  "resolvedIncidentReports": 1,
  "dismissedIncidentReports": 0,
  "totalSimulations": 4,
  "passedSimulations": 3,
  "simulationPassRate": 75.0,
  "recentAssessments": [
    {
      "id": 8,
      "score": 120,
      "maxScore": 150,
      "percentage": 80.0,
      "riskLevel": "LOW",
      "takenAt": "2026-10-20T14:22:10"
    }
  ],
  "recentIncidents": [
    {
      "id": 5,
      "title": "Suspicious login attempt from unknown IP",
      "type": "SUSPICIOUS_LOGIN",
      "status": "OPEN",
      "reportedAt": "2026-10-20T12:00:00"
    }
  ],
  "recommendations": [
    {
      "category": "PASSWORD_SAFETY",
      "priority": "HIGH",
      "title": "Improve Password Safety (50.0% Accuracy)",
      "description": "Review learning materials to understand key vulnerability vectors and defensive measures.",
      "actionUrl": "/learning.html",
      "actionText": "Explore Modules"
    }
  ]
}
```

---

### 2.2 Category Breakdown
- **Method:** `GET`
- **Path:** `/api/dashboard/category-breakdown`
- **Access:** Authenticated
- **CSRF Required:** No

**Success Response (`200 OK`):**
Returns accuracy percentages and risk classifications for each of the 6 privacy categories:
```json
[
  {
    "category": "PHISHING",
    "categoryName": "Phishing Attacks",
    "totalQuestions": 4,
    "correctCount": 3,
    "accuracyPercentage": 75.0,
    "riskScore": 25.0,
    "riskLevel": "MEDIUM"
  },
  {
    "category": "PASSWORD_SAFETY",
    "categoryName": "Password Safety",
    "totalQuestions": 2,
    "correctCount": 1,
    "accuracyPercentage": 50.0,
    "riskScore": 50.0,
    "riskLevel": "HIGH"
  }
]
```

---

### 2.3 Score Trend
- **Method:** `GET`
- **Path:** `/api/dashboard/score-trend`
- **Access:** Authenticated
- **CSRF Required:** No

**Success Response (`200 OK`):**
Returns chronological history of all quiz attempts taken by the authenticated user:
```json
[
  {
    "assessmentId": 1,
    "takenAt": "2026-10-18T11:00:00",
    "score": 60,
    "maxScore": 150,
    "percentage": 40.0,
    "riskLevel": "HIGH"
  },
  {
    "assessmentId": 8,
    "takenAt": "2026-10-20T14:22:10",
    "score": 120,
    "maxScore": 150,
    "percentage": 80.0,
    "riskLevel": "LOW"
  }
]
```

---

## 3. Privacy Quiz Endpoints

### 3.1 Fetch Active Quiz Questions
- **Method:** `GET`
- **Path:** `/api/quiz/questions`
- **Access:** Authenticated
- **CSRF Required:** No

**Success Response (`200 OK`):**
Returns active questions and multiple-choice options. The `correct` boolean is stripped from options to prevent client inspection.
```json
[
  {
    "id": 1,
    "questionText": "Which of the following is a common sign of a phishing email?",
    "category": "PHISHING",
    "points": 10,
    "options": [
      {
        "id": 1,
        "optionText": "A generic greeting like 'Dear Customer' with urgent requests for account verification",
        "displayOrder": 1
      },
      {
        "id": 2,
        "optionText": "A personalised email from your bank with your full name and last 4 digits of your card",
        "displayOrder": 2
      }
    ]
  }
]
```

---

### 3.2 Submit Quiz Assessment
- **Method:** `POST`
- **Path:** `/api/quiz/submit`
- **Access:** Authenticated
- **CSRF Required:** Yes

**Request Body (`QuizSubmitRequest`):**
```json
{
  "answers": [
    { "questionId": 1, "selectedOptionId": 1 },
    { "questionId": 2, "selectedOptionId": 7 }
  ]
}
```

**Success Response (`200 OK`):**
Calculates score, assigns a risk tier (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), and reveals explanations and correct flags:
```json
{
  "id": 9,
  "totalScore": 140,
  "maxPossibleScore": 150,
  "percentageScore": 93.33,
  "riskLevel": "LOW",
  "correctAnswers": 14,
  "totalQuestions": 15,
  "takenAt": "2026-10-20T15:10:00",
  "recommendations": "Excellent work! Your privacy awareness is strong. Stay vigilant and keep learning.",
  "answers": [
    {
      "questionId": 1,
      "questionText": "Which of the following is a common sign of a phishing email?",
      "category": "PHISHING",
      "selectedOptionId": 1,
      "selectedOptionText": "A generic greeting like 'Dear Customer' with urgent requests...",
      "correct": true,
      "pointsEarned": 10,
      "explanation": "Phishing emails often create urgency and ask you to click links..."
    }
  ]
}
```

---

### 3.3 Assessment History
- **Method:** `GET`
- **Path:** `/api/quiz/history`
- **Access:** Authenticated
- **CSRF Required:** No

Returns the chronological list of assessment results for the current user.

---

### 3.4 Single Assessment Result
- **Method:** `GET`
- **Path:** `/api/quiz/result/{id}`
- **Access:** Authenticated (Owner only)
- **CSRF Required:** No

**Error Responses:**
- `403 Forbidden`: Attempting to inspect another user's quiz result (`AccessDeniedException`).
- `404 Not Found`: Result ID does not exist.

---

## 4. Learning Center Endpoints

### 4.1 List All Modules
- **Method:** `GET`
- **Path:** `/api/learning/modules`
- **Access:** Authenticated
- **CSRF Required:** No

Returns all active learning curriculum modules along with a `completed` boolean indicating whether the current user has completed each module.

---

### 4.2 Get Module Details
- **Method:** `GET`
- **Path:** `/api/learning/modules/{id}`
- **Access:** Authenticated
- **CSRF Required:** No

Returns full HTML/rich text content, estimated reading minutes, and category details for a specific module.

---

### 4.3 Mark Module as Complete
- **Method:** `POST`
- **Path:** `/api/learning/modules/{id}/complete`
- **Access:** Authenticated
- **CSRF Required:** Yes

Records progress in the `module_progress` table. Idempotent — completing a module multiple times does not create duplicate records.

**Success Response (`200 OK`):**
```json
{
  "message": "Module marked as complete"
}
```

---

### 4.4 Get Learning Progress Summary
- **Method:** `GET`
- **Path:** `/api/learning/progress`
- **Access:** Authenticated
- **CSRF Required:** No

**Success Response (`200 OK`):**
```json
{
  "totalModules": 6,
  "completedModules": 3,
  "progressPercentage": 50
}
```

---

## 5. Incident Reporting Endpoints

### 5.1 Submit Incident Report
- **Method:** `POST`
- **Path:** `/api/incidents`
- **Access:** Authenticated
- **CSRF Required:** Yes

**Request Body (`IncidentReportRequest`):**
```json
{
  "title": "Received deceptive Direct Message claiming account copyright strike",
  "description": "User @support_verified98 sent a link asking to log in to prevent suspension.",
  "incidentType": "PHISHING"
}
```

**Allowed `incidentType` values:**
`PHISHING`, `DATA_LEAK`, `HARASSMENT`, `SUSPICIOUS_LOGIN`, `IMPERSONATION`, `MALWARE`, `OTHER`.

**Success Response (`201 Created`):**
```json
{
  "id": 7,
  "title": "Received deceptive Direct Message claiming account copyright strike",
  "description": "User @support_verified98 sent a link...",
  "incidentType": "PHISHING",
  "status": "OPEN",
  "reportedAt": "2026-10-20T16:00:00",
  "reportedBy": "alex_turner",
  "adminNotes": null
}
```

---

### 5.2 List User's Reports
- **Method:** `GET`
- **Path:** `/api/incidents`
- **Access:** Authenticated
- **CSRF Required:** No

Returns only the incidents submitted by the authenticated user. Cross-user data isolation ensures users cannot view other users' incident reports.

---

### 5.3 Get Incident Report by ID
- **Method:** `GET`
- **Path:** `/api/incidents/{id}`
- **Access:** Authenticated (Owner only)
- **CSRF Required:** No

Returns the single incident report if owned by the caller. If the report belongs to another user, returns `404 Not Found` to prevent account/report enumeration.

---

## 6. Phishing Simulator Endpoints

### 6.1 List Phishing Scenarios
- **Method:** `GET`
- **Path:** `/api/simulator/scenarios`
- **Access:** Authenticated
- **CSRF Required:** No

Returns 10 seeded scenarios (Email, SMS, Direct Message) with difficulty levels (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`). The `correct` flag on red flags is omitted to prevent client tampering.

---

### 6.2 Get Scenario Details
- **Method:** `GET`
- **Path:** `/api/simulator/scenarios/{id}`
- **Access:** Authenticated
- **CSRF Required:** No

---

### 6.3 Submit Simulation Attempt
- **Method:** `POST`
- **Path:** `/api/simulator/submit`
- **Access:** Authenticated
- **CSRF Required:** Yes

**Request Body (`PhishingSubmitRequest`):**
```json
{
  "scenarioId": 1,
  "selectedFlagIds": [1, 2, 3]
}
```

**Success Response (`200 OK`):**
Evaluates selected red flags, determines if the user correctly spotted all primary indicators without selecting distractors, saves outcome to `simulation_results`, and returns the educational debrief:
```json
{
  "id": 12,
  "scenarioId": 1,
  "scenarioTitle": "ApexBank: Suspicious Transfer Verification",
  "score": 85,
  "passed": true,
  "feedback": "Great identification! You correctly identified the spoofed sender domain...",
  "completedAt": "2026-10-20T16:30:00"
}
```

---

### 6.4 User Simulation History
- **Method:** `GET`
- **Path:** `/api/simulator/history`
- **Access:** Authenticated
- **CSRF Required:** No

---

## 7. Admin Management Endpoints

All endpoints under `/api/admin/**` require the `ADMIN` role. Requests from users with the `USER` role are rejected with `403 Forbidden`.

### 7.1 Admin Summary KPI Metrics
- **Method:** `GET`
- **Path:** `/api/admin/summary`
- **Access:** `ADMIN`

Returns system-wide operational metrics:
```json
{
  "totalUsers": 12,
  "activeUsers": 11,
  "disabledUsers": 1,
  "totalAssessments": 8,
  "averageQuizScore": 76.5,
  "totalIncidentReports": 5,
  "openIncidents": 2,
  "underReviewIncidents": 1,
  "resolvedIncidents": 2,
  "dismissedIncidents": 0,
  "totalLearningModules": 6,
  "activeLearningModules": 6,
  "totalSimulations": 15,
  "passedSimulations": 12,
  "simulationPassRate": 80.0
}
```

---

### 7.2 Detailed Admin Analytics
- **Method:** `GET`
- **Path:** `/api/admin/analytics`
- **Access:** `ADMIN`

Returns comprehensive analytics including:
- Most frequently missed questions across all student assessments.
- Incident volume distribution grouped by category and status.
- New user registration growth per week.
- Per-module student completion counts.
- Category-level accuracy percentages across the user base.

---

### 7.3 User Management

#### List Users
- **Method:** `GET`
- **Path:** `/api/admin/users`
- **Query Params:** `search` (optional string), `role` (optional: `USER` or `ADMIN`)
- **Access:** `ADMIN`

#### Create User as Admin
- **Method:** `POST`
- **Path:** `/api/admin/users`
- **Access:** `ADMIN`
- **Request Body:** `CreateUserRequest` (`username`, `email`, `password`, `fullName`, `role`)

#### Update User Role
- **Method:** `PATCH`
- **Path:** `/api/admin/users/{id}/role`
- **Access:** `ADMIN`
- **Request Body:** `{"role": "ADMIN"}`
- **Safeguard:** Cannot demote the last remaining administrator account (`400 Bad Request`).

#### Toggle Account Status (Activate / Deactivate)
- **Method:** `PATCH`
- **Path:** `/api/admin/users/{id}/toggle-status`
- **Access:** `ADMIN`
- **Safeguard:** An administrator cannot deactivate themselves or the last active admin.
- **Session Revocation:** If an account is deactivated, `UserAccountStatusFilter` immediately terminates any active user sessions on their next request.

#### Delete User
- **Method:** `DELETE`
- **Path:** `/api/admin/users/{id}`
- **Access:** `ADMIN`
- **Safeguard:** Cannot delete your own account or the last remaining administrator.
- **Cascade:** Deleting a user cascades to their assessment results, answers, incident reports, module progress, and simulation records.

---

### 7.4 Incident Management & Exports

#### List All Incidents
- **Method:** `GET`
- **Path:** `/api/admin/incidents`
- **Access:** `ADMIN`

#### Update Incident Status & Notes
- **Method:** `PATCH`
- **Path:** `/api/admin/incidents/{id}/status`
- **Access:** `ADMIN`
- **Request Body:**
  ```json
  {
    "status": "RESOLVED",
    "adminNotes": "Advised user to enable MFA and revoke third-party app tokens."
  }
  ```
- **Allowed statuses:** `OPEN`, `UNDER_REVIEW`, `RESOLVED`, `DISMISSED`.

#### Export Incidents to CSV
- **Method:** `GET`
- **Path:** `/api/admin/incidents/export/csv`
- **Access:** `ADMIN`
- **Query Params:** `status` (optional), `category` (optional)
- **Content-Type:** `text/csv; charset=UTF-8`
- **Security:** Formula injection shielding prefixes dangerous characters (`=`, `+`, `-`, `@`, `\t`, `\r`) with an apostrophe (`'`). Emits UTF-8 Byte Order Mark (`\uFEFF`) for Microsoft Excel compatibility.

#### Export Incidents to PDF
- **Method:** `GET`
- **Path:** `/api/admin/incidents/export/pdf`
- **Access:** `ADMIN`
- **Query Params:** `status` (optional), `category` (optional)
- **Content-Type:** `application/pdf`
- **Generation:** Generates a landscape A4 report via OpenPDF with executive summary statistics and detailed table rows.

---

### 7.5 Quiz Question Management (Full CRUD)

#### List All Questions
- **Method:** `GET`
- **Path:** `/api/admin/questions`
- **Access:** `ADMIN`

#### Get Single Question with Options
- **Method:** `GET`
- **Path:** `/api/admin/questions/{id}`
- **Access:** `ADMIN`

#### Create Question
- **Method:** `POST`
- **Path:** `/api/admin/questions`
- **Access:** `ADMIN`
- **Request Body (`QuestionRequest`):**
  ```json
  {
    "questionText": "What should you check first when receiving an urgent email asking to verify account credentials?",
    "category": "PHISHING",
    "explanation": "Always inspect the sender domain and links rather than clicking through.",
    "points": 10,
    "active": true,
    "options": [
      { "optionText": "Inspect sender email header and actual URL links", "correct": true, "displayOrder": 1 },
      { "optionText": "Click the link immediately to verify before suspension", "correct": false, "displayOrder": 2 }
    ]
  }
  ```
- **Validation:** Minimum 2 options required, and exactly 1 option must be designated as `correct: true`.

#### Update Question
- **Method:** `PUT`
- **Path:** `/api/admin/questions/{id}`
- **Access:** `ADMIN`

#### Toggle Question Active State
- **Method:** `PATCH`
- **Path:** `/api/admin/questions/{id}/toggle`
- **Access:** `ADMIN`

#### Delete Question
- **Method:** `DELETE`
- **Path:** `/api/admin/questions/{id}`
- **Access:** `ADMIN`
- **Relational Safeguard:** If the question has been answered in past user assessments (present in `assessment_answers`), hard deletion is rejected with `400 Bad Request`. Administrators are instructed to toggle the question to inactive instead to preserve student assessment records.

---

### 7.6 Learning Module Management (Full CRUD)

#### List All Learning Modules
- **Method:** `GET`
- **Path:** `/api/admin/modules`
- **Access:** `ADMIN`

#### Get Single Module Details
- **Method:** `GET`
- **Path:** `/api/admin/modules/{id}`
- **Access:** `ADMIN`

#### Create Learning Module
- **Method:** `POST`
- **Path:** `/api/admin/modules`
- **Access:** `ADMIN`
- **Request Body (`ModuleRequest`):**
  ```json
  {
    "title": "Securing Third-Party App Permissions",
    "category": "PRIVACY_SETTINGS",
    "description": "Understand how OAuth tokens grant permissions to external apps.",
    "content": "<h2>Audit Connected Applications</h2><p>Regularly remove unused integrations...</p>",
    "estimatedMinutes": 5,
    "displayOrder": 7,
    "active": true
  }
  ```

#### Update Learning Module
- **Method:** `PUT`
- **Path:** `/api/admin/modules/{id}`
- **Access:** `ADMIN`

#### Toggle Module Active State
- **Method:** `PATCH`
- **Path:** `/api/admin/modules/{id}/toggle`
- **Access:** `ADMIN`

#### Delete Learning Module
- **Method:** `DELETE`
- **Path:** `/api/admin/modules/{id}`
- **Access:** `ADMIN`
- **Relational Safeguard:** If users have recorded completion progress for this module (present in `module_progress`), deletion is rejected with `400 Bad Request`. Administrators are instructed to deactivate the module instead.

---

## 8. Standard Error Format

All error responses from the application follow a consistent JSON structure handled by `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-10-20T17:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "password": "Password must contain at least 1 digit",
    "username": "Username must be between 3 and 50 characters"
  }
}
```

No stack traces, internal Java package names, or SQL query fragments are exposed to the client in production error responses.
