# Database Architecture & Management Guide — Privacy Guard

This guide documents the relational database design, setup procedures, entity mappings, seeding mechanisms, and integrity rules for **Privacy Guard**.

---

## 1. Database Connection Overview

Privacy Guard connects to a local **MySQL 8.x** instance using Spring Data JPA, Hibernate, and the HikariCP connection pool.

| Parameter | Configuration Property | Default Value | Description |
|---|---|---|---|
| **Host** | `DB_HOST` | `localhost` | MySQL server host |
| **Port** | `DB_PORT` | `3306` | MySQL server port |
| **Database Name** | `DB_NAME` | `privacy_awareness_db` | Application database schema |
| **Username** | `DB_USERNAME` | `privacy_user` | Dedicated application user |
| **Password** | `DB_PASSWORD` | Set via `.env` | User password |
| **DDL Auto** | `JPA_DDL_AUTO` | `update` | Hibernate schema creation/update mode |
| **Character Set** | - | `utf8mb4` | Full Unicode support including emojis |
| **Collation** | - | `utf8mb4_unicode_ci` | Case-insensitive Unicode collation |

The connection URL generated in `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:privacy_awareness_db}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
```

---

## 2. Setting Up MySQL

Connect to your MySQL server as `root` using MySQL Workbench or the CLI, and execute:

```sql
-- 1. Create the database
CREATE DATABASE IF NOT EXISTS privacy_awareness_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Create the dedicated application user
CREATE USER IF NOT EXISTS 'privacy_user'@'localhost' IDENTIFIED BY 'YourChosenPassword';

-- 3. Grant privileges on the application database only (Least Privilege Principle)
GRANT ALL PRIVILEGES ON privacy_awareness_db.* TO 'privacy_user'@'localhost';

-- 4. Apply changes
FLUSH PRIVILEGES;
```

---

## 3. Database Tables & Schema Reference

When the application starts with `spring.jpa.hibernate.ddl-auto=update`, Hibernate verifies and generates 11 tables corresponding to the JPA entity classes:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        privacy_awareness_db Schema                     │
├───────────────────────────────────┬────────────────────────────────────┤
│ Table Name                        │ Associated JPA Entity Class        │
├───────────────────────────────────┼────────────────────────────────────┤
│ 1. users                          │ com.privacy.entity.User            │
│ 2. quiz_questions                 │ com.privacy.entity.QuizQuestion    │
│ 3. quiz_options                   │ com.privacy.entity.QuizOption      │
│ 4. assessment_results             │ com.privacy.entity.AssessmentResult│
│ 5. assessment_answers             │ com.privacy.entity.AssessmentAnswer│
│ 6. learning_modules               │ com.privacy.entity.LearningModule  │
│ 7. module_progress                │ com.privacy.entity.ModuleProgress  │
│ 8. incident_reports               │ com.privacy.entity.IncidentReport  │
│ 9. phishing_scenarios             │ com.privacy.entity.PhishingScenario│
│ 10. phishing_red_flags            │ com.privacy.entity.PhishingRedFlag │
│ 11. simulation_results            │ com.privacy.entity.SimulationResult│
└───────────────────────────────────┴────────────────────────────────────┘
```

---

### 3.1 `users` Table
Stores user accounts, authentication credentials, and system roles.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Unique user identifier |
| `username` | `VARCHAR(50)` | `NOT NULL UNIQUE` | Unique username (3–50 chars) |
| `email` | `VARCHAR(100)` | `NOT NULL UNIQUE` | Unique email address |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | BCrypt-hashed password (never plaintext) |
| `full_name` | `VARCHAR(100)` | `NULLABLE` | Display name of user |
| `role` | `VARCHAR(20)` | `NOT NULL` | `USER` or `ADMIN` |
| `enabled` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Active/deactivated status |
| `created_at` | `DATETIME(6)` | `NOT NULL` | Account registration timestamp |

---

### 3.2 `quiz_questions` & `quiz_options` Tables
Store the educational assessment quiz curriculum.

**`quiz_questions`**:
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Question identifier |
| `question_text` | `VARCHAR(1000)` | `NOT NULL` | The question prompt |
| `category` | `VARCHAR(30)` | `NOT NULL` | Category (`PHISHING`, `PASSWORD_SAFETY`, etc.) |
| `points` | `INT` | `NOT NULL DEFAULT 10` | Point value awarded for correct answer |
| `active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Whether question is included in quizzes |
| `explanation` | `VARCHAR(2000)` | `NULLABLE` | Educational explanation revealed after submission |

**`quiz_options`**:
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Option identifier |
| `question_id` | `BIGINT` | `NOT NULL, FK -> quiz_questions(id)` | Parent question |
| `option_text` | `VARCHAR(500)` | `NOT NULL` | Answer choice text |
| `correct` | `BOOLEAN` | `NOT NULL` | Indicates whether option is the correct answer |
| `display_order` | `INT` | `NOT NULL DEFAULT 1` | Presentation ordering |

