# Privacy Guard — Database Entity Relationship (ER) Diagram

This document illustrates the relational database structure for **Privacy Guard**, mapping all 11 JPA entities directly to their MySQL tables.

---

## 1. Visual Entity-Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ ASSESSMENT_RESULTS : "takes"
    USERS ||--o{ MODULE_PROGRESS : "completes"
    USERS ||--o{ INCIDENT_REPORTS : "submits"
    USERS ||--o{ SIMULATION_RESULTS : "attempts"

    QUIZ_QUESTIONS ||--|{ QUIZ_OPTIONS : "contains"
    QUIZ_QUESTIONS ||--o{ ASSESSMENT_ANSWERS : "evaluated_in"
    QUIZ_OPTIONS ||--o{ ASSESSMENT_ANSWERS : "selected_in"

    ASSESSMENT_RESULTS ||--|{ ASSESSMENT_ANSWERS : "records"

    LEARNING_MODULES ||--o{ MODULE_PROGRESS : "tracked_in"

    PHISHING_SCENARIOS ||--|{ PHISHING_RED_FLAGS : "defines"
    PHISHING_SCENARIOS ||--o{ SIMULATION_RESULTS : "simulated_in"

    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar role
        boolean enabled
        datetime created_at
    }

    QUIZ_QUESTIONS {
        bigint id PK
        varchar question_text
        varchar category
        int points
        boolean active
        varchar explanation
    }

    QUIZ_OPTIONS {
        bigint id PK
        bigint question_id FK
        varchar option_text
        boolean correct
        int display_order
    }

    ASSESSMENT_RESULTS {
        bigint id PK
        bigint user_id FK
        int total_score
        int max_possible_score
        int correct_answers
        int total_questions
        double percentage_score
        varchar risk_level
        varchar recommendations
        datetime taken_at
    }

    ASSESSMENT_ANSWERS {
        bigint id PK
        bigint result_id FK
        bigint question_id FK
        bigint selected_option_id FK
        boolean correct
        int points_earned
    }

    LEARNING_MODULES {
        bigint id PK
        varchar title
        varchar category
        varchar description
        longtext content
        int estimated_minutes
        int display_order
        boolean active
    }

    MODULE_PROGRESS {
        bigint id PK
        bigint user_id FK
        bigint module_id FK
        boolean completed
        datetime completed_at
    }

    INCIDENT_REPORTS {
        bigint id PK
        bigint user_id FK
        varchar title
        varchar description
        varchar incident_type
        varchar status
        varchar admin_notes
        datetime reported_at
    }

    PHISHING_SCENARIOS {
        bigint id PK
        varchar title
        varchar channel
        varchar sender
        varchar subject
        longtext content
        varchar difficulty
        varchar explanation
        boolean active
    }

    PHISHING_RED_FLAGS {
        bigint id PK
        bigint scenario_id FK
        varchar flag_text
        varchar description
        boolean correct
        int display_order
    }

    SIMULATION_RESULTS {
        bigint id PK
        bigint user_id FK
        bigint scenario_id FK
        int score
        boolean passed
        varchar feedback
        datetime completed_at
    }
```

---

## 2. Key Entity Descriptions

1. **`USERS`**: Core identity table storing user credentials, BCrypt hashes, account status (`enabled`), and authorization roles (`USER`, `ADMIN`).
2. **`QUIZ_QUESTIONS` & `QUIZ_OPTIONS`**: The privacy questionnaire catalog. Each question contains at least 2 options, with exactly one designated as correct.
3. **`ASSESSMENT_RESULTS` & `ASSESSMENT_ANSWERS`**: Track quiz attempts. Results record total scores, percentages, and risk classifications (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). Answers record the specific choice submitted for each question and the points awarded.
4. **`LEARNING_MODULES` & `MODULE_PROGRESS`**: Structured educational articles categorized by threat topic. `MODULE_PROGRESS` maintains a unique constraint on `(user_id, module_id)` ensuring accurate completion tracking.
5. **`INCIDENT_REPORTS`**: User-submitted security incident reports. Supports status transitions (`OPEN` -> `UNDER_REVIEW` -> `RESOLVED` / `DISMISSED`) and administrator triage notes.
6. **`PHISHING_SCENARIOS`, `PHISHING_RED_FLAGS` & `SIMULATION_RESULTS`**: Realistic social engineering exercises across email, SMS, and direct messages. Users earn points by identifying genuine red flags without selecting deceptive distractors.

---

## 3. Referential Integrity & Deletion Policies

- **Cascading User Deletion**: Deleting a user account cascades to all assessment results, answers, incident reports, module progress, and simulation records.
- **Relational Protection for Questions**: A quiz question cannot be hard-deleted if it is referenced in past `ASSESSMENT_ANSWERS`. The application rejects deletion with HTTP 400 Bad Request, directing the administrator to deactivate the question instead to protect student history.
- **Relational Protection for Modules**: A learning module cannot be hard-deleted if users have recorded completion progress in `MODULE_PROGRESS`. The application rejects deletion with HTTP 400 Bad Request, advising administrators to toggle it inactive instead.
