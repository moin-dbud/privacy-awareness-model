# 🛡️ Social Media Privacy Awareness and Security Management System

## Overview

A full-stack educational web application built with **Java 21 + Spring Boot 3.x** and **MySQL 8** that helps users understand online privacy risks, evaluate their security awareness through quizzes, access curated learning content, and report security incidents.

> **Educational Prototype** — This system does not connect to real social media accounts or collect actual credentials. It is designed for college-level learning and demonstration.

---

## ✨ Features

| Module | Description |
|---|---|
| **Authentication** | Secure registration, login, logout with BCrypt password hashing |
| **Role-Based Access** | USER and ADMIN roles with server-enforced authorization |
| **Privacy Quiz** | 15 multiple-choice questions across 6 security categories |
| **Risk Scoring** | Personalized risk levels (LOW / MEDIUM / HIGH / CRITICAL) and recommendations |
| **Learning Center** | 6 rich educational modules covering all key privacy topics |
| **Incident Reporting** | Report suspicious messages, phishing, impersonation, and more |
| **User Dashboard** | Score history, learning progress, risk overview |
| **Admin Dashboard** | Manage users, quiz questions, and incident report statuses |

### Quiz Categories
- Phishing Recognition
- Password Safety
- Privacy Settings
- Identity Theft
- Cyberbullying
- Oversharing

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Build Tool | Apache Maven 3.9+ |
| Security | Spring Security 6, BCrypt, Session-based auth, CSRF, UserAccountStatusFilter |
| Document Export | OpenPDF (com.github.librepdf:openpdf 1.3.40), RFC 4180 CSV with formula shielding |
| Database | MySQL 8.x (HikariCP connection pool) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Frontend | HTML5, CSS3 (Dark Theme Glassmorphism), Vanilla JavaScript |
| Testing | JUnit 5, H2 in-memory, PowerShell automated MySQL regression runner |
| IDE | IntelliJ IDEA / Eclipse Adoptium JDK 21 |

---

## 🔒 Security Highlights

- Passwords hashed with **BCrypt** (cost factor 12); raw passwords never logged or returned.
- **CSRF protection** via Cookie `XSRF-TOKEN` and `X-XSRF-TOKEN` header.
- **Active Session Invalidation Filter (`UserAccountStatusFilter`)**: Immediately terminates active sessions upon user deactivation or deletion.
- **Runtime Credential Management**: Authenticated self-service password changes (`/api/auth/change-password`) with bootstrap recovery mode (`ADMIN_RECOVER`).
- **CSV Formula Injection Shielding**: Escapes spreadsheet command triggers (`=`, `+`, `-`, `@`, `\t`, `\r`) in incident exports.
- **Relational Integrity Safeguards**: Protects historical quiz answers and student module progress against foreign key cascades.
- Server-side role authorization (`USER` vs `ADMIN`) enforced via `@PreAuthorize` and `SecurityFilterChain`.
- Zero stack traces or internal exception details exposed to clients.

---

## 🚀 Quick Start

See [SETUP.md](./SETUP.md) for step-by-step installation instructions.

```powershell
# 1. Load environment variables
. .\Set-Env.ps1

# 2. Start the Spring Boot application
mvn spring-boot:run

# 3. Access the portal
http://localhost:8080
```

---

## 📄 Documentation & Artifacts

| Document | Contents |
|---|---|
| [docs/SYSTEM_ARCHITECTURE.md](./docs/SYSTEM_ARCHITECTURE.md) | Full architectural diagram & data flow specifications |
| [docs/ER_DIAGRAM.md](./docs/ER_DIAGRAM.md) | Visual entity-relationship diagram covering all 11 JPA entities |
| [docs/Privacy_Guard_Presentation.pptx](./docs/Privacy_Guard_Presentation.pptx) | Professional 15-slide widescreen presentation deck |
| [PROJECT_REPORT.md](./PROJECT_REPORT.md) | Full academic and technical project report |
| [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) | Complete REST API endpoint reference and payload examples |
| [TEST_REPORT.md](./TEST_REPORT.md) | Comprehensive test summary (Unit tests + 40-test live regression suite) |
| [SETUP.md](./SETUP.md) | Step-by-step setup guide for Windows & MySQL 8 |
| [DATABASE.md](./DATABASE.md) | MySQL schema, credentials recovery, and relational safeguards |
| [.env.example](./.env.example) | Environment variable template |

---

## 👥 Default Credentials

After initial database installation, the seeder creates the administrator account:

| Role | Username | Password |
|---|---|---|
| ADMIN | Set via `ADMIN_USERNAME` in `.env` | Set via `ADMIN_PASSWORD` in `.env` (bootstrap: `Admin@1234!`) |

> Set these in your `.env` file. **Never commit real credentials.** Use the in-app **Change Password** option or `ADMIN_RECOVER=true` for credential synchronization.