---

### 3.3 `assessment_results` & `assessment_answers` Tables
Store student quiz submissions and detailed historical answers.

**`assessment_results`**:
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Result attempt identifier |
| `user_id` | `BIGINT` | `NOT NULL, FK -> users(id)` | Student who completed the quiz |
| `total_score` | `INT` | `NOT NULL` | Total points earned |
| `max_possible_score`| `INT` | `NOT NULL` | Maximum points available on active questions |
| `percentage_score` | `DOUBLE` | `NOT NULL` | Calculated percentage score |
| `risk_level` | `VARCHAR(20)` | `NOT NULL` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `correct_answers` | `INT` | `NOT NULL` | Count of correctly answered questions |
| `total_questions` | `INT` | `NOT NULL` | Total questions evaluated |
| `recommendations` | `VARCHAR(2000)` | `NULLABLE` | Tailored study advice based on missed topics |
| `taken_at` | `DATETIME(6)` | `NOT NULL` | Timestamp of completion |

**`assessment_answers`**:
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Answer item identifier |
| `result_id` | `BIGINT` | `NOT NULL, FK -> assessment_results(id)` | Parent assessment result |
| `question_id` | `BIGINT` | `NOT NULL, FK -> quiz_questions(id)` | Question answered |
| `selected_option_id`| `BIGINT` | `NOT NULL, FK -> quiz_options(id)` | Option chosen by user |
| `correct` | `BOOLEAN` | `NOT NULL` | Whether user's selection was correct |
| `points_earned` | `INT` | `NOT NULL` | Points earned on this specific item |

---

### 3.4 `learning_modules` & `module_progress` Tables
Manage the educational curriculum and user reading completion state.

**`learning_modules`**:
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Module identifier |
| `title` | `VARCHAR(200)` | `NOT NULL` | Educational module title |
| `category` | `VARCHAR(30)` | `NOT NULL` | Threat topic category |
| `description` | `VARCHAR(500)` | `NULLABLE` | Short synopsis |
| `content` | `LONGTEXT` | `NOT NULL` | Rich text / HTML educational content |
| `estimated_minutes` | `INT` | `NOT NULL DEFAULT 5` | Estimated reading duration |
| `display_order` | `INT` | `NOT NULL DEFAULT 1` | Catalog presentation order |
| `active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Active visibility flag |

**`module_progress`**:
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Progress identifier |
| `user_id` | `BIGINT` | `NOT NULL, FK -> users(id)` | User tracking progress |
| `module_id` | `BIGINT` | `NOT NULL, FK -> learning_modules(id)` | Module completed |
| `completed` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Completion flag |
| `completed_at` | `DATETIME(6)` | `NOT NULL` | Timestamp of completion |
| - | - | `UNIQUE (user_id, module_id)` | Prevents duplicate completion entries |

---

### 3.5 `incident_reports` Table
Stores privacy incidents reported by users for administrative review.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Incident report identifier |
| `user_id` | `BIGINT` | `NOT NULL, FK -> users(id)` | Submitting user |
| `title` | `VARCHAR(200)` | `NOT NULL` | Incident title summary |
| `description` | `VARCHAR(2000)` | `NOT NULL` | Detailed incident narrative |
| `incident_type` | `VARCHAR(30)` | `NOT NULL` | Type (`PHISHING`, `DATA_LEAK`, etc.) |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'OPEN'` | `OPEN`, `UNDER_REVIEW`, `RESOLVED`, `DISMISSED` |
| `reported_at` | `DATETIME(6)` | `NOT NULL` | Submission timestamp |
| `admin_notes` | `VARCHAR(2000)` | `NULLABLE` | Resolution notes added by administrator |

---

### 3.6 `phishing_scenarios`, `phishing_red_flags`, & `simulation_results` Tables
Manage realistic simulated phishing exercises.

