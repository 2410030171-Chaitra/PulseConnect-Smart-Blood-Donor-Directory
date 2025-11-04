# Creates a Desktop shortcut that auto-starts the server (if needed) and opens the app.

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$desktop = [Environment]::GetFolderPath('Desktop')

$wsh = New-Object -ComObject WScript.Shell
$lnkPath = Join-Path $desktop 'Pulse Connect.lnk'
$shortcut = $wsh.CreateShortcut($lnkPath)
$shortcut.TargetPath = 'powershell.exe'
$shortcut.Arguments = "-NoLogo -NoProfile -ExecutionPolicy Bypass -File `"$scriptDir\open-pulseconnect.ps1`""
$shortcut.WorkingDirectory = $scriptDir
$shortcut.WindowStyle = 7  # Minimized
$shortcut.Description = 'Open Pulse Connect (auto-starts server if needed)'
$shortcut.IconLocation = "$env:SystemRoot\System32\shell32.dll,276"
$shortcut.Save()

Write-Host "Shortcut created on Desktop: $lnkPath" -ForegroundColor Green
