# Setup and Running Guide — Privacy Guard

This guide walks you through setting up, configuring, running, and verifying the **Privacy Guard** application on Windows 10/11 using PowerShell.

---

## 1. Verified Software Requirements

Ensure the following tools are installed and available on your system:

| Software | Minimum Version | Verified Machine Version | Verification Command |
|---|---|---|---|
| **Java JDK** | 21 | Eclipse Adoptium OpenJDK 21.0.12 | `java -version` |
| **Apache Maven** | 3.9.0 | Apache Maven 3.10.0 | `mvn -v` |
| **MySQL Server** | 8.0 | MySQL Community Server 8.0.46 | `Get-Service MySQL80` |
| **Web Browser** | Any modern browser | Google Chrome / Microsoft Edge | - |

> **Note on Maven path:** If `mvn` is not recognized in your terminal, either add its `bin` folder (e.g., `C:\tools\apache-maven-3.10.0\bin`) to your system `PATH`, or use the full path command:  
> `& "C:\tools\apache-maven-3.10.0\bin\mvn.cmd"`

---

## 2. MySQL Database Setup

The application connects to MySQL using a dedicated database named `privacy_awareness_db` and an application user named `privacy_user`.

### Step 2.1 — Verify the MySQL Service
Open PowerShell and check if the MySQL service is running:
```powershell
Get-Service MySQL80
```
If the status is `Stopped`, start it with:
```powershell
Start-Service MySQL80
```

### Step 2.2 — Create the Database and User
Connect to MySQL as the `root` administrative user using MySQL Workbench or the MySQL command line client:
```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
```

Execute the following SQL script to create the database with full UTF-8 character support and create the dedicated application account:

```sql
-- 1. Create the application database
CREATE DATABASE IF NOT EXISTS privacy_awareness_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Create the application user (replace 'YourSecurePasswordHere' with your local password)
CREATE USER IF NOT EXISTS 'privacy_user'@'localhost' IDENTIFIED BY 'YourSecurePasswordHere';

-- 3. Grant privileges on the application database
GRANT ALL PRIVILEGES ON privacy_awareness_db.* TO 'privacy_user'@'localhost';

-- 4. Apply privilege changes
FLUSH PRIVILEGES;
```

---

## 3. Environment Variable Configuration

The project reads its configuration from environment variables so that database credentials and server settings are never hardcoded into source code.

### Step 3.1 — Create `.env` from Template
Open a PowerShell terminal in the project root:
```powershell
cd "C:\Users\my\Downloads\Phishing System"
Copy-Item .env.example .env
```

### Step 3.2 — Configure Credentials in `.env`
Open `.env` in Notepad:
```powershell
notepad .env
```
Ensure the variables match your local environment:
```env
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=privacy_awareness_db
DB_USERNAME=privacy_user
DB_PASSWORD=YourSecurePasswordHere

# Server Configuration
SERVER_PORT=8080

# Administrator Bootstrap Account
ADMIN_USERNAME=admin
ADMIN_PASSWORD=Admin@1234!
ADMIN_EMAIL=admin@privacysystem.local

# Hibernate Schema Update Mode (update creates/updates tables without data loss)
JPA_DDL_AUTO=update
```
Save the file and close Notepad.

> **Security Rule:** `.env` is ignored by Git in `.gitignore` and must never be committed to a public repository.

---

## 4. Starting the Application

Spring Boot runs inside your active PowerShell terminal session and reads environment variables set in the process.

### Step 4.1 — Load Environment Variables into PowerShell
In the project directory, run the helper script `Set-Env.ps1`:
```powershell
. .\Set-Env.ps1
```
> **Important:** The leading dot and space (`. .\Set-Env.ps1`) are required. This ensures the environment variables are exported into the current PowerShell process scope.

Verify that the variables are active:
```powershell
$env:DB_USERNAME   # Should print: privacy_user
$env:DB_NAME       # Should print: privacy_awareness_db
```

### Step 4.2 — Start the Spring Boot Application
Run the Maven spring-boot goal:
```powershell
mvn spring-boot:run
```
*(Or use `& "C:\tools\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run` if Maven is not in your PATH).*

