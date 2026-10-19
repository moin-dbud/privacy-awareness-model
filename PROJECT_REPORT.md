# PROJECT REPORT

## Social Media Privacy Awareness and Security Management System

**Course:** Information Security / Web Application Development  
**Technology Stack:** Java 21 · Spring Boot 3.x · MySQL 8 · HTML/CSS/JavaScript  

---

## Abstract

This project presents a Social Media Privacy Awareness and Security Management System — a full-stack educational web application that addresses the growing risks associated with social media usage. The system educates users about online privacy threats through interactive quizzes and curated learning modules, evaluates their security awareness with personalised risk scoring, enables security incident reporting, and provides administrative tools to manage educational content and user-reported incidents. The application is built using Java 21, Spring Boot 3, Spring Security, JPA/Hibernate with MySQL, and a responsive vanilla HTML/CSS/JavaScript frontend.

---

## 1. Problem Statement

Social media platforms have become integral to daily communication, yet most users lack awareness of the privacy risks they entail. Threats such as phishing, identity theft, cyberbullying, and account compromise are increasing in frequency and sophistication. Traditional security training is often inaccessible, abstract, or one-size-fits-all. There is a need for an interactive, personalised educational tool that evaluates individual awareness levels and provides targeted guidance.

---

## 2. Objectives

1. Develop a secure, role-based web application for privacy education
2. Implement an interactive privacy awareness quiz with risk scoring
3. Provide categorised educational content covering the six primary social media threat areas
4. Enable users to report security incidents and track their status
5. Build an administrative panel for content and incident management
6. Apply industry-standard security practices (BCrypt, CSRF, session auth, input validation)
7. Persist all data in a normalised MySQL database

---

## 3. Functional Requirements

| ID | Requirement |
|----|-------------|
| FR-01 | Users shall register with a validated username, email, and password |
| FR-02 | Passwords shall be hashed using BCrypt before storage |
| FR-03 | Users shall log in via username and password with session-based auth |
| FR-04 | The system shall enforce USER and ADMIN roles server-side |
| FR-05 | The quiz shall present multiple-choice questions across 6 categories |
| FR-06 | Submission shall calculate score, risk level, and personalised recommendations |
| FR-07 | Users shall access per-question feedback and explanations after submission |
| FR-08 | The learning center shall provide rich educational content for each threat category |
| FR-09 | Users shall mark learning modules as complete and track their progress |
| FR-10 | Users shall submit security incident reports with type, title, and description |
| FR-11 | Admins shall update incident report statuses and add notes |
| FR-12 | Admins shall toggle quiz questions active/inactive |
| FR-13 | The user dashboard shall display assessment history, latest score, and learning progress |
| FR-14 | The database shall be seeded with sample questions and content on first run |

---

## 4. Non-Functional Requirements

| Requirement | Implementation |
|-------------|----------------|
| Security | BCrypt passwords, CSRF tokens, server-side role enforcement |
| Privacy | No plain-text passwords stored; no sensitive data in API responses |
| Usability | Responsive dark-theme UI with clear navigation |
| Maintainability | Layered architecture (Entity/Repository/Service/Controller/DTO) |
| Testability | Context load test using H2 in-memory database |
| Portability | All secrets in environment variables; no hardcoded credentials |

---

## 5. System Architecture

The application follows a standard Spring Boot layered architecture:

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (Browser)                       │
│    HTML5 + CSS3 + Vanilla JS  ← fetch() API calls →             │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP / HTTPS
┌────────────────────────────▼────────────────────────────────────┐
│                    Spring Boot Application                        │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │  Controller  │→ │   Service    │→ │      Repository         │ │
│  │  (REST API)  │  │ (Business    │  │  (Spring Data JPA)     │ │
│  └──────────────┘  │  Logic)      │  └────────────┬───────────┘ │
│                    └──────────────┘               │             │
│  ┌──────────────┐  ┌──────────────┐               │             │
│  │  Spring      │  │   DTOs       │            Hibernate        │
│  │  Security    │  │  (Request /  │               │             │
│  │  (Auth/CSRF) │  │   Response)  │               │             │
│  └──────────────┘  └──────────────┘               │             │
└───────────────────────────────────────────────────┼─────────────┘
                                                    │
┌───────────────────────────────────────────────────▼─────────────┐
│                          MySQL 8 Database                         │
│    users · quiz_questions · quiz_options · assessment_results    │
│    assessment_answers · learning_modules · module_progress       │
│    incident_reports                                              │
└─────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

