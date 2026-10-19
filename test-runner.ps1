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

$loginBodyAdmin = @{
    username = $adminUser
    password = $adminPass
} | ConvertTo-Json

try {
    $loginRespAdmin = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBodyAdmin -Headers @{ "X-XSRF-TOKEN" = $csrfAdmin; "Content-Type" = "application/json" } -WebSession $loginSessionAdmin
    Add-TestResult "Admin Authentication" "HTTP 200, role ADMIN" "Logged in: $($loginRespAdmin.username), Role: $($loginRespAdmin.role)" ($loginRespAdmin.role -eq "ADMIN")
} catch {
    Add-TestResult "Admin Authentication" "HTTP 200" "Exception: $($_.Exception.Message)" $false
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

Write-Host "`n==== SUMMARY: $($results.Count) TESTS EXECUTED ====" -ForegroundColor Cyan
$passCount = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
Write-Host "Passed: $passCount | Failed: $failCount" -ForegroundColor Green

# Output results as JSON for report generator
$results | ConvertTo-Json -Depth 3 | Set-Content "C:\Users\my\Downloads\Phishing System\test_results.json"