### Step 4.3 — Confirm Startup Success
Watch the console output. You should observe:
1. `HikariPool-1 - Added connection conn0: url=jdbc:mysql://localhost:3306/privacy_awareness_db...`
2. `HikariPool-1 - Start completed.`
3. `Initialized JPA EntityManagerFactory for persistence unit 'default'`
4. `DataSeeder: Initial installation: bootstrap admin 'admin' created successfully.` (on first run)
5. `DataSeeder: Seeded 15 quiz questions.`
6. `DataSeeder: Seeded 6 learning modules.`
7. `DataSeeder: Seeded 10 phishing scenarios with detailed red flag indicators.`
8. `Started PrivacyAwarenessApplication in ... seconds`

The web application is now active at **`http://localhost:8080`**.

---

## 5. First-Time Login & Credential Management

### Default Administrator Account
On first startup, the database seeder provisions the initial admin account using the values in `.env`:
- **URL:** [http://localhost:8080/login.html](http://localhost:8080/login.html)
- **Username:** Value of `ADMIN_USERNAME` (default: `admin`)
- **Password:** Value of `ADMIN_PASSWORD` (default: `Admin@1234!`)

### Changing Your Password at Runtime
Both regular users and administrators can safely change their password while logged in:
1. Click the **Change Password** button in the top navigation bar of the user dashboard or admin panel.
2. Enter your current password, the new password (minimum 8 characters with uppercase, lowercase, numbers, and symbols), and confirm.
3. The server validates the existing password hash, updates MySQL with a fresh BCrypt hash, and returns confirmation.

### Administrator Disaster Recovery (`ADMIN_RECOVER`)
If you ever lose access to the administrator account or if the database password hash diverges from `.env`:
1. Open `.env` and set:
   ```env
   ADMIN_PASSWORD=NewAdminPassword@123!
   ADMIN_RECOVER=true
   ```
2. Re-export the variables and start the server:
   ```powershell
   . .\Set-Env.ps1
   mvn spring-boot:run
   ```
3. `DataSeeder` detects `ADMIN_RECOVER=true`, resets the `admin` password in MySQL to the new hash, ensures the account is enabled, and logs a confirmation.
4. Set `ADMIN_RECOVER=false` in `.env` after completing the recovery.

---

## 6. Running Automated Tests

### 6.1 Unit and Slice Tests (H2 In-Memory)
These tests execute isolated service and controller tests using an embedded H2 database without modifying your MySQL records:
```powershell
mvn clean test
```
**Expected outcome:** 15 tests executed, 0 failures, 0 errors across:
- `AdminAnalyticsTests`
- `AdminUserManagementTests`
- `DashboardAnalyticsTests`
- `PhishingSimulatorTests`
- `PrivacyAwarenessApplicationTests`

### 6.2 Live End-to-End Regression Test Suite (MySQL)
To run the automated 40-test integration suite against the live running application:
1. Ensure the Spring Boot server is running on port 8080 in one terminal.
2. Open a second PowerShell terminal in the project directory:
   ```powershell
   cd "C:\Users\my\Downloads\Phishing System"
   . .\Set-Env.ps1
   .\test-runner.ps1
   ```
**Expected outcome:** 40 / 40 tests passing, validating registration, authentication, quiz evaluation, module progress, incident lifecycle, CSV and PDF exports, session invalidation filters, and relational deletion safeguards.

---

## 7. Common Troubleshooting

| Issue | Cause | Solution |
|---|---|---|
| `Access denied for user 'privacy_user'@'localhost'` | Password mismatch between MySQL and `.env` | Ensure `DB_PASSWORD` in `.env` matches the password specified in `CREATE USER`. To reset, run `ALTER USER 'privacy_user'@'localhost' IDENTIFIED BY 'YourNewPassword'; FLUSH PRIVILEGES;` in MySQL Workbench. |
| `Communications link failure` | MySQL service is stopped or port is blocked | Verify MySQL service is active: `Get-Service MySQL80`. If stopped, run `Start-Service MySQL80`. |
| `Port 8080 was already in use` | Another process is using port 8080 | Identify the process using `Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess` or change `SERVER_PORT=8081` in `.env`, run `. .\Set-Env.ps1`, and restart. |
| Admin login fails with 401 | Database password hash differs from current `.env` value | Use the recovery mode: add `ADMIN_RECOVER=true` to `.env`, run `. .\Set-Env.ps1`, and restart the application. |
| Environment variables not picked up | Ran `Set-Env.ps1` without the dot prefix | Always invoke the script with the leading dot and space: `. .\Set-Env.ps1` so variables persist in the current PowerShell process. |
| Cannot delete question/module (HTTP 400) | Item has historical answers or user progress | This is an intentional relational safeguard preventing broken foreign keys. Deactivate the question or module using the toggle switch instead of hard-deleting it. |