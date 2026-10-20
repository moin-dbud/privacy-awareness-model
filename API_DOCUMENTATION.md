# Ã°Å¸â€œÂ¡ API Documentation

Base URL: `http://localhost:8080`

All protected endpoints require an active session (login first).  
CSRF token is sent automatically via the `XSRF-TOKEN` cookie and must be included in the `X-XSRF-TOKEN` header for mutating requests.

---

## Authentication

### POST /api/auth/register
Register a new user account.

**Request Body:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass@123",
  "fullName": "John Doe"
}
```

**Validation Rules:**
- `username`: 3Ã¢â‚¬â€œ50 characters, letters/digits/underscore only
- `email`: valid email format
- `password`: min 8 chars, must include uppercase, lowercase, digit, special character
- `fullName`: optional, max 100 characters

**Success Response** `201 Created`:
```json
{
  "id": 2,
  "username": "johndoe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER"
}
```

**Error Responses:**
- `400` Ã¢â‚¬â€ Validation errors (field-level details in `details` object)
- `409` Ã¢â‚¬â€ Username or email already taken

---

### POST /api/auth/login
Authenticate and start a session.

**Request Body:**
```json
{
  "username": "johndoe",
  "password": "SecurePass@123"
}
```

**Success Response** `200 OK`:
```json
{
  "id": 2,
  "username": "johndoe",
  "email": "john@example.com",
  "role": "USER"
}
```

**Error Responses:**
- `401` Ã¢â‚¬â€ Invalid credentials

---

### GET /api/auth/me
Get the currently authenticated user. Requires login.

**Success Response** `200 OK`:
```json
{
  "id": 2,
  "username": "johndoe",
  "email": "john@example.com",
  "role": "USER"
}
```

---

### POST /api/auth/logout
End the current session.

**Success Response** `200 OK`

---

### POST /api/auth/change-password
Update the password of the currently authenticated user. Requires active session.

**Request Body:**
```json
{
  "currentPassword": "OldPassword@123",
  "newPassword": "NewStrongPassword@888!",
  "confirmPassword": "NewStrongPassword@888!"
}
```

**Validation & Rules:**
- `currentPassword`: Must match the user's existing password hash (verified via BCrypt).
- `newPassword`: Minimum 8 characters, must contain at least 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special character. Cannot be identical to current password.
- `confirmPassword`: Must match `newPassword`.

**Success Response** `200 OK`:
```json
{
  "message": "Password changed successfully"
}
```

**Error Responses:**
- `400 Bad Request` — Incorrect current password or validation failure.
- `401 Unauthorized` — Unauthenticated request.

---

## Dashboard

### GET /api/dashboard
Get the authenticated user's dashboard summary. Requires login.

**Success Response** `200 OK`:
```json
{
  "username": "johndoe",
  "fullName": "John Doe",
  "role": "USER",
  "totalAssessments": 3,
  "latestScore": 80,
  "latestMaxScore": 150,
  "latestPercentage": 53.33,
  "latestRiskLevel": "MEDIUM",
  "latestTakenAt": "2026-10-19T12:00:00",
  "totalModules": 6,
  "completedModules": 2,
  "learningProgress": 33,
  "totalIncidentReports": 1
}
```

---

## Quiz

### GET /api/quiz/questions
Retrieve all active quiz questions. Requires login.  
**Note:** Correct answers are NOT included in this response to prevent cheating.

**Success Response** `200 OK`:
```json
[
  {
    "id": 1,
    "questionText": "Which of the following is a common sign of a phishing email?",
    "category": "PHISHING",
    "points": 10,
    "options": [
      { "id": 1, "optionText": "A generic greeting like 'Dear Customer'...", "displayOrder": 0 },
      { "id": 2, "optionText": "A personalised email from your bank...", "displayOrder": 1 },
      { "id": 3, "optionText": "A newsletter you subscribed to...", "displayOrder": 2 },
      { "id": 4, "optionText": "An order confirmation...", "displayOrder": 3 }
    ]
  }
]
```

---

### POST /api/quiz/submit
Submit quiz answers. Requires login.

**Request Body:**
```json
{
  "answers": [
    { "questionId": 1, "selectedOptionId": 1 },
    { "questionId": 2, "selectedOptionId": 8 },
    { "questionId": 3, "selectedOptionId": 11 }
  ]
}
```

**Success Response** `200 OK`:
```json
{
  "id": 5,
  "totalScore": 20,
  "maxPossibleScore": 30,
  "percentageScore": 66.67,
  "correctAnswers": 2,
  "totalQuestions": 3,
  "riskLevel": "MEDIUM",
  "recommendations": "Good effort! Review the topics below...\n\nAreas to focus on:\nÃ¢â‚¬Â¢ PHISHING",
  "takenAt": "2026-10-19T14:00:00",
  "answerFeedbacks": [
    {
      "questionText": "Which of the following is a common sign...",
      "selectedAnswer": "A generic greeting like 'Dear Customer'...",
      "correctAnswer": "A generic greeting like 'Dear Customer'...",
      "correct": true,
      "pointsEarned": 10,
      "explanation": "Phishing emails often create urgency..."
    }
  ]
}
```

**Risk Levels:**
| Score % | Risk Level |
|---|---|
| Ã¢â€°Â¥ 80% | LOW |
| 60Ã¢â‚¬â€œ79% | MEDIUM |
| 40Ã¢â‚¬â€œ59% | HIGH |
| < 40% | CRITICAL |

---

### GET /api/quiz/history
Get the authenticated user's quiz history. Requires login.

**Success Response** `200 OK`: Array of assessment result objects (same as submit response, without `answerFeedbacks`)

---

### GET /api/quiz/result/{id}
Get a specific result by ID. Requires login. Users can only access their own results.

**Success Response** `200 OK`: Full assessment result with answer feedbacks.

**Error Responses:**
- `403` Ã¢â‚¬â€ Trying to access another user's result
- `404` Ã¢â‚¬â€ Result not found

---

## Learning Center

### GET /api/learning/modules
Get all active learning modules with completion status. Requires login.

**Success Response** `200 OK`:
```json
[
  {
    "id": 1,
    "title": "Understanding Phishing Attacks",
    "category": "PHISHING",
    "description": "Learn how to recognise and avoid phishing scams...",
    "content": "<h2>What is Phishing?</h2>...",
    "estimatedMinutes": 8,
    "displayOrder": 1,
    "completed": false
  }
]
```

---

### GET /api/learning/modules/{id}
Get a single module with content. Requires login.

---

### POST /api/learning/modules/{id}/complete
Mark a module as completed. Requires login. Idempotent.

**Success Response** `200 OK`:
```json
{ "message": "Module marked as complete" }
```

---

### GET /api/learning/progress
Get learning progress summary. Requires login.

**Success Response** `200 OK`:
```json
{ "total": 6, "completed": 3 }
```

---

## Incident Reports

### POST /api/incidents
Submit a new incident report. Requires login.

**Request Body:**
```json
{
  "incidentType": "PHISHING_ATTEMPT",
  "title": "Received phishing DM on Instagram",
  "description": "I received a direct message asking me to verify my account through a suspicious link...",
  "platform": "Instagram"
}
```

**Incident Types:** `SUSPICIOUS_MESSAGE`, `IMPERSONATION`, `ACCOUNT_COMPROMISE`, `PHISHING_ATTEMPT`, `CYBERBULLYING`, `PRIVACY_VIOLATION`, `OTHER`

**Success Response** `201 Created`: Full report object.

---

### GET /api/incidents
Get the authenticated user's incident reports. Requires login.

---

### GET /api/incidents/{id}
Get a specific report. Requires login. Users can only see their own reports.

---
## Phishing Simulator

### GET /api/simulator/scenarios
Get all active phishing scenarios (10 fictional attacks across Email, SMS, DM) with red flag options. Requires login.

**Success Response** `200 OK`:
```json
[
  {
    "id": 1,
    "title": "ApexBank: Unauthorized Wire Transfer Detected",
    "channel": "EMAIL",
    "sender": "security-alert@apexbank-notice.net",
    "subject": "URGENT: Suspicious transfer of $4,850.00 initiated from new device",
    "content": "...",
    "difficulty": "BEGINNER",
    "redFlagOptions": [
      {
        "id": 1,
        "flagText": "Spoofed sender domain 'apexbank-notice.net' instead of official bank domain",
        "displayOrder": 1
      }
    ]
  }
]
```

### POST /api/simulator/submit
Submit user-identified red flags for a scenario and receive an educational score, flag breakdowns, and threat defense advice. Requires login & CSRF token.

**Request Body**:
```json
{
  "scenarioId": 1,
  "selectedFlagIds": [1, 2, 3]
}
```

**Success Response** `200 OK`:
```json
{
  "id": 1,
  "scenarioId": 1,
  "scenarioTitle": "ApexBank: Unauthorized Wire Transfer Detected",
  "channel": "EMAIL",
  "score": 100,
  "totalPossibleFlags": 4,
  "flagsIdentified": 4,
  "falsePositives": 0,
  "passed": true,
  "feedback": "Outstanding threat detection! You identified 4 of 4 red flags with keen precision.",
  "generalExplanation": "Classic high-pressure banking lure...",
  "takenAt": "2026-10-19T22:39:55",
  "flagDetails": [
    {
      "id": 1,
      "flagText": "Spoofed sender domain 'apexbank-notice.net'",
      "description": "Banks always use their registered primary domain name.",
      "actualRedFlag": true,
      "selectedByUser": true
    }
  ]
}
```

### GET /api/simulator/history
Get past phishing simulation results for the authenticated user. Requires login.


## Admin Endpoints
**All require ADMIN role.**

### GET /api/admin/summary
Platform-wide high-level counts.

### GET /api/admin/analytics
Comprehensive executive security analytics and intelligence metrics for the admin dashboard.

**Success Response** `200 OK`:
```json
{
  "averageScore": 74.5,
  "totalAssessments": 42,
  "incidentsByCategory": {
    "SUSPICIOUS_MESSAGE": 5,
    "IMPERSONATION": 2,
    "ACCOUNT_COMPROMISE": 3,
    "PHISHING_ATTEMPT": 7,
    "CYBERBULLYING": 1,
    "PRIVACY_VIOLATION": 4,
    "OTHER": 0
  },
  "incidentsByStatus": {
    "OPEN": 4,
    "UNDER_REVIEW": 2,
    "RESOLVED": 15,
    "DISMISSED": 1
  },
  "newUsersPerWeek": [
    { "weekLabel": "Sep 14", "userCount": 2 },
    { "weekLabel": "Sep 21", "userCount": 5 },
    { "weekLabel": "Sep 28", "userCount": 3 },
    { "weekLabel": "Oct 05", "userCount": 8 },
    { "weekLabel": "Oct 12", "userCount": 6 },
    { "weekLabel": "Oct 19", "userCount": 11 }
  ],
  "mostMissedQuestions": [
    {
      "questionId": 2,
      "questionText": "You receive a message claiming your account will be deleted...",
      "category": "PHISHING",
      "totalAttempts": 25,
      "missedCount": 18,
      "missedPercentage": 72.0
    }
  ]
}
```

### GET /api/admin/users
List all registered users with optional search query and role filter. Passwords and hashes are strictly omitted.

**Query Parameters (optional):**
- `search`: Filter by username, email, or full name substring.
- `role`: Filter by role (`USER` or `ADMIN`).

**Success Response** `200 OK`:
```json
[
  {
    "id": 1,
    "username": "admin",
    "email": "admin@privacy.local",
    "fullName": "System Administrator",
    "role": "ADMIN",
    "enabled": true,
    "createdAt": "2026-10-19T20:47:00"
  }
]
```

### POST /api/admin/users
Create a new user account with pre-assigned role and BCrypt password hashing.

**Request Body:**
```json
{
  "username": "alice_smith",
  "email": "alice@company.local",
  "fullName": "Alice Smith",
  "password": "Password@123!",
  "role": "USER"
}
```

**Success Response** `201 Created`: Returns created `UserResponse` (without password hash).

**Error Responses:**
- `400 Bad Request` — Validation failure (e.g. password too short, invalid email).
- `409 Conflict` — Username or email already registered.

### DELETE /api/admin/users/{id}
Permanently delete a user account and cascade-delete all linked records (assessment results, answers, incident reports, module progress, simulations).

**Safeguards:**
- Cannot delete your own account (returns `400 Bad Request`).
- Cannot delete the last remaining `ADMIN` account (returns `400 Bad Request`).

**Success Response** `200 OK`:
```json
{
  "message": "User deleted successfully",
  "id": "5"
}
```

### PATCH /api/admin/users/{id}/role
Promote or demote a user's role.

**Request Body:**
```json
{
  "role": "ADMIN"
}
```

**Safeguard:**
- Cannot demote the last remaining `ADMIN` account (returns `400 Bad Request`).

### PATCH /api/admin/users/{id}/toggle-status
Activate or deactivate a user account. Deactivated accounts cannot sign in.

**Safeguard:**
- An admin cannot deactivate themselves.
- Cannot deactivate the last remaining active `ADMIN`.

**Success Response** `200 OK`: Returns updated `UserResponse` with new `enabled` boolean.

### GET /api/admin/incidents
Retrieve all incident reports, with optional filtering by status and incident type.

**Query Parameters (Optional):**
- `status`: `OPEN`, `UNDER_REVIEW`, `RESOLVED`, `DISMISSED`
- `type`: `PHISHING`, `DATA_LEAK`, `HARASSMENT`, `SUSPICIOUS_LOGIN`, `IMPERSONATION`, `MALWARE`, `OTHER`

### GET /api/admin/incidents/export/csv
Export incident reports as an RFC 4180-compliant CSV file with built-in spreadsheet formula injection protection.

**Query Parameters (Optional):**
- `status`: Filter by incident status
- `type`: Filter by incident type

**Security Protection:**
- Formula characters (`=`, `+`, `-`, `@`, `\t`, `\r`) at the beginning of fields are automatically prefixed with `'` to prevent command execution in Microsoft Excel or LibreOffice Calc.
- Emits UTF-8 Byte Order Mark (`\uFEFF`) for seamless international character rendering.