**`phishing_scenarios`**:
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Scenario identifier |
| `title` | `VARCHAR(200)` | `NOT NULL` | Scenario title |
| `channel` | `VARCHAR(20)` | `NOT NULL` | `EMAIL`, `SMS`, `DIRECT_MESSAGE` |
| `sender` | `VARCHAR(200)` | `NOT NULL` | Deceptive sender address/number |
| `subject` | `VARCHAR(300)` | `NULLABLE` | Subject line (for email scenarios) |
| `content` | `LONGTEXT` | `NOT NULL` | Raw lure content |
| `difficulty` | `VARCHAR(20)` | `NOT NULL` | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |
| `explanation` | `VARCHAR(2000)` | `NOT NULL` | Educational debrief explaining attack tactics |
| `active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Scenario active status |

**`phishing_red_flags`**:
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Red flag identifier |
| `scenario_id` | `BIGINT` | `NOT NULL, FK -> phishing_scenarios(id)` | Associated scenario |
| `flag_text` | `VARCHAR(500)` | `NOT NULL` | Indicator description |
| `description` | `VARCHAR(1000)` | `NOT NULL` | Educational explanation |
| `correct` | `BOOLEAN` | `NOT NULL` | True if legitimate red flag; false if distractor |
| `display_order` | `INT` | `NOT NULL DEFAULT 1` | Option ordering |

**`simulation_results`**:
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Simulation attempt ID |
| `user_id` | `BIGINT` | `NOT NULL, FK -> users(id)` | User who took the simulation |
| `scenario_id` | `BIGINT` | `NOT NULL, FK -> phishing_scenarios(id)` | Scenario attempted |
| `score` | `INT` | `NOT NULL` | Points earned (0–100) |
| `passed` | `BOOLEAN` | `NOT NULL` | True if all correct flags identified |
| `feedback` | `VARCHAR(2000)` | `NULLABLE` | Tailored feedback |
| `completed_at` | `DATETIME(6)` | `NOT NULL` | Attempt timestamp |

---

## 4. Data Seeding & Initialization

Data initialization is handled automatically by [DataSeeder.java](src/main/java/com/privacy/config/DataSeeder.java) (implementing `CommandLineRunner`).

When the server boots up:
1. **Admin User Seeding:**
   - On a fresh database (`userRepository.count() == 0`), creates the default administrator account from `.env` values.
   - If the database is already populated, it will not auto-recreate a missing admin, preventing deleted accounts from reappearing.
   - If `ADMIN_RECOVER=true` is set in `.env`, it updates the administrator's password to the value configured in `.env`.
2. **Quiz Questions:**
   - Checks if `quiz_questions` has records. If empty, seeds 15 questions with 4 options each across 6 privacy categories.
3. **Learning Modules:**
   - Checks if `learning_modules` has records. If empty, seeds 6 rich educational modules.
4. **Phishing Scenarios:**
   - Checks if `phishing_scenarios` has records. If empty, seeds 10 realistic email, SMS, and direct message phishing exercises with red flags and distractors.

---

## 5. Relational Integrity & Deletion Safeguards

The database schema and business logic enforce relational integrity:

1. **Cascade User Deletion:**
   When an administrator deletes a user via `DELETE /api/admin/users/{id}`, JPA cascades the deletion to all records owned by that user (`assessment_results`, `assessment_answers`, `incident_reports`, `module_progress`, `simulation_results`).
2. **Quiz Question Protection:**
   Administrators cannot hard-delete a question if it has been answered in past student assessments (`assessmentAnswerRepository.existsByQuestionId(id)`). The API returns HTTP 400 Bad Request, instructing staff to deactivate the question using the toggle switch. This preserves historical grade records.
3. **Learning Module Protection:**
   Administrators cannot hard-delete a module if student progress records exist in `module_progress`. The API returns HTTP 400 Bad Request, instructing staff to deactivate the module instead.
4. **Active Session Invalidation Filter:**
   When a user account is deleted or disabled (`enabled = false`) in MySQL, `UserAccountStatusFilter` detects the change on the user's next request, invalidates the active `HttpSession`, and returns HTTP 401 Unauthorized.

---

## 6. Verification Queries in MySQL

You can verify that tables and seed data are populated by running these queries in MySQL Workbench:

```sql
USE privacy_awareness_db;

-- 1. Verify table counts
SELECT 'users' AS tbl, COUNT(*) AS total FROM users
UNION ALL SELECT 'quiz_questions', COUNT(*) FROM quiz_questions
UNION ALL SELECT 'quiz_options', COUNT(*) FROM quiz_options
UNION ALL SELECT 'learning_modules', COUNT(*) FROM learning_modules
UNION ALL SELECT 'phishing_scenarios', COUNT(*) FROM phishing_scenarios
UNION ALL SELECT 'phishing_red_flags', COUNT(*) FROM phishing_red_flags
UNION ALL SELECT 'assessment_results', COUNT(*) FROM assessment_results
UNION ALL SELECT 'incident_reports', COUNT(*) FROM incident_reports;

-- 2. Check administrator account
SELECT id, username, email, role, enabled, created_at FROM users WHERE role = 'ADMIN';

-- 3. Verify that all stored passwords are BCrypt hashes (starting with $2a$ or $2b$)
SELECT COUNT(*) AS non_bcrypt_passwords 
FROM users 
WHERE password_hash NOT LIKE '$2a$%' AND password_hash NOT LIKE '$2b$%';
```
*(The third query should return `0`, verifying that no plaintext passwords exist in the database).*