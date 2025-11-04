@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "JAR=%SCRIPT_DIR%target\blood-donor-directory-1.0.0.jar"
if not exist "%JAR%" (
  echo [PulseConnect] JAR not found at "%JAR%"
  echo Build the project first: mvn -DskipTests package
  pause
  exit /b 1
)

REM Resolve absolute file URI for static path
for /f "delims=" %%I in ('powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "(New-Object System.Uri('%SCRIPT_DIR%src\main\resources\static')).AbsoluteUri"') do set STATIC_URI=%%I

echo [PulseConnect] Starting (MySQL) on http://localhost:8081 ... (logs: .\logs\pulseconnect-mysql.log)
start "PulseConnect-MySQL" cmd /c java -jar "%JAR" --spring.profiles.active=mysql --spring.web.resources.static-locations="%STATIC_URI%" --logging.file.name="logs\pulseconnect-mysql.log"

exit /b 0
