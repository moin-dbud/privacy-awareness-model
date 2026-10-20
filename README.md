# Privacy Guard

**Privacy Guard** is a full-stack educational web application designed to help social media users identify privacy risks, assess their security habits, and learn defensive behaviors. The application includes interactive quizzes with personalized recommendations, categorized learning modules, a realistic phishing simulator, a password strength checker, and an incident reporting portal. It also provides an administrative dashboard for user management, educational content administration, incident triage, and report exports.

The project is built with **Java 21**, **Spring Boot 3.2.5**, and **MySQL 8**, using Spring Security for session-based authentication and a vanilla HTML5/CSS3/JavaScript frontend with a dark theme.

---

## The Problem

Social media platforms are common vectors for account takeover, identity theft, phishing campaigns, and harassment. Most users understand that online threats exist, but struggle to identify specific warning signs in messages, evaluate password strength accurately, or configure account privacy settings safely. Generic compliance training often fails to engage users or provide direct feedback on their individual risk areas.

Privacy Guard addresses this by combining practical awareness tests, hands-on phishing detection exercises, and targeted learning resources into a single self-assessment platform.

---

## Features

### User Features
- **Account Management:** User registration, session-based login, and authenticated password changes.
- **Privacy Assessment Quiz:** 15 multiple-choice questions across 6 privacy categories with instant explanations, percentage scoring, and risk tier classification.
- **Personalized Recommendations:** Dynamic guidance based on assessment performance, pointing users to specific learning modules matching their weak areas.
- **Learning Center:** 6 categorized educational modules covering phishing, password safety, privacy settings, identity theft, cyberbullying, and oversharing.
- **Phishing Simulator:** 10 realistic email, SMS, and direct message phishing scenarios where users identify specific red flags and receive immediate scoring.
- **Password Strength Checker:** Interactive tool evaluating password length, character diversity, common patterns, entropy, and estimated crack times.
- **Incident Reporting:** User reporting portal for privacy incidents (phishing, impersonation, leaks, harassment) with status tracking.
- **Personal Dashboard:** Summary metrics showing assessment history, score trends, module completion progress, and incident reports.

### Administrator Features
- **System Metrics & Analytics:** Real-time database metrics showing active users, average quiz scores, simulation pass rates, category accuracy breakdowns, and incident statistics.
- **User Management:** Search and filter users, update roles (`USER` / `ADMIN`), activate/deactivate accounts, and delete users with full relational cascade.
- **Incident Triage:** Review incoming reports, update statuses (`OPEN`, `UNDER_REVIEW`, `RESOLVED`, `DISMISSED`), add administrative notes, and filter by status or incident type.
- **Report Exports:** Export incident reports to RFC 4180 CSV (with spreadsheet formula injection protection) or landscape PDF (generated via OpenPDF).
- **Quiz Question Administration:** Full CRUD management for assessment questions, category assignment, point weights, options, and active/inactive toggles, protected by deletion safeguards if historical answers exist.
- **Learning Module Administration:** Full CRUD management for curriculum modules, display order, estimated reading times, body content, and active/inactive toggles, protected by deletion safeguards if user progress exists.

---

## Technology Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | JDK 21 | Core backend programming language |
| Framework | Spring Boot | 3.2.5 | Application framework and dependency injection |
| Security | Spring Security | 6.2.x | Authentication, authorization, session management, CSRF |
| Persistence | Spring Data JPA / Hibernate | 6.4.x | Object-relational mapping and repository abstraction |
| Database | MySQL Community Server | 8.0.x | Relational database storage |
| Connection Pool | HikariCP | Built-in | Database connection pooling |
| PDF Generation | OpenPDF | 1.3.40 | Formatted administrative incident report exports |
| Frontend | HTML5, CSS3, Vanilla JavaScript | - | Dark-themed responsive user interface (no external UI framework) |
| Testing | JUnit 5, Spring Boot Test, H2 | - | Automated unit, controller, and repository test suites |

---

## Prerequisites

Before running the project, ensure you have the following installed on your machine:
- **Java Development Kit (JDK) 21** or higher.
- **Apache Maven 3.9** or higher.
- **MySQL Community Server 8.0** (running locally on port 3306).
- A modern web browser (Chrome, Edge, Firefox).

---

## Quick Start (Windows PowerShell)