**Response:** `200 OK`, `Content-Type: text/csv;charset=UTF-8`

---

### GET /api/admin/incidents/export/pdf
Export incident reports as a formatted, corporate-grade landscape PDF document powered by OpenPDF.

**Query Parameters (Optional):**
- `status`: Filter by incident status
- `type`: Filter by incident type

**Response:** `200 OK`, `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="incident-reports-*.pdf"`

---

### PATCH /api/admin/incidents/{id}/status
Update incident status and attach resolution notes.

**Request Body:**
```json
{
  "status": "RESOLVED",
  "adminNotes": "User was advised to enable 2FA and change their password."
}
```

**Status Values:** `OPEN`, `UNDER_REVIEW`, `RESOLVED`, `DISMISSED`

---

### Quiz Question Management (Full CRUD)

#### GET /api/admin/questions
Retrieve all quiz questions with full option arrays, correct flags, point values, and active statuses.

#### GET /api/admin/questions/{id}
Retrieve detailed information for a single quiz question by ID.

#### POST /api/admin/questions
Create a new quiz question with multiple options.

**Request Body:**
```json
{
  "questionText": "What should you check first when receiving an urgent email asking to verify account credentials?",
  "category": "PHISHING_DETECTION",
  "explanation": "Phishing emails frequently impersonate official domains. Always verify sender domain.",
  "points": 10,
  "active": true,
  "options": [
    { "optionText": "Inspect sender email header and actual URL links", "correct": true, "displayOrder": 1 },
    { "optionText": "Click the link immediately to verify before account suspension", "correct": false, "displayOrder": 2 }
  ]
}
```

