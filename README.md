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
| Security | Spring Security, BCrypt, Session-based auth, CSRF |
| Database | MySQL 8.x |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Frontend | HTML5, CSS3, Vanilla JavaScript |
| Testing | JUnit 5, H2 in-memory |
| IDE | IntelliJ IDEA (recommended) |

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/privacy/
│   │   ├── PrivacyAwarenessApplication.java   # Entry point
│   │   ├── config/
│   │   │   ├── SecurityConfig.java             # Spring Security
│   │   │   ├── CustomUserDetailsService.java   # User loading
│   │   │   └── DataSeeder.java                 # DB seed on startup
│   │   ├── entity/          # JPA Entities
│   │   ├── repository/      # Spring Data JPA repos
│   │   ├── service/         # Business logic
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Request/Response DTOs
│   │   └── exception/       # Global error handling
│   └── resources/
│       ├── application.properties
│       └── static/          # Frontend (HTML/CSS/JS)
│           ├── index.html
│           ├── login.html
│           ├── register.html
│           ├── dashboard.html
│           ├── quiz.html
│           ├── learning.html
│           ├── incidents.html
│           ├── admin.html
│           ├── css/style.css
│           └── js/app.js
└── test/
    └── java/com/privacy/
        └── PrivacyAwarenessApplicationTests.java
```

---

## 🔒 Security Highlights

- Passwords hashed with **BCrypt** (cost factor 12)
- **CSRF protection** via Cookie XSRF-TOKEN
- Session-based authentication (no JWT)
- Server-side role authorization — not just frontend gating
- Users can only access **their own** data
- Validation on all inputs (server and client)
- No stack traces exposed to clients
- Admin endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`

---

## 🚀 Quick Start

See [SETUP.md](./SETUP.md) for step-by-step installation instructions.

```bash
# Set environment variables, then:
mvn spring-boot:run
# Visit http://localhost:8080
```

---

## 📄 Documentation

| File | Contents |
|---|---|
| [SETUP.md](./SETUP.md) | Installation and run instructions (Windows) |
| [DATABASE.md](./DATABASE.md) | MySQL setup and schema details |
| [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) | REST API reference |
| [PROJECT_REPORT.md](./PROJECT_REPORT.md) | Full academic project report |
| [.env.example](./.env.example) | Environment variable template |

---

## 👥 Default Credentials

After first run, the seeder creates:

| Role | Username | Password |
|---|---|---|
| ADMIN | Set via `ADMIN_USERNAME` env var | Set via `ADMIN_PASSWORD` env var |

> Set these in your `.env` file. **Never commit real credentials.**
