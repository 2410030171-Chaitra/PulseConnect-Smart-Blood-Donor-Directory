# Open Pulse Connect
# Starts the server if not running, then opens the site in your default browser.

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

function Test-PortListening {
  param([int]$Port)
  try {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return [bool]$conn
  } catch {
    return $false
  }
}

if (!(Test-Path $jar)) {
  Write-Host "[PulseConnect] JAR not found at $jar" -ForegroundColor Yellow
  Write-Host "Build the project first (e.g., mvn -DskipTests package)."
  exit 1
}

$needsStart = -not (Test-PortListening -Port $port)
if ($needsStart) {
  Write-Host "[PulseConnect] Server not running. Starting now..." -ForegroundColor Cyan
  $staticPath = Join-Path $ScriptDir 'src\main\resources\static'
  $staticUri = (New-Object System.Uri($staticPath)).AbsoluteUri
  Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar, "--spring.web.resources.static-locations=$staticUri") -WindowStyle Minimized

  # Wait until the port is listening (max ~30s)
  $deadline = (Get-Date).AddSeconds(30)
  while (-not (Test-PortListening -Port $port)) {
    if ((Get-Date) -gt $deadline) {
      Write-Host "[PulseConnect] Timed out waiting for server to start on port $port." -ForegroundColor Yellow
      break
    }
    Start-Sleep -Milliseconds 500
  }
}

Write-Host "[PulseConnect] Opening http://localhost:$port ..." -ForegroundColor Green
Start-Process "http://localhost:$port"
