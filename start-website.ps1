$ErrorActionPreference = 'Stop'

# Start Website: one-click launcher for Pulse Connect (Windows PowerShell)
# - Builds the app if the JAR is missing (requires Maven if build is needed)
# - Starts Spring Boot with live static files and Mongo sync disabled for faster local startup
# - Waits until http://localhost:8081 is reachable, then opens your browser

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$Port    = 8081
$Jar     = Join-Path $ScriptDir 'target\blood-donor-directory-1.0.0.jar'
$LogsDir = Join-Path $ScriptDir 'logs'
$LogFile = Join-Path $LogsDir 'pulseconnect.log'
if (-not (Test-Path $LogsDir)) { New-Item -ItemType Directory -Path $LogsDir | Out-Null }

function Write-Info($msg)  { Write-Host "[StartWebsite] $msg" -ForegroundColor Cyan }
function Write-Warn($msg)  { Write-Host "[StartWebsite] $msg" -ForegroundColor Yellow }
function Write-Err($msg)   { Write-Host "[StartWebsite] $msg" -ForegroundColor Red }

function Load-DotEnv {
  $dotenv = Join-Path $ScriptDir '.env'
  if (Test-Path $dotenv) {
    Write-Info "Loading environment from .env"
    foreach ($line in Get-Content $dotenv) {
      if ($line -match '^[\s]*#' -or $line -match '^[\s]*$') { continue }
      if ($line -match '^[\s]*([^=]+)[\s]*=[\s]*(.*)$') {
        $key = $Matches[1].Trim()
        $val = $Matches[2].Trim().Trim('"').Trim("'")
        [Environment]::SetEnvironmentVariable($key, $val, 'Process')
      }
    }
  }
}

function Resolve-JavaCmd {
  $candidates = @()
  $fromPath = (Get-Command java -ErrorAction SilentlyContinue | Select-Object -First 1).Path
  if ($fromPath) { $candidates += $fromPath }
  if ($env:JAVA_HOME) {
    $home = Join-Path $env:JAVA_HOME 'bin\\java.exe'
    if (Test-Path $home) { $candidates += $home }
  }
  foreach ($root in @('C:\\Program Files\\Java','C:\\Program Files (x86)\\Java')) {
    if (Test-Path $root) {
      $jdkBins = Get-ChildItem -Path $root -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName 'bin\\java.exe' } |
        Where-Object { Test-Path $_ }
      $candidates += $jdkBins
    }
  }
  $seen = @{}
  foreach ($c in $candidates) {
    if ($seen.ContainsKey($c)) { continue } else { $seen[$c] = $true }
    try { & $c -version 1>$null 2>$null; return $c } catch { }
  }
  return $null
}

function Test-PortListening($Port) {
  try {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return [bool]$conn
  } catch {
    # fallback on Test-NetConnection
    try { return (Test-NetConnection -ComputerName localhost -Port $Port).TcpTestSucceeded } catch { return $false }
  }
}

function Ensure-Jar {
  if (Test-Path $Jar) { return }
  Write-Info "JAR not found. Attempting to build with Maven (tests skipped)..."
  try {
    $mvn = Get-Command mvn -ErrorAction Stop | Select-Object -First 1
  } catch {
    Write-Err "Maven not found. Please install Maven or build once from VS Code (Maven: package)."
    Write-Host "You can also run:" -ForegroundColor Gray
    Write-Host "  mvn -DskipTests package" -ForegroundColor Gray
    exit 1
  }
  & $mvn.Path -DskipTests package
  if (-not (Test-Path $Jar)) {
    Write-Err "Build completed but JAR still missing: $Jar"
    exit 1
  }
}

function Start-App {
  $staticPath = Join-Path $ScriptDir 'src\main\resources\static'
  $staticUri  = (New-Object System.Uri($staticPath)).AbsoluteUri

  if (Test-PortListening -Port $Port) {
    Write-Info "Server already running on port $Port."
    return
  }

  Write-Info "Starting app on http://localhost:$Port (Mongo sync disabled). Logs: $LogFile"
  $args = @(
    '-jar', $Jar,
    "--spring.web.resources.static-locations=$staticUri",
    '--mongo.sync.enabled=false',
    "--logging.file.name=$LogFile"
  )
  Start-Process -FilePath $script:JavaCmd -ArgumentList $args -WindowStyle Minimized | Out-Null

  # Wait up to 90s for readiness
  $deadline = (Get-Date).AddSeconds(90)
  while (-not (Test-PortListening -Port $Port)) {
    if ((Get-Date) -gt $deadline) {
      Write-Warn "Timed out waiting for server to listen on port $Port."
      if (Test-Path $LogFile) {
        Write-Host "----- Last 200 log lines ($LogFile) -----" -ForegroundColor Gray
        Get-Content -Path $LogFile -Tail 200
        Write-Host "----------------------------------------" -ForegroundColor Gray
      }
      exit 1
    }
    Start-Sleep -Milliseconds 500
  }
}

# Main
Load-DotEnv

# Resolve Java
$script:JavaCmd = Resolve-JavaCmd
if (-not $script:JavaCmd) {
  Write-Warn "Java not auto-detected. Will try 'java' from PATH; if it fails, install JDK 21+ and set JAVA_HOME."
  $script:JavaCmd = 'java'
}

Ensure-Jar
Start-App

Write-Info "Opening http://localhost:$Port ..."
Start-Process "http://localhost:$Port"
Write-Host "All set. If the page doesn't load immediately, refresh after a few seconds." -ForegroundColor Green
