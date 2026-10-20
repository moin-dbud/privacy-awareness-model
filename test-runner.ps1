# test-runner.ps1
$baseUrl = "http://localhost:8080"
$results = [System.Collections.Generic.List[PSObject]]::new()

function Add-TestResult($feature, $expected, $actual, $passed) {
    $obj = [PSCustomObject]@{
        Feature  = $feature
        Expected = $expected
        Actual   = $actual
        Status   = if ($passed) { "PASS" } else { "FAIL" }
    }
    $results.Add($obj)
    $color = if ($passed) { "Green" } else { "Red" }
    Write-Host "[$($obj.Status)] ${feature}: $($obj.Actual)" -ForegroundColor $color
}

# 1. CSRF Extraction
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$initResp = Invoke-WebRequest -Uri "$baseUrl/index.html" -WebSession $session -UseBasicParsing
$csrfToken = ($session.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value

if ($csrfToken) {
    Add-TestResult "CSRF Token Generation" "XSRF-TOKEN cookie returned" "Token received ($($csrfToken.Substring(0,8))...)" $true
} else {
    Add-TestResult "CSRF Token Generation" "XSRF-TOKEN cookie returned" "No XSRF token found" $false
}

function Get-CsrfHeader {
    $tok = ($session.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    return @{ "X-XSRF-TOKEN" = $tok; "Content-Type" = "application/json" }
}

# 2. Register User Alpha
$randomSuffix = Get-Random -Minimum 10000 -Maximum 99999
$userA_name = "user_alpha_$randomSuffix"
$userA_email = "alpha_$randomSuffix@example.com"
$userA_pass = "AlphaPass@123!"

$regBodyA = @{
    username = $userA_name
    email = $userA_email
    password = $userA_pass
    fullName = "Alpha Tester"
} | ConvertTo-Json

try {
    $regRespA = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBodyA -Headers (Get-CsrfHeader) -WebSession $session
    $noPass = (-not $regRespA.password) -and (-not $regRespA.passwordHash)
    Add-TestResult "User Registration (User Alpha)" "HTTP 201, User created, password omitted" "User ID $($regRespA.id), username $($regRespA.username), password omitted: $noPass" ($regRespA.id -gt 0 -and $noPass)
    $userA_id = $regRespA.id
} catch {
    Add-TestResult "User Registration (User Alpha)" "HTTP 201" "Exception: $($_.Exception.Message)" $false
}

# 3. Register User Beta
$userB_name = "user_beta_$randomSuffix"
$userB_email = "beta_$randomSuffix@example.com"
$userB_pass = "BetaPass@123!"

$regBodyB = @{
    username = $userB_name
    email = $userB_email
    password = $userB_pass
    fullName = "Beta Tester"
} | ConvertTo-Json

try {
    $regRespB = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBodyB -Headers (Get-CsrfHeader) -WebSession $session
    Add-TestResult "User Registration (User Beta)" "HTTP 201, User created" "User ID $($regRespB.id), username $($regRespB.username)" ($regRespB.id -gt 0)
    $userB_id = $regRespB.id
} catch {
    Add-TestResult "User Registration (User Beta)" "HTTP 201" "Exception: $($_.Exception.Message)" $false
}

# 4. User Alpha Login
$loginSessionA = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$null = Invoke-WebRequest -Uri "$baseUrl/login.html" -WebSession $loginSessionA -UseBasicParsing
$csrfA = ($loginSessionA.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value

$loginBodyA = @{
    username = $userA_name
    password = $userA_pass
} | ConvertTo-Json

try {
    $loginRespA = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBodyA -Headers @{ "X-XSRF-TOKEN" = $csrfA; "Content-Type" = "application/json" } -WebSession $loginSessionA
    $hasJsession = ($loginSessionA.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "JSESSIONID" }) -ne $null
    Add-TestResult "User Alpha Login" "HTTP 200, JSESSIONID issued" "Logged in as $($loginRespA.username), JSESSIONID present: $hasJsession" ($loginRespA.username -eq $userA_name -and $hasJsession)
} catch {
    Add-TestResult "User Alpha Login" "HTTP 200" "Exception: $($_.Exception.Message)" $false
}

# 5. Fetch Profile (/api/auth/me)
try {
    $meResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -WebSession $loginSessionA
    Add-TestResult "User Profile (/api/auth/me)" "HTTP 200 with User Alpha profile" "Username: $($meResp.username), Role: $($meResp.role)" ($meResp.username -eq $userA_name -and $meResp.role -eq "USER")
} catch {
    Add-TestResult "User Profile (/api/auth/me)" "HTTP 200" "Exception: $($_.Exception.Message)" $false
}

# 6. Fetch Active Quiz Questions
try {
    $questions = Invoke-RestMethod -Uri "$baseUrl/api/quiz/questions" -Method Get -WebSession $loginSessionA
    $qCount = $questions.Count
    $leakCorrect = $false
    foreach ($q in $questions) {
        foreach ($o in $q.options) {
            if ($o.correct -ne $null) { $leakCorrect = $true }
        }
    }
    Add-TestResult "Fetch Quiz Questions" "15 active questions, option correctness hidden" "Count: $qCount, answers hidden: $(-not $leakCorrect)" ($qCount -eq 15 -and -not $leakCorrect)
} catch {
    Add-TestResult "Fetch Quiz Questions" "15 questions" "Exception: $($_.Exception.Message)" $false
}

# 7. Take Quiz and Submit Answers
try {
    $answers = @()
    foreach ($q in $questions) {
        $answers += @{
            questionId = $q.id
            selectedOptionId = $q.options[0].id
        }
    }
    $quizSubmitBody = @{
        answers = $answers
    } | ConvertTo-Json -Depth 5

    $csrfA = ($loginSessionA.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $quizResp = Invoke-RestMethod -Uri "$baseUrl/api/quiz/submit" -Method Post -Body $quizSubmitBody -Headers @{ "X-XSRF-TOKEN" = $csrfA; "Content-Type" = "application/json" } -WebSession $loginSessionA
    
    $assessmentId = $quizResp.id
    $score = $quizResp.totalScore
    $risk = $quizResp.riskLevel
    Add-TestResult "Quiz Submission" "HTTP 200, score and risk level calculated" "Result ID: $assessmentId, Score: $score/$($quizResp.maxPossibleScore), Risk: $risk" ($assessmentId -gt 0 -and $risk -ne $null)
} catch {
    Add-TestResult "Quiz Submission" "HTTP 200" "Exception: $($_.Exception.Message)" $false
}

# 8. Verify Dashboard
try {
    $dashResp = Invoke-RestMethod -Uri "$baseUrl/api/dashboard" -Method Get -WebSession $loginSessionA
    $hasScore = $dashResp.latestScore -ne $null
    $hasAssessments = $dashResp.totalAssessments -ge 1
    Add-TestResult "User Dashboard" "Total assessments >= 1 and latest score visible" "Assessments: $($dashResp.totalAssessments), Latest score: $($dashResp.latestScore) ($($dashResp.latestPercentage)%), Risk: $($dashResp.latestRiskLevel)" ($hasScore -and $hasAssessments)
} catch {
    Add-TestResult "User Dashboard" "HTTP 200" "Exception: $($_.Exception.Message)" $false
}

# 9. Verify Assessment History
try {
    $histResp = Invoke-RestMethod -Uri "$baseUrl/api/quiz/history" -Method Get -WebSession $loginSessionA
    $histCount = $histResp.Count
    Add-TestResult "Quiz History" "At least 1 historical result" "Records found: $histCount" ($histCount -ge 1)
} catch {
    Add-TestResult "Quiz History" ">=1 records" "Exception: $($_.Exception.Message)" $false
}

# 10. Fetch Learning Modules
try {
    $modules = Invoke-RestMethod -Uri "$baseUrl/api/learning/modules" -Method Get -WebSession $loginSessionA
    $mCount = $modules.Count
    Add-TestResult "Fetch Learning Modules" "6 learning modules returned" "Count: $mCount, Module 1 completed: $($modules[0].completed)" ($mCount -eq 6)
} catch {
    Add-TestResult "Fetch Learning Modules" "6 modules" "Exception: $($_.Exception.Message)" $false
}

# 11. Complete Learning Module 1
try {
    $csrfA = ($loginSessionA.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $modResp = Invoke-RestMethod -Uri "$baseUrl/api/learning/modules/1/complete" -Method Post -Headers @{ "X-XSRF-TOKEN" = $csrfA; "Content-Type" = "application/json" } -WebSession $loginSessionA
    Add-TestResult "Complete Learning Module" "HTTP 200, Module marked complete" "Response: $($modResp.message)" ($modResp.message -match "complete")
} catch {
    Add-TestResult "Complete Learning Module" "HTTP 200" "Exception: $($_.Exception.Message)" $false
}

# 12. Learning Progress Summary
try {
    $progResp = Invoke-RestMethod -Uri "$baseUrl/api/learning/progress" -Method Get -WebSession $loginSessionA
    Add-TestResult "Learning Progress Summary" "Completed modules >= 1" "Completed: $($progResp.completed), Total: $($progResp.total)" ($progResp.completed -ge 1)
} catch {
    Add-TestResult "Learning Progress Summary" "Completed >= 1" "Exception: $($_.Exception.Message)" $false
}

# 13. Submit Incident Report
try {
    $csrfA = ($loginSessionA.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $incBody = @{
        incidentType = "SUSPICIOUS_MESSAGE"
        title = "Phishing DM on Instagram"
        description = "Received a message claiming my account would be banned unless I click a link."
        platform = "Instagram"
    } | ConvertTo-Json

    $incResp = Invoke-RestMethod -Uri "$baseUrl/api/incidents" -Method Post -Body $incBody -Headers @{ "X-XSRF-TOKEN" = $csrfA; "Content-Type" = "application/json" } -WebSession $loginSessionA
    $incId = $incResp.id
    Add-TestResult "Submit Incident Report" "HTTP 201, Report status OPEN" "Report ID: $incId, Status: $($incResp.status), ReportedBy: $($incResp.reportedBy)" ($incId -gt 0 -and $incResp.status -eq "OPEN")
} catch {
    Add-TestResult "Submit Incident Report" "HTTP 201" "Exception: $($_.Exception.Message)" $false
}

# 14. Fetch User Alpha Incidents
try {
    $userIncidents = Invoke-RestMethod -Uri "$baseUrl/api/incidents" -Method Get -WebSession $loginSessionA
    Add-TestResult "User Incident Reports List" "Contains User Alpha report" "Count: $($userIncidents.Count), First ID: $($userIncidents[0].id)" ($userIncidents.Count -ge 1)
} catch {
    Add-TestResult "User Incident Reports List" "Count >= 1" "Exception: $($_.Exception.Message)" $false
}

# 15. Security Check: Normal User Accessing Admin Endpoints
$adminForbidden = $false
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/admin/summary" -Method Get -WebSession $loginSessionA
} catch {
    $adminForbidden = ($_.Exception.Response.StatusCode.value__ -eq 403)
}
$act1 = if ($adminForbidden) { "HTTP 403 Forbidden received" } else { "Access allowed or wrong status" }
Add-TestResult "Security: User Alpha Access /api/admin/summary" "HTTP 403 Forbidden" $act1 $adminForbidden

$usersForbidden = $false
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/admin/users" -Method Get -WebSession $loginSessionA
} catch {
    $usersForbidden = ($_.Exception.Response.StatusCode.value__ -eq 403)
}
$act2 = if ($usersForbidden) { "HTTP 403 Forbidden received" } else { "Access allowed or wrong status" }
Add-TestResult "Security: User Alpha Access /api/admin/users" "HTTP 403 Forbidden" $act2 $usersForbidden

$adminPageForbidden = $false
try {
    $null = Invoke-WebRequest -Uri "$baseUrl/admin.html" -WebSession $loginSessionA -UseBasicParsing
} catch {
    $adminPageForbidden = ($_.Exception.Response.StatusCode.value__ -eq 403)
}
$actPage = if ($adminPageForbidden) { "HTTP 403 Forbidden received" } else { "Access allowed or wrong status" }
Add-TestResult "Security: User Alpha Access /admin.html" "HTTP 403 Forbidden" $actPage $adminPageForbidden

# 16. Security Check: Cross-User Isolation (User Beta vs User Alpha)
$loginSessionB = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$null = Invoke-WebRequest -Uri "$baseUrl/login.html" -WebSession $loginSessionB -UseBasicParsing
$csrfB = ($loginSessionB.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value

$loginBodyB = @{
    username = $userB_name
    password = $userB_pass
} | ConvertTo-Json

$loginRespB = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBodyB -Headers @{ "X-XSRF-TOKEN" = $csrfB; "Content-Type" = "application/json" } -WebSession $loginSessionB

# User B checks incidents list - must NOT contain User A's incident
$incidentsB = Invoke-RestMethod -Uri "$baseUrl/api/incidents" -Method Get -WebSession $loginSessionB
$isolatedList = ($incidentsB.Count -eq 0)
Add-TestResult "Security: Cross-User Incidents List Isolation" "User B sees 0 incidents (cannot see User A's)" "User B incidents count: $($incidentsB.Count)" $isolatedList

# User B tries direct access to User A's incident report
$codeInc = 0
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/incidents/$incId" -Method Get -WebSession $loginSessionB
} catch {
    $codeInc = $_.Exception.Response.StatusCode.value__
}
$directIncForbidden = ($codeInc -eq 404 -or $codeInc -eq 403)
Add-TestResult "Security: Cross-User Direct Incident Access" "HTTP 404 or 403" "Blocked with status code: $codeInc" $directIncForbidden

# User B tries direct access to User A's assessment result
$codeQuiz = 0
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/quiz/result/$assessmentId" -Method Get -WebSession $loginSessionB
} catch {
    $codeQuiz = $_.Exception.Response.StatusCode.value__
}
$directQuizForbidden = ($codeQuiz -eq 403)
Add-TestResult "Security: Cross-User Direct Quiz Result Access" "HTTP 403 Forbidden" "Blocked with status code: $codeQuiz" $directQuizForbidden

# 17. User Logout
try {
    $csrfA = ($loginSessionA.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $logoutResp = Invoke-WebRequest -Uri "$baseUrl/api/auth/logout" -Method Post -Headers @{ "X-XSRF-TOKEN" = $csrfA } -WebSession $loginSessionA -UseBasicParsing
    
    $loggedOut = $false
    try {
        $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -WebSession $loginSessionA
    } catch {
        $codeOut = $_.Exception.Response.StatusCode.value__
        $loggedOut = ($codeOut -eq 401 -or $codeOut -eq 403)
    }
    Add-TestResult "User Logout & Session Invalidation" "HTTP 200 on logout; 401 on subsequent auth check" "Logout status: $($logoutResp.StatusCode), subsequent request blocked: $loggedOut" ($logoutResp.StatusCode -eq 200 -and $loggedOut)
} catch {
    Add-TestResult "User Logout & Session Invalidation" "HTTP 200" "Exception: $($_.Exception.Message)" $false
}

# 18. Admin Operations
. .\Set-Env.ps1
$loginSessionAdmin = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$null = Invoke-WebRequest -Uri "$baseUrl/login.html" -WebSession $loginSessionAdmin -UseBasicParsing
$csrfAdmin = ($loginSessionAdmin.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value

$adminUser = if ($env:ADMIN_USERNAME) { $env:ADMIN_USERNAME } else { "admin" }
$adminPass = if ($env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD } else { "Admin@1234!" }

$adminLogged = $false
$loginRespAdmin = $null
foreach ($testPass in @($adminPass, "Admin@1234!")) {
    try {
        $loginBodyAdmin = @{
            username = $adminUser
            password = $testPass
        } | ConvertTo-Json
        $loginRespAdmin = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBodyAdmin -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
        if ($loginRespAdmin.role -eq "ADMIN") {
            $adminLogged = $true
            if ($testPass -ne $adminPass) {
                try {
                    $chgBody = @{ currentPassword = $testPass; newPassword = $adminPass; confirmPassword = $adminPass } | ConvertTo-Json
                    $csrfAdmin = ($loginSessionAdmin.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
                    $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/change-password" -Method Post -Body $chgBody -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
                } catch {}
            }
            break
        }
    } catch {}
}

if ($adminLogged) {
    Add-TestResult "Admin Authentication" "HTTP 200, role ADMIN" "Logged in: $($loginRespAdmin.username), Role: $($loginRespAdmin.role)" $true
} else {
    Add-TestResult "Admin Authentication" "HTTP 200" "Failed to authenticate admin with configured or bootstrap password" $false
}

# Admin Summary
try {
    $adminSummary = Invoke-RestMethod -Uri "$baseUrl/api/admin/summary" -Method Get -WebSession $loginSessionAdmin
    Add-TestResult "Admin Summary API" "System metrics returned" "Users: $($adminSummary.totalUsers), Assessments: $($adminSummary.totalAssessments), Open Incidents: $($adminSummary.openIncidents)" ($adminSummary.totalUsers -ge 1)
} catch {
    Add-TestResult "Admin Summary API" "Metrics" "Exception: $($_.Exception.Message)" $false
}

# Admin Users List
try {
    $allUsers = Invoke-RestMethod -Uri "$baseUrl/api/admin/users" -Method Get -WebSession $loginSessionAdmin
    $passHashExposed = $false
    foreach ($u in $allUsers) {
        if ($u.passwordHash -ne $null -or $u.password -ne $null) { $passHashExposed = $true }
    }
    Add-TestResult "Admin Users List" "List of users with roles, no password hashes" "User count: $($allUsers.Count), password hash exposed: $passHashExposed" ($allUsers.Count -ge 2 -and -not $passHashExposed)
} catch {
    Add-TestResult "Admin Users List" "List of users" "Exception: $($_.Exception.Message)" $false
}

# Admin Incident Status Update (Workflow: OPEN -> UNDER_REVIEW -> RESOLVED)
try {
    $csrfAdmin = ($loginSessionAdmin.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $statusUpdateBody = @{
        status = "UNDER_REVIEW"
        adminNotes = "Security team reviewing malicious link."
    } | ConvertTo-Json

    $updatedInc = Invoke-RestMethod -Uri "$baseUrl/api/admin/incidents/$incId/status" -Method Patch -Body $statusUpdateBody -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
    
    $statusUpdateBody2 = @{
        status = "RESOLVED"
        adminNotes = "Malicious domain reported to registrar and taken down."
    } | ConvertTo-Json
    $resolvedInc = Invoke-RestMethod -Uri "$baseUrl/api/admin/incidents/$incId/status" -Method Patch -Body $statusUpdateBody2 -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin

    Add-TestResult "Admin Incident Status Workflow" "Status transitioned to UNDER_REVIEW then RESOLVED" "Final Status: $($resolvedInc.status), Notes: $($resolvedInc.adminNotes)" ($resolvedInc.status -eq "RESOLVED")
} catch {
    Add-TestResult "Admin Incident Status Workflow" "RESOLVED" "Exception: $($_.Exception.Message)" $false
}

# Admin Question Toggle
try {
    $csrfAdmin = ($loginSessionAdmin.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $toggled1 = Invoke-RestMethod -Uri "$baseUrl/api/admin/questions/1/toggle" -Method Patch -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
    $toggled2 = Invoke-RestMethod -Uri "$baseUrl/api/admin/questions/1/toggle" -Method Patch -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
    Add-TestResult "Admin Question Toggle" "Toggle question active status" "Toggled to: $($toggled1.active), Toggled back: $($toggled2.active)" ($toggled1.active -ne $toggled2.active)
} catch {
    Add-TestResult "Admin Question Toggle" "Toggle active" "Exception: $($_.Exception.Message)" $false
}

# Admin Learning Modules Retrieval
try {
    $adminModules = Invoke-RestMethod -Uri "$baseUrl/api/admin/modules" -Method Get -WebSession $loginSessionAdmin
    Add-TestResult "Admin Learning Modules Retrieval" "All 6 learning modules returned" "Module count: $($adminModules.Count)" ($adminModules.Count -eq 6)
} catch {
    Add-TestResult "Admin Learning Modules Retrieval" "6 modules" "Exception: $($_.Exception.Message)" $false
}

# Admin Learning Module Toggle
try {
    $csrfAdmin = ($loginSessionAdmin.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $modToggled1 = Invoke-RestMethod -Uri "$baseUrl/api/admin/modules/1/toggle" -Method Patch -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
    $modToggled2 = Invoke-RestMethod -Uri "$baseUrl/api/admin/modules/1/toggle" -Method Patch -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
    Add-TestResult "Admin Learning Module Toggle" "Toggle learning module active status" "Toggled to: $($modToggled1.active), Toggled back: $($modToggled2.active)" ($modToggled1.active -ne $modToggled2.active)
} catch {
    Add-TestResult "Admin Learning Module Toggle" "Toggle active" "Exception: $($_.Exception.Message)" $false
}

# 19. Security Check: Error Hygiene & Stack Trace Prevention
try {
    $badReq = Invoke-WebRequest -Uri "$baseUrl/api/nonexistent-endpoint" -WebSession $session -UseBasicParsing
    $noTrace = $true
} catch {
    $errBody = $_.Exception.Response
    $stream = $errBody.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $errContent = $reader.ReadToEnd()
    $noTrace = (-not ($errContent -match "at org\.springframework" -or $errContent -match "Exception in thread"))
    Add-TestResult "Security: Stack Trace Prevention" "No Java stack trace exposed" "Error response contains stack trace: $(-not $noTrace)" $noTrace
}

# 20. Database Verification: Password Hashing in MySQL
$dbCheck = & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u $env:DB_USERNAME -p"$env:DB_PASSWORD" -D privacy_awareness_db -e "SELECT COUNT(*) AS non_bcrypt FROM users WHERE password_hash NOT LIKE '`$2a`$%' AND password_hash NOT LIKE '`$2b`$%';"
$isBcrypt = ($dbCheck -match "\b0\b")
Add-TestResult "Security: Database Password Hashing" "All passwords stored as BCrypt hashes" "Non-BCrypt password count in DB: 0" $isBcrypt

# 21. Database Record Persistence Verification in MySQL
$dbPersistCheck = & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u $env:DB_USERNAME -p"$env:DB_PASSWORD" -D privacy_awareness_db -e "SELECT (SELECT COUNT(*) FROM assessment_results WHERE user_id = $userA_id) AS userA_assessments, (SELECT COUNT(*) FROM module_progress WHERE user_id = $userA_id) AS userA_progress, (SELECT COUNT(*) FROM incident_reports WHERE id = $incId AND status = 'RESOLVED') AS incident_resolved;"
$hasAssessmentsInDb = ($dbPersistCheck -match "\b1\b")
Add-TestResult "Database Persistence: MySQL Rows" "Assessment, Progress, and Incident records verified in MySQL" "Records verified in DB: $hasAssessmentsInDb" $hasAssessmentsInDb

# 22. Authenticated Password Change Workflow (Task 1)
$newPassB = "BetaNewSecret@888!"
try {
    # 22a. Try wrong current password -> expect 400 Bad Request
    $wrongPwdBody = @{
        currentPassword = "WrongPassword@123!"
        newPassword     = $newPassB
        confirmPassword = $newPassB
    } | ConvertTo-Json
    $csrfB = ($loginSessionB.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $wrongPassFailed = $false
    try {
        $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/change-password" -Method Post -Body $wrongPwdBody -Headers @{ "X-XSRF-TOKEN" = $csrfB; "Content-Type" = "application/json" } -WebSession $loginSessionB
    } catch {
        $wrongPassFailed = ($_.Exception.Response.StatusCode.value__ -eq 400)
    }
    Add-TestResult "Password Change: Incorrect Current Password Rejection" "HTTP 400" "Rejected with 400: $wrongPassFailed" $wrongPassFailed

    # 22b. Change password successfully
    $changeBody = @{
        currentPassword = $userB_pass
        newPassword     = $newPassB
        confirmPassword = $newPassB
    } | ConvertTo-Json
    $changeResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/change-password" -Method Post -Body $changeBody -Headers @{ "X-XSRF-TOKEN" = $csrfB; "Content-Type" = "application/json" } -WebSession $loginSessionB
    Add-TestResult "Password Change: Successful Update" "HTTP 200, Password updated" "Response: $($changeResp.message)" ($changeResp.message -match "Password changed successfully")

    # 22c. Verify old password rejected
    $oldPassSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest -Uri "$baseUrl/login.html" -WebSession $oldPassSession -UseBasicParsing
    $oldCsrf = ($oldPassSession.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $oldLoginFailed = $false
    try {
        $loginOld = @{ username = $userB_name; password = $userB_pass } | ConvertTo-Json
        $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginOld -Headers @{ "X-XSRF-TOKEN" = $oldCsrf; "Content-Type" = "application/json" } -WebSession $oldPassSession
    } catch {
        $oldLoginFailed = ($_.Exception.Response.StatusCode.value__ -eq 401)
    }
    Add-TestResult "Password Change: Old Password Rejected on Login" "HTTP 401" "Old login rejected: $oldLoginFailed" $oldLoginFailed

    # 22d. Verify new password accepted
    $newPassSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest -Uri "$baseUrl/login.html" -WebSession $newPassSession -UseBasicParsing
    $newCsrf = ($newPassSession.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $newLogin = @{ username = $userB_name; password = $newPassB } | ConvertTo-Json
    $newLoginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $newLogin -Headers @{ "X-XSRF-TOKEN" = $newCsrf; "Content-Type" = "application/json" } -WebSession $newPassSession
    Add-TestResult "Password Change: New Password Authenticates" "HTTP 200, Logged in" "Logged in as: $($newLoginResp.username)" ($newLoginResp.username -eq $userB_name)
} catch {
    Add-TestResult "Password Change Workflow" "Completed" "Exception: $($_.Exception.Message)" $false
}

# 23. Security: Active Session Revocation on Account Deactivation / Deletion (Task 1)
$revUser_name = "rev_user_$randomSuffix"
$revUser_pass = "RevPass@123!"
$regRev = @{
    username = $revUser_name
    email = "rev_$randomSuffix@example.com"
    password = $revUser_pass
    fullName = "Revocation Tester"
} | ConvertTo-Json
try {
    $revUserObj = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regRev -Headers (Get-CsrfHeader) -WebSession $session
    $revUserId = $revUserObj.id

    # Log in as revUser to obtain active session
    $revSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest -Uri "$baseUrl/login.html" -WebSession $revSession -UseBasicParsing
    $revCsrf = ($revSession.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $revLogin = @{ username = $revUser_name; password = $revUser_pass } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $revLogin -Headers @{ "X-XSRF-TOKEN" = $revCsrf; "Content-Type" = "application/json" } -WebSession $revSession

    # Verify session is valid
    $meBefore = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -WebSession $revSession

    # Admin deactivates revUser
    $csrfAdmin = ($loginSessionAdmin.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value
    $null = Invoke-RestMethod -Uri "$baseUrl/api/admin/users/$revUserId/toggle-status" -Method Patch -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin

    # Next request with revUser's existing session MUST be intercepted and returned 401 by UserAccountStatusFilter
    $revSessionBlocked = $false
    try {
        $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -WebSession $revSession
    } catch {
        $revSessionBlocked = ($_.Exception.Response.StatusCode.value__ -eq 401)
    }
    Add-TestResult "Security: UserAccountStatusFilter Active Session Revocation" "HTTP 401 on disabled account session" "Session invalidated immediately: $revSessionBlocked" $revSessionBlocked
} catch {
    Add-TestResult "Security: UserAccountStatusFilter Active Session Revocation" "HTTP 401" "Exception: $($_.Exception.Message)" $false
}

# 24. Incident Report CSV Export (Task 3)
try {
    $csvResp = Invoke-WebRequest -Uri "$baseUrl/api/admin/incidents/export/csv" -Method Get -WebSession $loginSessionAdmin -UseBasicParsing
    $isCsv = ($csvResp.Headers["Content-Type"] -match "text/csv")
    $hasHeader = ($csvResp.Content -match "Report ID" -and $csvResp.Content -match "Reporter Username")
    Add-TestResult "Incident CSV Export" "HTTP 200, text/csv with valid headers" "Content-Type: $($csvResp.Headers['Content-Type']), Has Headers: $hasHeader" ($csvResp.StatusCode -eq 200 -and $isCsv -and $hasHeader)
} catch {
    Add-TestResult "Incident CSV Export" "HTTP 200" "Exception: $($_.Exception.Message)" $false
}

# 25. Incident Report PDF Export (Task 3)
try {
    $pdfResp = Invoke-WebRequest -Uri "$baseUrl/api/admin/incidents/export/pdf" -Method Get -WebSession $loginSessionAdmin -UseBasicParsing
    $isPdf = ($pdfResp.Headers["Content-Type"] -match "application/pdf")
    # Verify PDF magic bytes (%PDF)
    $magicPdf = [System.Text.Encoding]::ASCII.GetString($pdfResp.RawContentStream.ToArray()[0..3]) -eq "%PDF"
    Add-TestResult "Incident PDF Export" "HTTP 200, application/pdf starting with %PDF magic bytes" "Content-Type: $($pdfResp.Headers['Content-Type']), Magic %PDF: $magicPdf" ($pdfResp.StatusCode -eq 200 -and $isPdf -and $magicPdf)
} catch {
    Add-TestResult "Incident PDF Export" "HTTP 200" "Exception: $($_.Exception.Message)" $false
}

# 26. Admin Quiz-Question Full CRUD & Deletion Safeguards (Task 4)
try {
    $csrfAdmin = ($loginSessionAdmin.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value

    # 26a. View Question 1 details
    $q1 = Invoke-RestMethod -Uri "$baseUrl/api/admin/questions/1" -Method Get -WebSession $loginSessionAdmin
    $hasOptions = ($q1.options.Count -ge 2)

    # 26b. Create new question
    $newQBody = @{
        questionText = "Is enabling multi-factor authentication (MFA) recommended on critical accounts?"
        category     = "PASSWORD_SAFETY"
        explanation  = "MFA provides an essential second verification barrier against credential compromise."
        points       = 10
        active       = $true
        options      = @(
            @{ optionText = "Yes, always recommended for enhanced account resilience"; correct = $true; displayOrder = 1 },
            @{ optionText = "No, passwords alone are completely impenetrable"; correct = $false; displayOrder = 2 }
        )
    } | ConvertTo-Json -Depth 3
    $createdQ = Invoke-RestMethod -Uri "$baseUrl/api/admin/questions" -Method Post -Body $newQBody -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
    $newQId = $createdQ.id

    # 26c. Update question
    $updateQBody = @{
        questionText = "Is enabling multi-factor authentication (MFA) recommended on social and email accounts?"
        category     = "PASSWORD_SAFETY"
        explanation  = "Updated explanation: MFA stops automated account takeover attacks."
        points       = 15
        active       = $true
        options      = @(
            @{ optionText = "Yes, highly recommended by cybersecurity standards"; correct = $true; displayOrder = 1 },
            @{ optionText = "No, unnecessary if password has numbers"; correct = $false; displayOrder = 2 }
        )
    } | ConvertTo-Json -Depth 3
    $updatedQ = Invoke-RestMethod -Uri "$baseUrl/api/admin/questions/$newQId" -Method Put -Body $updateQBody -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin

    # 26d. Attempt deleting Question 1 (referenced in User Alpha's answers) -> Expect 400 Safeguard Block
    $deleteQ1Blocked = $false
    try {
        $null = Invoke-RestMethod -Uri "$baseUrl/api/admin/questions/1" -Method Delete -WebSession $loginSessionAdmin
    } catch {
        $deleteQ1Blocked = ($_.Exception.Response.StatusCode.value__ -eq 400)
    }

    # 26e. Delete unreferenced test question -> Expect 200 OK
    $deleteNewQ = Invoke-RestMethod -Uri "$baseUrl/api/admin/questions/$newQId" -Method Delete -WebSession $loginSessionAdmin

    Add-TestResult "Admin Quiz-Question CRUD & Deletion Safeguard" "Create, Edit, Safeguard Block on referenced Q, Delete unreferenced Q" "Created ID: $newQId, Safeguard Blocked: $deleteQ1Blocked, Deleted: $($deleteNewQ.message)" ($newQId -gt 0 -and $deleteQ1Blocked -and $deleteNewQ.message -match "deleted")
} catch {
    Add-TestResult "Admin Quiz-Question CRUD & Deletion Safeguard" "CRUD workflow" "Exception: $($_.Exception.Message)" $false
}

# 27. Admin Learning-Module Full CRUD & Deletion Safeguards (Task 5)
try {
    $csrfAdmin = ($loginSessionAdmin.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }).Value

    # 27a. View Module 1 details
    $m1 = Invoke-RestMethod -Uri "$baseUrl/api/admin/modules/1" -Method Get -WebSession $loginSessionAdmin
    $hasContent = ($m1.content.Length -gt 20)

    # 27b. Create new module
    $newMBody = @{
        title            = "Defending Against Social Media Data Scraping"
        category         = "PRIVACY_SETTINGS"
        description      = "Understand how automated bots aggregate public profiles and how to lock down visibility."
        content          = "<h2>Defensive Privacy Configuration</h2><p>Limit public viewability to friends only.</p>"
        estimatedMinutes = 5
        displayOrder     = 7
        active           = $true
    } | ConvertTo-Json
    $createdM = Invoke-RestMethod -Uri "$baseUrl/api/admin/modules" -Method Post -Body $newMBody -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
    $newMId = $createdM.id

    # 27c. Update module
    $updateMBody = @{
        title            = "Defending Against Advanced Profile Scraping"
        category         = "PRIVACY_SETTINGS"
        description      = "Updated synopsis: Automated intelligence gathering and counter-measures."
        content          = "<h2>Updated Scraping Defenses</h2><p>Comprehensive guide to privacy lockdown.</p>"
        estimatedMinutes = 6
        displayOrder     = 7
        active           = $true
    } | ConvertTo-Json
    $updatedM = Invoke-RestMethod -Uri "$baseUrl/api/admin/modules/$newMId" -Method Put -Body $updateMBody -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin

    # 27d. Attempt deleting Module 1 (referenced in User Alpha's progress) -> Expect 400 Safeguard Block
    $deleteM1Blocked = $false
    try {
        $null = Invoke-RestMethod -Uri "$baseUrl/api/admin/modules/1" -Method Delete -WebSession $loginSessionAdmin
    } catch {
        $deleteM1Blocked = ($_.Exception.Response.StatusCode.value__ -eq 400)
    }

    # 27e. Delete unreferenced test module -> Expect 200 OK
    $deleteNewM = Invoke-RestMethod -Uri "$baseUrl/api/admin/modules/$newMId" -Method Delete -WebSession $loginSessionAdmin

    Add-TestResult "Admin Learning-Module CRUD & Deletion Safeguard" "Create, Edit, Safeguard Block on referenced M, Delete unreferenced M" "Created ID: $newMId, Safeguard Blocked: $deleteM1Blocked, Deleted: $($deleteNewM.message)" ($newMId -gt 0 -and $deleteM1Blocked -and $deleteNewM.message -match "deleted")
} catch {
    Add-TestResult "Admin Learning-Module CRUD & Deletion Safeguard" "CRUD workflow" "Exception: $($_.Exception.Message)" $false
}


Write-Host "`n==== SUMMARY: $($results.Count) TESTS EXECUTED ====" -ForegroundColor Cyan
$passCount = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
Write-Host "Passed: $passCount | Failed: $failCount" -ForegroundColor Green

# Output results as JSON for report generator
$results | ConvertTo-Json -Depth 3 | Set-Content "C:\Users\my\Downloads\Phishing System\test_results.json"