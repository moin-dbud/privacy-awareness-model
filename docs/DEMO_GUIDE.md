# Privacy Guard — College Demonstration & Viva Script

This script provides a step-by-step demonstration walkthrough for college project reviews, evaluator evaluations, and viva presentations. It outlines the exact sequence of screens to show, key technical features to highlight, and concise answers to likely evaluator questions.

---

## 1. Pre-Demo Setup Checklist (5 Minutes Before)

1. **Verify MySQL is Running:**
   ```powershell
   Get-Service MySQL80
   ```
2. **Launch Application in PowerShell:**
   ```powershell
   cd "C:\Users\my\Downloads\Phishing System"
   . .\Set-Env.ps1
   mvn spring-boot:run
   ```
3. **Open Browser:**
   Navigate to [http://localhost:8080](http://localhost:8080) and make sure the page loads cleanly.
4. **Prepare Two Browser Profiles or an Incognito Window:**
   - Normal Window: For regular student user demonstration.
   - Incognito Window: For administrator panel demonstration.

---

## 2. Step-by-Step Demonstration Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                      Demonstration Journey                             │
│                                                                        │
│  1. Landing Page (Overview & Threat Context)                           │
│  2. User Registration & Password Validation                            │
│  3. User Dashboard & Empty States                                      │
│  4. Privacy Assessment Quiz & Risk Scoring                             │
│  5. Phishing Simulator Lab (Hands-on Red Flags)                        │
│  6. Password Strength Checker Tool                                     │
│  7. Learning Center & Progress Tracking                                │
│  8. Incident Reporting Submission                                      │
│  9. Authenticated Password Change Workflow                             │
│ 10. Administrator Panel: Analytics, Triage, Exports, CRUD & Safeguards │
└────────────────────────────────────────────────────────────────────────┘
```

---

### Step 1: Landing Page (`index.html`)
- **Action:** Open `http://localhost:8080`.
- **What to say:**  
  *"Privacy Guard is an educational cybersecurity web application built with Spring Boot 3, Java 21, and MySQL 8. It evaluates user habits through interactive diagnostics, provides realistic phishing simulations, and offers targeted learning to reduce social media privacy risks."*
- **Key point to highlight:** The application uses clean vanilla HTML5/CSS3 with a dark cybersecurity theme without relying on heavyweight frontend JavaScript frameworks.

---

### Step 2: User Registration (`register.html`)
- **Action:** Click **Get Started** or **Register**.
- **Action:** Attempt to submit with a weak password like `pass` to demonstrate validation feedback. Then register a test user:
  - **Username:** `evaluator_demo`
  - **Email:** `evaluator@university.edu`
  - **Password:** `SecurityDemo@2026!`
  - **Full Name:** `Project Evaluator`
- **What to say:**  
  *"The registration endpoint enforces Jakarta Bean Validation on the server. Passwords must be at least 8 characters with uppercase, lowercase, numbers, and symbols. The password is never stored in plaintext—it is hashed using BCrypt with a cost factor of 12."*

---

### Step 3: User Dashboard (`dashboard.html`)
- **Action:** Log in as `evaluator_demo`.
- **What to say:**  
  *"When a new user logs in, the dashboard displays real-time metrics pulled directly from MySQL. Notice the empty-state prompts: rather than showing empty tables or null errors, the system presents clear educational calls-to-action guiding the user to take their first assessment."*

---

### Step 4: Privacy Assessment Quiz (`quiz.html`)
- **Action:** Click **Take Assessment** or navigate to **Quiz**.
- **Action:** Answer the questions. Answer a few correctly and deliberately miss one or two (e.g., in Password Safety or Phishing). Submit the quiz.
- **Action:** Review the results screen.
- **What to say:**  
  *"The quiz presents 15 diagnostic questions across six threat categories: Phishing, Password Safety, Privacy Settings, Identity Theft, Cyberbullying, and Oversharing. Notice three key behaviors:*
  1. *The correct answer flags are completely omitted from the question delivery API, preventing inspection in browser developer tools.*
  2. *Upon submission, the server computes the percentage score and assigns an awareness risk tier: Low, Medium, High, or Critical.*
  3. *The system analyzes incorrect answers and generates targeted study recommendations directing the user to specific learning modules.*
  4. *The user immediately receives detailed explanations explaining why each option was correct or incorrect."*

---

### Step 5: Phishing Simulation Lab (`simulator.html`)
- **Action:** Click **Phishing Lab** in the navigation bar.
- **Action:** Open Scenario 1: **ApexBank Suspicious Wire Transfer Alert**.
- **Action:** Show the lure. Point out the spoofed sender domain (`apexbank-notice.net`), the artificial 15-minute countdown, and the generic greeting.
- **Action:** Select the corresponding red flags, avoiding the distractor (*"The email contains the FDIC member disclaimer"*). Submit the simulation.
- **What to say:**  
  *"Rather than just reading about phishing, users practice identifying real indicators across email, SMS, and direct message channels. Distractors test whether users can distinguish authentic features from actual threats. The score and feedback are saved directly to MySQL."*

---

### Step 6: Password Strength Checker (`password-checker.html`)
- **Action:** Navigate to **Password Checker**.
- **Action:** Type a common password like `password123` to show low entropy, pattern warnings, and instant crack time.
- **Action:** Type a passphrase like `Correct-Horse-Battery-Staple-2026!` to show high entropy and an estimated crack time of centuries.
- **What to say:**  
  *"This tool runs client-side to demonstrate the exponential impact of password length and entropy without ever transmitting the entered passphrase over the network."*

---

### Step 7: Learning Center & Progress Tracking (`learning.html`)
- **Action:** Navigate to **Learning Center**. Open the **Password Safety** module.
- **Action:** Scroll through the structured content and click **Mark as Complete**.
- **Action:** Return to **Dashboard** and show the updated progress bar (e.g., 1 of 6 completed, 17%).
- **What to say:**  
  *"Module completion is persisted in the `module_progress` table with a composite unique constraint on user ID and module ID, ensuring accurate progress tracking across sessions."*

---

### Step 8: Incident Reporting (`incidents.html`)
- **Action:** Navigate to **Incidents**.
- **Action:** Fill out a sample report:
  - **Type:** `IMPERSONATION`
  - **Title:** `Fake university portal clone on social media`
  - **Description:** `Found an account mimicking the university department asking students for login credentials.`
- **Action:** Submit and show the record appearing in the user's personal list with status `OPEN`.
- **What to say:**  
  *"Users can document incidents in a structured format. Horizontal tenant isolation ensures that users can only see reports they submitted."*

---

### Step 9: Authenticated Password Change
- **Action:** Click **Change Password** in the top navigation bar.
- **Action:** Demonstrate that entering the wrong current password returns an immediate error message.
- **Action:** Update the password to a new value and show confirmation.
- **What to say:**  
  *"Password updates verify the caller's existing password hash via BCrypt before encoding the replacement. This gives users full runtime credential management."*

---

### Step 10: Administrator Panel (`admin.html`)
- **Action:** Open an **Incognito Window** and navigate to `http://localhost:8080/login.html`.
- **Action:** Log in with the administrator account: `admin` / configured password.
- **Action:** Walk through the sections:
  1. **Live Analytics & Charts:** Show system-wide metrics calculated in real time from MySQL (active vs. disabled users, average assessment score, simulation pass rate, category accuracy).
  2. **Incident Triage:** Locate the incident submitted in Step 8. Update its status to `UNDER_REVIEW`, add investigator notes, and update to `RESOLVED`.
  3. **Data Exports:**
     - Click **📥 Export CSV**: Open the file in Excel or Notepad. Point out the UTF-8 BOM, standard headers, and formula injection defense (dangerous characters like `=`, `+`, `-` are prefixed with `'`).
     - Click **📄 Export PDF**: Open the downloaded landscape PDF. Point out the corporate summary header, filter metadata, and formatted incident table.
  4. **Quiz Question CRUD:**
     - Show the question list.
     - Demonstrate the toggle switch (instantly activates/deactivates a question).
     - Click **Delete** on Question 1 (which was answered in Step 4). Show the safeguard error: *The server rejects hard-deletion with HTTP 400 because student answer records exist in `assessment_answers`, protecting academic grading integrity.*
  5. **Learning Module CRUD:** Show the module list, edit modal, and similar deletion safeguard.
  6. **User Management:** Show user list. Toggle a test user to disabled. Explain that the custom `UserAccountStatusFilter` immediately terminates active sessions of disabled accounts.

---

## 3. Anticipated Viva / Evaluator Questions & Clear Answers

### Q1: Why did you choose session-based authentication instead of JWT?
> **Answer:**  
> *"Session-based authentication with `HttpSession` and Spring Security was chosen because Privacy Guard is a server-rendered educational web application accessed through a browser. Sessions offer two distinct security advantages: first, built-in CSRF protection via Spring Security's `CookieCsrfTokenRepository` using double-submit cookies; second, instant server-side revocation. With JWT, invalidating a compromised or disabled user's token requires maintaining a distributed blacklist. With our session model and custom `UserAccountStatusFilter`, an administrator can disable an account and their access is terminated immediately on their very next HTTP request."*

---

### Q2: How do you prevent sensitive passwords from leaking into API responses or logs?
> **Answer:**  
> *"We maintain a strict separation between JPA entities and Data Transfer Objects (DTOs). The `User` entity has a `passwordHash` field, but `UserResponse` DTO completely omits it. Controllers never return JPA entities directly. Furthermore, passwords are never included in log statements, error payloads, or serialized JSON responses."*

---

### Q3: What happens if someone changes the administrator password in `.env`?
> **Answer:**  
> *"Environment variables in `.env` are designed for initial bootstrap provisioning. When the database is fresh (`users` table is empty), `DataSeeder` seeds the initial admin account. If `.env` is modified later, the database retains the existing BCrypt hash to prevent accidental overwrite. If disaster recovery is needed, setting `ADMIN_RECOVER=true` in `.env` instructs `DataSeeder` to force-sync the MySQL admin record to the `.env` credentials upon restart. For normal runtime updates, administrators use the authenticated `/api/auth/change-password` endpoint."*

---

### Q4: How does your CSV export protect against spreadsheet formula injection?
> **Answer:**  
> *"Spreadsheet applications like Microsoft Excel and LibreOffice execute formulas if a cell starts with `=`, `+`, `-`, `@`, `\t`, or `\r`. If an attacker submits an incident description beginning with `=cmd|' /C calc'!A0`, opening the CSV could trigger arbitrary command execution. Our `IncidentService.exportCsv()` method inspects every cell: if the first character matches any formula trigger, it prefixes an apostrophe (`'`), forcing the spreadsheet to treat the cell strictly as plain text. We also prepend the UTF-8 Byte Order Mark (`\uFEFF`) so international characters render properly."*

---

### Q5: How do you prevent foreign key errors when an administrator deletes a question or module?
> **Answer:**  
> *"In `AdminController`, before deleting a question or module, we query `assessmentAnswerRepository.existsByQuestionId(id)` and `moduleProgressRepository.existsByModuleId(id)`. If student records reference the item, the hard delete is blocked with HTTP 400 Bad Request, instructing the administrator to toggle `active = false` instead. This preserves historical grade records and audit integrity while removing the item from future student views."*

---

### Q6: How does the quiz scoring and recommendation logic work?
> **Answer:**  
> *"Each active question carries 10 points. The system sums the points earned, divides by total possible points, and computes a percentage score. It maps the score to four risk tiers: 80–100% is Low Risk, 60–79% is Medium Risk, 40–59% is High Risk, and below 40% is Critical Risk. The service inspects which categories the user missed questions in and generates targeted study recommendations linking directly to unfinished modules in those categories."*
