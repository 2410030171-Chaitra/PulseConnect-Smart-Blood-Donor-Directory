# Pulse Connect quick launcher (Windows PowerShell)
# - Double-click to start the app minimized on http://localhost:8081
# - Requires Java (JRE/JDK) on PATH

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
try {
  $listening = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
} catch {
  $listening = $null
}

if ($listening) {
  Write-Host "[PulseConnect] Already running on port $port." -ForegroundColor Green
  exit 0
}

$jar = Join-Path $ScriptDir 'target\blood-donor-directory-1.0.0.jar'
if (!(Test-Path $jar)) {
  Write-Host "[PulseConnect] JAR not found at $jar" -ForegroundColor Yellow
  Write-Host "Build the project first (e.g., mvn -DskipTests package)."
  exit 1
}

# Use live static files from src/main/resources/static so UI changes reflect without rebuild
$staticPath = Join-Path $ScriptDir 'src\main\resources\static'
$staticUri = (New-Object System.Uri($staticPath)).AbsoluteUri

# Ensure logs folder exists and start with file logging for easier diagnostics
New-Item -ItemType Directory -Force -Path "$ScriptDir\logs" | Out-Null
Write-Host "[PulseConnect] Starting on http://localhost:$port ... (logging to .\\logs\\pulseconnect.log)" -ForegroundColor Cyan
Start-Process -FilePath 'java' -ArgumentList @(
  '-jar', $jar,
  "--spring.web.resources.static-locations=$staticUri",
  '--logging.file.name=logs\\pulseconnect.log'
) -WindowStyle Minimized
