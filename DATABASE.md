# MySQL Database Setup & Integration Guide

This guide covers setting up MySQL Community Server 8.x for the **Social Media Privacy Awareness and Security Management System**.

---

## 1. Prerequisites Checklist

- **MySQL 8.x Community Server**: Installed and running (Service name: `MySQL80`).
- **MySQL Workbench** or MySQL Command Line Client (`mysql.exe`).
- **Administrative Access**: MySQL `root` account credentials.

---

## 2. Step-by-Step MySQL Setup (MySQL Workbench or CLI)

Connect to your local MySQL instance as `root` (either via **MySQL Workbench** or using the command prompt: `& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p`).

### Step 2.1 — Run Database & User Creation Script

Execute the following SQL commands:

```sql
-- 1. Create the database with utf8mb4 encoding (supports all Unicode characters and emojis)
CREATE DATABASE IF NOT EXISTS privacy_awareness_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Create the dedicated local application user
-- IMPORTANT: Replace 'YOUR_CHOSEN_PASSWORD' with your own strong password
CREATE USER IF NOT EXISTS 'privacy_user'@'localhost' IDENTIFIED BY 'YOUR_CHOSEN_PASSWORD';

-- 3. Grant privileges on the application database only (Principle of Least Privilege)
GRANT ALL PRIVILEGES ON privacy_awareness_db.* TO 'privacy_user'@'localhost';

-- 4. Apply changes
FLUSH PRIVILEGES;
```

### Step 2.2 — Verify the Database and Permissions

Run the following queries to verify the setup:

```sql
-- Verify database exists with utf8mb4 default character set
SELECT schema_name, default_character_set_name, default_collation_name 
FROM information_schema.schemata 
WHERE schema_name = 'privacy_awareness_db';

-- Verify the dedicated user exists
SELECT user, host FROM mysql.user WHERE user = 'privacy_user';

-- Verify privileges granted to privacy_user
SHOW GRANTS FOR 'privacy_user'@'localhost';
```

---

## 3. Configure Local Credentials

The application uses environment variables so no sensitive credentials are ever hardcoded or committed to version control.

1. In the project root (`C:\Users\my\Downloads\Phishing System`), create your local `.env` file by copying the template:
   ```powershell
   Copy-Item .env.example .env
   ```
2. Open `.env` in Notepad or your editor:
   ```powershell
   notepad .env
   ```
3. Update `DB_PASSWORD` with the exact password you chose for `privacy_user`:
   ```env
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=privacy_awareness_db
   DB_USERNAME=privacy_user
   DB_PASSWORD=YOUR_CHOSEN_PASSWORD
   SERVER_PORT=8080
   ```
4. Save and close the file.

> **Security Note:** `.env` is listed in `.gitignore` and must never be committed to Git.

---

## 4. Load Environment Variables into PowerShell

Spring Boot runs inside your terminal session and reads process environment variables. Use the included helper script `Set-Env.ps1`:

```powershell
# In your PowerShell terminal at C:\Users\my\Downloads\Phishing System:
. .\Set-Env.ps1
```

*(Note: The dot and space before `.\Set-Env.ps1` are required to export the variables into the current session scope).*

You can test that the variable is active:
```powershell
$env:DB_USERNAME  # Should display: privacy_user
$env:DB_NAME      # Should display: privacy_awareness_db
```

---

## 5. Launch the Application & Verify Connection

Start the Spring Boot application using Maven:

```powershell
& "C:\tools\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
```

*(If you have added Maven to your system PATH, you can simply type `mvn spring-boot:run`).*

### How to Confirm Connection Success in Logs:
Look for the following log output:
1. `HikariPool-1 - Added connection conn0: url=jdbc:mysql://localhost:3306/privacy_awareness_db...`
2. `HikariPool-1 - Start completed.`
3. `Initialized JPA EntityManagerFactory for persistence unit 'default'`
4. `Admin user 'admin' created successfully.`
5. `Seeded 15 quiz questions.`
6. `Seeded 6 learning modules.`
7. `Started PrivacyAwarenessApplication in ... seconds`

---

## 6. Verify Tables and Seed Data in MySQL

In **MySQL Workbench** or CLI, execute:

```sql
USE privacy_awareness_db;

-- 1. Check created tables
SHOW TABLES;

-- 2. Verify seeded admin user
SELECT id, username, email, role, full_name FROM users;

-- 3. Verify seeded quiz questions (15 items)
SELECT id, category, points, LEFT(question_text, 60) AS question_preview FROM quiz_questions;

-- 4. Verify seeded learning modules (6 modules)
SELECT id, title, category, estimated_minutes FROM learning_modules;
```

---

## 7. Troubleshooting Common MySQL Issues

| Issue | Root Cause | Solution |
|---|---|---|
| `Access denied for user 'privacy_user'@'localhost'` | Incorrect password in `.env` or user not created | Double-check the password in `.env` matches what you set in `CREATE USER`. Re-run `ALTER USER 'privacy_user'@'localhost' IDENTIFIED BY 'YOUR_PASSWORD'; FLUSH PRIVILEGES;` |
| `Communications link failure` | MySQL Windows service is stopped | Start the service in an Administrator PowerShell: `Start-Service MySQL80` or `net start MySQL80`. |
| `Unknown database 'privacy_awareness_db'` | Database was not created | Run `CREATE DATABASE privacy_awareness_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;` in MySQL Workbench. |
| Variables not reflecting in Spring Boot | Variables were set in another terminal or `. .\Set-Env.ps1` was executed without the leading dot | Run `. .\Set-Env.ps1` in the exact PowerShell terminal where you launch `mvn spring-boot:run`. |
| `Port 8080 was already in use` | Another process is listening on 8080 | Change `SERVER_PORT=8081` in your `.env` and re-run `. .\Set-Env.ps1`. |