- **Session-based authentication** chosen over JWT for simplicity and to leverage Spring Security's built-in CSRF support
- **Hibernate DDL auto-update** reduces setup friction for a college project
- **DataSeeder as CommandLineRunner** ensures idempotent seed data without SQL scripts
- **DTOs separate from entities** prevents accidental exposure of sensitive fields (e.g. `passwordHash`)
- **@PreAuthorize** on admin controller adds method-level security in addition to URL-level security

---

## 6. Database Design

### Entity Relationship Summary

```
users (1) ──────────────── (N) assessment_results
users (1) ──────────────── (N) module_progress
users (1) ──────────────── (N) incident_reports
assessment_results (1) ─── (N) assessment_answers
quiz_questions (1) ──────── (N) quiz_options
quiz_questions (1) ──────── (N) assessment_answers
quiz_options (1) ────────── (N) assessment_answers
learning_modules (1) ────── (N) module_progress
```

### Important Constraints

- `users.username` and `users.email` have UNIQUE constraints
- `module_progress(user_id, module_id)` has a composite UNIQUE constraint
- All passwords stored as BCrypt hashes (never plain text)
- All foreign keys use LAZY loading where appropriate for performance

---

## 7. Security Implementation

### Authentication Flow
1. User submits credentials via `POST /api/auth/login`
2. Spring Security's `DaoAuthenticationProvider` calls `CustomUserDetailsService`
3. BCrypt verifies the password hash
4. On success, Spring Security creates an `HttpSession` and sets `JSESSIONID` cookie
5. All subsequent requests include the session cookie automatically

### CSRF Protection
- Cookie-based CSRF using `CookieCsrfTokenRepository`
- `XSRF-TOKEN` cookie set on first request
- Mutating requests must include `X-XSRF-TOKEN` header
- Frontend JavaScript reads the cookie and sets the header automatically

### Authorization Layers
- URL-level: `HttpSecurity.authorizeHttpRequests()` in `SecurityConfig`
- Method-level: `@PreAuthorize("hasRole('ADMIN')")` on `AdminController`
- Data-level: services verify `user.id` ownership before returning data

---

## 8. Quiz Scoring Algorithm

```
percentageScore = (totalScore / maxPossibleScore) × 100

Risk Level:
  ≥ 80% → LOW      (Strong awareness)
  60–79% → MEDIUM  (Room for improvement)
  40–59% → HIGH    (Needs attention)
  < 40%  → CRITICAL (Urgent improvement needed)

Recommendations are generated based on:
  1. Overall risk level (introductory message)
  2. Categories of incorrectly answered questions (targeted topics)
```

---

## 9. Testing

### Unit / Integration Tests

`PrivacyAwarenessApplicationTests` verifies that the Spring application context loads successfully using an in-memory H2 database. This confirms that:
- All beans are correctly defined
- No circular dependencies exist
- DataSeeder runs without errors
- JPA entity mappings are valid

### Manual Testing Workflow

1. Register a new user account
2. Login and view the empty dashboard
3. Take the privacy quiz — submit all 15 answers
4. Review the result, risk level, and recommendations
5. Visit the learning center — open and complete modules
6. Check dashboard for updated progress
7. Submit an incident report
8. Login as admin — verify incident appears in admin dashboard
9. Update incident status and add admin notes
10. Verify updated status is visible in the user's incident list

---

## 10. Limitations

1. **Educational prototype only** — does not integrate with real social media platforms
2. **No email notifications** — incident status updates are not emailed to users
3. **No password reset** — users must contact the admin to reset passwords
4. **In-memory sessions** — not suitable for multi-instance deployment without session sharing
5. **H2 for tests only** — production requires MySQL; test coverage is limited to context loading
6. **Static quiz questions** — no UI for admins to create new questions (can only activate/deactivate)
7. **Single admin** — no mechanism to promote regular users to admin via the UI

---

## 11. Future Scope

1. **Email notifications** via Spring Mail for incident status updates and password resets
2. **JWT authentication** for stateless API usage and mobile app compatibility
3. **Admin CRUD for quiz questions** — full create/edit/delete interface
4. **Password reset flow** via email token
5. **Quiz categories filter** — allow users to take targeted quizzes per topic
6. **Leaderboard** — anonymised ranking of top quiz scores
7. **Social media API simulation** — mock integration to demonstrate privacy settings review
8. **Export to PDF** — allow users to download their assessment reports
9. **Multi-language support** — i18n for broader accessibility
10. **Containerisation** — Docker Compose setup for easy deployment

---

## 12. Conclusion

This project demonstrates practical application of full-stack web development with Java Spring Boot, incorporating security best practices, database design, REST API architecture, and modern frontend techniques. The system achieves its primary objective of educating users about social media privacy risks through an engaging, interactive platform while maintaining appropriate security controls for user data protection.