#### PUT /api/admin/questions/{id}
Update an existing question and its options. Replaces option set with the provided array.

#### PATCH /api/admin/questions/{id}/toggle
Toggle question active/inactive state. Inactive questions are excluded from user assessments.

#### DELETE /api/admin/questions/{id}
Delete a quiz question.

**Relational Deletion Safeguard:**
- If the question has been answered in past student assessments (present in `assessment_answers`), hard deletion is **blocked** with `400 Bad Request` (`"Cannot delete question as it is referenced in past assessments. Deactivate it instead."`).
- Unreferenced questions are safely removed along with their options.

---

### Learning Module Management (Full CRUD)

#### GET /api/admin/modules
Retrieve all learning modules with full content and display orders.

#### GET /api/admin/modules/{id}
Retrieve a single learning module by ID.

#### POST /api/admin/modules
Author a new learning curriculum module.

**Request Body:**
```json
{
  "title": "Securing Third-Party App Permissions",
  "category": "PRIVACY_SETTINGS",
  "description": "Learn how OAuth tokens and app integrations can access your profile data.",
  "content": "<h2>OAuth & Third-Party Permissions</h2><p>Regularly audit connected applications.</p>",
  "estimatedMinutes": 5,
  "displayOrder": 7,
  "active": true
}
```

#### PUT /api/admin/modules/{id}
Update an existing learning module.

#### PATCH /api/admin/modules/{id}/toggle
Toggle a module active/inactive in the user learning catalog.

#### DELETE /api/admin/modules/{id}
Delete a learning module.

**Relational Deletion Safeguard:**
- If student progress exists for this module in `module_progress`, hard deletion is **blocked** with `400 Bad Request` (`"Cannot delete module as users have recorded progress for it. Deactivate it instead."`).
- Unreferenced modules are safely removed.

---

---

## Error Response Format

All errors follow this structure:
```json
{
  "timestamp": "2026-10-19T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "password": "Password does not meet requirements",
    "username": "Username must be 3Ã¢â‚¬â€œ50 characters"
  }
}
```

No stack traces, exception class names, or internal paths are exposed.

---

## Postman Setup

1. Import a new collection
2. Set Base URL variable: `{{baseUrl}}` = `http://localhost:8080`
3. Register a user, then login
4. Get the CSRF token from the `XSRF-TOKEN` cookie
5. Add header `X-XSRF-TOKEN: {{csrfToken}}` to mutating requests
6. Session cookie `JSESSIONID` is sent automatically by Postman
