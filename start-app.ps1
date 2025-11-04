$ErrorActionPreference = 'Stop'

# Move to script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# Load .env into process environment
$envPath = Join-Path $scriptDir '.env'
if (Test-Path $envPath) {
  Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) { return }
    $kv = $line -split '=', 2
    if ($kv.Length -eq 2) {
      [Environment]::SetEnvironmentVariable($kv[0].Trim(), $kv[1].Trim(), 'Process')
    }
  }
}

# Ensure logs directory
$logsDir = Join-Path $scriptDir 'logs'
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir | Out-Null }
$logFile = Join-Path $logsDir 'pulseconnect.log'

# Jar path
$jar = Join-Path $scriptDir 'target\blood-donor-directory-1.0.0.jar'
if (-not (Test-Path $jar)) { Write-Error "Jar not found: $jar"; exit 1 }

# Start app minimized and redirect logs (using cmd redirection for PS 5.1)
$cmd = 'cmd.exe'
$args = "/c java -jar `"$jar`" --spring.web.resources.static-locations=`"file:src/main/resources/static/,classpath:/static/`" >> `"$logFile`" 2>&1"
$proc = Start-Process -FilePath $cmd -ArgumentList $args -PassThru -WindowStyle Minimized
Write-Host "Started java via cmd PID=$($proc.Id)"

# Wait up to ~60s for port 8081 to listen
$ok = $false
for ($i=0; $i -lt 30; $i++) {
  Start-Sleep -Seconds 2
  if ((Test-NetConnection -ComputerName localhost -Port 8081).TcpTestSucceeded) { $ok = $true; break }
}
if ($ok) { Write-Host 'Server is listening on 8081' } else { Write-Host 'Server did not start within timeout' }