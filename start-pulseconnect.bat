@echo off
setlocal
REM Pulse Connect quick launcher (Windows .bat)
REM - Double-click this file to start the application on http://localhost:8081
REM - Requires Java (JRE/JDK) on PATH

set "APP_DIR=%~dp0"
cd /d "%APP_DIR%"

set "JAR=target\blood-donor-directory-1.0.0.jar"
if not exist "%JAR%" (
  echo [PulseConnect] JAR not found at "%JAR%".
  echo Build the project first, then re-run this script.
  echo Suggested: mvn -DskipTests package
  pause
  exit /b 1
)

echo [PulseConnect] Starting on http://localhost:8081 ...
start "PulseConnect" cmd /c java -jar "%JAR%"

REM Optional: to use live static files from src/main/resources/static without rebuild,
REM uncomment the next block and comment out the simple start above.
REM powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
REM   "$p=Split-Path -Parent '%~f0'; ^
REM    $s=Join-Path $p 'src\main\resources\static'; ^
REM    $u=(New-Object System.Uri($s)).AbsoluteUri; ^
REM    Start-Process -FilePath 'java' -ArgumentList @('-jar', '%JAR%', "--spring.web.resources.static-locations=$u") -WindowStyle Minimized"

exit /b 0