1. **Clone or navigate to the project directory:**
   ```powershell
   cd "C:\Users\my\Downloads\Phishing System"
   ```

2. **Configure environment variables:**
   Copy the sample environment file to `.env`:
   ```powershell
   Copy-Item .env.example .env
   ```
   Open `.env` and set your local MySQL password for `DB_PASSWORD`:
   ```env
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=privacy_awareness_db
   DB_USERNAME=privacy_user
   DB_PASSWORD=YourLocalPasswordHere
   SERVER_PORT=8080
   ADMIN_USERNAME=admin
   ADMIN_PASSWORD=Admin@1234!
   ADMIN_EMAIL=admin@privacysystem.local
   ```

3. **Set up the MySQL database:**
   Log into MySQL as `root` and run:
   ```sql
   CREATE DATABASE IF NOT EXISTS privacy_awareness_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER IF NOT EXISTS 'privacy_user'@'localhost' IDENTIFIED BY 'YourLocalPasswordHere';
   GRANT ALL PRIVILEGES ON privacy_awareness_db.* TO 'privacy_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

4. **Load environment variables and start the server:**
   ```powershell
   . .\Set-Env.ps1
   mvn spring-boot:run
   ```
   *(Note the dot and space before `.\Set-Env.ps1` to export variables into the active PowerShell session).*

5. **Open the application:**
   - URL: [http://localhost:8080](http://localhost:8080)
   - Initial Administrator Account: Username `admin`, Password `Admin@1234!` (seeded from `.env` on first startup).
   - Regular User: Click **Register** on the login screen to create a new personal account.

---

## Running Tests

- **Run Maven Unit and Slice Tests (In-Memory H2):**
  ```powershell
  mvn clean test
  ```
  Executes 15 tests covering user management, admin analytics, dashboard metrics, phishing simulation logic, and context startup.

- **Run Live MySQL Integration Suite:**
  ```powershell
  . .\Set-Env.ps1
  .\test-runner.ps1
  ```
  Executes 40 end-to-end integration tests verifying authentication, session invalidation filters, quiz evaluations, learning modules, incident workflows, CSV/PDF exports, and relational database integrity against the live MySQL instance.

---

## Project Documentation Index

Detailed documentation for evaluators, developers, and teammates is available in the repository:

| Document | Description |
|---|---|
| [SETUP.md](./SETUP.md) | Step-by-step installation, database setup, environment configuration, and troubleshooting guide. |
| [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) | Exhaustive REST API endpoint reference with request bodies, query parameters, responses, and error codes. |
| [DATABASE.md](./DATABASE.md) | Database schema details, table structures, foreign key relationships, seeding mechanics, and verification queries. |
| [PROJECT_REPORT.md](./PROJECT_REPORT.md) | Formal academic project report covering problem background, architecture, security analysis, scoring models, and evaluation. |
| [TEST_REPORT.md](./TEST_REPORT.md) | Comprehensive test report documenting test environments, unit test cases, and the 40-test live integration suite. |
| [docs/SYSTEM_ARCHITECTURE.md](./docs/SYSTEM_ARCHITECTURE.md) | Architectural documentation with Mermaid request flow and component interaction diagrams. |
| [docs/ER_DIAGRAM.md](./docs/ER_DIAGRAM.md) | Visual entity-relationship diagram with cardinalities and cascade constraints for all 11 database entities. |
| [docs/DEMO_GUIDE.md](./docs/DEMO_GUIDE.md) | Step-by-step demonstration walkthrough for viva, college project review, and presentations. |
| [docs/TEAM_HANDOVER.md](./docs/TEAM_HANDOVER.md) | Practical onboarding guide for teammates explaining code structure, workflows, and extension guidelines. |

---

## Current Scope & Limitations

1. **Self-Contained Educational Prototype:** The system does not integrate directly with third-party social media APIs (e.g., Meta, X, LinkedIn) or mail servers. Simulations and questions are self-contained.
2. **Session Storage:** Authentication relies on standard HTTP sessions stored in server memory; multi-instance horizontal scaling would require external session stores (such as Redis).
3. **No Automated Email Dispatch:** Password resets and incident status notifications occur within the application rather than via outbound SMTP email. Users reset passwords directly through the authenticated profile interface or via administrator recovery mode (`ADMIN_RECOVER=true`).
