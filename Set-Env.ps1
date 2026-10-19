# Set-Env.ps1 - Load .env file variables into current PowerShell session
# Usage: . .\Set-Env.ps1
# Note: The dot-space prefix (. .\Set-Env.ps1) is required so variables export into the active shell.

param(
    [string]$EnvFile = ".env"
)

if (-not (Test-Path $EnvFile)) {
    Write-Host "[!] File '$EnvFile' not found." -ForegroundColor Yellow
    Write-Host "    Create it by copying .env.example:" -ForegroundColor Cyan
    Write-Host "    Copy-Item .env.example .env" -ForegroundColor White
    return
}

$count = 0
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $eqPos = $line.IndexOf('=')
    if ($eqPos -lt 1) { return }
    $name  = $line.Substring(0, $eqPos).Trim()
    $value = $line.Substring($eqPos + 1).Trim()
    [System.Environment]::SetEnvironmentVariable($name, $value, 'Process')
    Write-Host "  -> Set $name" -ForegroundColor Cyan
    $count++
}

Write-Host ""
Write-Host "[+] Loaded $count environment variables from '$EnvFile'." -ForegroundColor Green
Write-Host "    Variables are active for this PowerShell terminal session." -ForegroundColor Gray