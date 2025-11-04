# Pulse Connect launcher (MySQL profile)
# Starts the app with --spring.profiles.active=mysql and logs to logs/pulseconnect-mysql.log

$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$dotenv = Join-Path $ScriptDir '.env'
if (Test-Path $dotenv) {
  Write-Host "[PulseConnect] Loading environment from .env" -ForegroundColor Green
  foreach ($line in Get-Content $dotenv) {
    if ($line -match '^\s*#' -or $line -match '^\s*$') { continue }
    if ($line -match '^\s*([^=]+)\s*=\s*(.*)\s*$') {
      $key = $Matches[1].Trim()
      $val = $Matches[2]
      $val = $val.Trim('"').Trim("'")
      [Environment]::SetEnvironmentVariable($key, $val, 'Process')
    }
  }
}

$port = 8081
$jar = Join-Path $ScriptDir 'target\blood-donor-directory-1.0.0.jar'
if (!(Test-Path $jar)) {
  Write-Host "[PulseConnect] JAR not found at $jar" -ForegroundColor Yellow
  Write-Host "Build the project first (e.g., mvn -DskipTests package)."
  exit 1
}

# Use live static files so UI changes reflect without rebuild
$staticPath = Join-Path $ScriptDir 'src\main\resources\static'
$staticUri = (New-Object System.Uri($staticPath)).AbsoluteUri

# Ensure logs folder exists
New-Item -ItemType Directory -Force -Path (Join-Path $ScriptDir 'logs') | Out-Null

Write-Host "[PulseConnect] Starting (MySQL) on http://localhost:$port ... (logs: .\\logs\\pulseconnect-mysql.log)" -ForegroundColor Cyan
Start-Process -FilePath 'java' -ArgumentList @(
  '-jar', $jar,
  "--spring.profiles.active=mysql",
  "--spring.web.resources.static-locations=$staticUri",
  '--logging.file.name=logs\pulseconnect-mysql.log'
) -WindowStyle Minimized
