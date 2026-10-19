# Setup Guide (Windows) — Social Media Privacy Awareness & Security Management System

This document provides step-by-step instructions to set up, build, configure, and run the project on Windows 10/11.

---

## 1. Environment Verification

The project requires **Java JDK 21**, **Apache Maven 3.9+**, and **MySQL 8.x**.

### Installed Tooling on this Machine:
- **Java JDK 21**: `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` (`JAVA_HOME` is pre-configured).
- **Apache Maven 3.10.0**: `C:\tools\apache-maven-3.10.0\bin\mvn.cmd`.
- **MySQL 8.0**: `C:\Program Files\MySQL\MySQL Server 8.0` (Service: `MySQL80`, running automatically).

To add Maven to your user PATH permanently (optional but recommended):
```powershell
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\tools\apache-maven-3.10.0\bin", [EnvironmentVariableTarget]::User)
```

---

## 2. MySQL Database Setup

Follow [DATABASE.md](./DATABASE.md) for full details.

1. Open **MySQL Workbench** and connect to your local MySQL connection as `root`.
2. Open a new Query tab and execute:
   ```sql
   CREATE DATABASE IF NOT EXISTS privacy_awareness_db
       CHARACTER SET utf8mb4
       COLLATE utf8mb4_unicode_ci;

   CREATE USER IF NOT EXISTS 'privacy_user'@'localhost' IDENTIFIED BY 'YOUR_CHOSEN_PASSWORD';
   GRANT ALL PRIVILEGES ON privacy_awareness_db.* TO 'privacy_user'@'localhost';
   FLUSH PRIVILEGES;
   ```
3. Replace `'YOUR_CHOSEN_PASSWORD'` with your preferred password.

---

## 3. Application Configuration

1. In PowerShell, navigate to the project directory:
   ```powershell
   cd "C:\Users\my\Downloads\Phishing System"
   ```
2. Create `.env` from the template:
   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```
3. Set your chosen MySQL password for `DB_PASSWORD`:
   ```env
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=privacy_awareness_db
   DB_USERNAME=privacy_user
   DB_PASSWORD=YOUR_CHOSEN_PASSWORD
   SERVER_PORT=8080
   ```
4. Save and close `notepad`.

---

## 4. Load Environment Variables in PowerShell

Spring Boot does not automatically read unexported `.env` files on Windows. Run the helper script in the current session:

```powershell
. .\Set-Env.ps1
```

*(Ensure the leading dot and space are included: `. .\Set-Env.ps1`)*

---

## 5. Build, Test, and Run

### Run Independent Tests (H2 In-Memory Database)
Tests do not require a live MySQL server:
```powershell
& "C:\tools\apache-maven-3.10.0\bin\mvn.cmd" test
```

### Launch the Application (Connected to MySQL)
```powershell
& "C:\tools\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
```

Once you see:
```
Started PrivacyAwarenessApplication in ... seconds
```
The application is running and the database schema and seed data have been initialized.

---

## 6. Accessing the System

- **Web Application URL**: [http://localhost:8080](http://localhost:8080)
- **Initial Administrator Login**:
  - Username: `admin`
  - Password: `Admin@1234!` (or whatever was configured in `.env`)
- **Regular User**:
  - Click **Register** on the login page to create a personal user profile.

---

## 7. Verifying in MySQL Workbench

After starting the application, run this query in MySQL Workbench to confirm table creation and data seeding:

```sql
USE privacy_awareness_db;
SHOW TABLES;
SELECT COUNT(*) AS total_questions FROM quiz_questions;
SELECT COUNT(*) AS total_modules FROM learning_modules;
```

---

## 8. Common Troubleshooting

| Issue | Solution |
|---|---|
| `Access denied for user 'privacy_user'@'localhost'` | Check that `DB_PASSWORD` in `.env` matches the password used in `CREATE USER`. |
| `Communications link failure` | Confirm MySQL service is running: `Get-Service MySQL80`. Start it with `Start-Service MySQL80`. |
| `mvn` command not recognized | Use `& "C:\tools\apache-maven-3.10.0\bin\mvn.cmd"` or add `C:\tools\apache-maven-3.10.0\bin` to your `Path`. |
| Port 8080 conflict | Change `SERVER_PORT=8081` in `.env`, run `. .\Set-Env.ps1`, then restart. |