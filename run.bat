@echo off
title SCIS - Quick Run
color 0B

echo ============================================
echo   SCIS v2 - Quick Run
echo ============================================
echo.

REM ── Locate project root ──────────────────────────────────────────────
set "ROOT=%~dp0"

REM ── Check Java ───────────────────────────────────────────────────────
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found in PATH.
    echo  Install JDK 11+ from https://adoptium.net then re-run.
    pause
    exit /b 1
)

REM ── Locate jar ───────────────────────────────────────────────────────
set "JAR=%ROOT%frontend\target\SCIS-app.jar"
if not exist "%JAR%" (
    echo [ERROR] SCIS-app.jar not found.
    echo         Please run compile.bat first to build the project.
    pause
    exit /b 1
)

REM ── Pre-configured SMTP credentials ──────────────────────────────────
set "SMTP_USER=keshav8a7841@gmail.com"
set "SMTP_PASS=pphhzwyfsdydrycz"

echo [INFO] Email OTP configured for: %SMTP_USER%
echo [INFO] Starting SCIS application...
echo [INFO] MongoDB must be running on localhost:27017
echo.

java -Dscis.smtp.host=smtp.gmail.com ^
     -Dscis.smtp.port=587 ^
     -Dscis.smtp.user="%SMTP_USER%" ^
     -Dscis.smtp.pass="%SMTP_PASS%" ^
     -jar "%JAR%"

if errorlevel 1 (
    echo.
    echo [ERROR] Application exited with an error. Common fixes:
    echo.
    echo   1. MongoDB not running:
    echo      Start MongoDB service or run: mongod --dbpath C:\data\db
    echo.
    echo   2. OTP email not sending:
    echo      - Confirm 2-Step Verification is ON for keshav8a7841@gmail.com
    echo      - App Password may have expired; generate a new one at:
    echo        Google Account ^> Security ^> App Passwords
    echo      - Then open run.bat in Notepad and update SMTP_PASS
    echo.
    echo   3. Java version too old:
    echo      Run: java -version  (need 11 or higher)
    echo.
    pause
)
