@echo off
title SCIS - Full Compile + Test
color 0A

echo ============================================
echo   SCIS v2 - Full Compile + Test Suite
echo ============================================
echo.

REM ── Locate project root (folder where this .bat lives) ──────────────
set "ROOT=%~dp0"

REM ── Check Java ───────────────────────────────────────────────────────
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found in PATH.
    echo.
    echo  Fix: Install JDK 11 or later from https://adoptium.net
    echo       Then add the JDK bin folder to your PATH environment variable.
    echo       Example: C:\Program Files\Eclipse Adoptium\jdk-21\bin
    echo.
    pause
    exit /b 1
)
echo [OK] Java found.

REM ── Locate mvn (check PATH, then common install locations) ───────────
set "MVN_CMD=mvn"
mvn -version >nul 2>&1
if errorlevel 1 (
    REM Try common Maven install paths
    if exist "C:\Program Files\apache-maven\bin\mvn.cmd" (
        set "MVN_CMD=C:\Program Files\apache-maven\bin\mvn.cmd"
        goto :mvn_found
    )
    if exist "C:\maven\bin\mvn.cmd" (
        set "MVN_CMD=C:\maven\bin\mvn.cmd"
        goto :mvn_found
    )
    if exist "C:\tools\maven\bin\mvn.cmd" (
        set "MVN_CMD=C:\tools\maven\bin\mvn.cmd"
        goto :mvn_found
    )
    REM Try user home
    if exist "%USERPROFILE%\maven\bin\mvn.cmd" (
        set "MVN_CMD=%USERPROFILE%\maven\bin\mvn.cmd"
        goto :mvn_found
    )
    REM Try using mvnw wrapper if it exists in root
    if exist "%ROOT%mvnw.cmd" (
        set "MVN_CMD=%ROOT%mvnw.cmd"
        goto :mvn_found
    )
    echo [ERROR] Maven (mvn) not found.
    echo.
    echo  Fix: Install Maven from https://maven.apache.org/download.cgi
    echo       Extract to C:\Program Files\apache-maven
    echo       Add its bin folder to PATH, e.g.:
    echo         C:\Program Files\apache-maven\bin
    echo       Then restart this command window.
    echo.
    echo  Quick install with winget (Windows 10/11):
    echo       winget install Apache.Maven
    echo.
    pause
    exit /b 1
)

:mvn_found
echo [OK] Maven found: %MVN_CMD%
echo.

REM ── Step 1: Build + test + install backend ───────────────────────────
echo [1/2] Building backend (compile + install, skipping tests)...
echo       This may take 2-5 minutes on first run (downloads dependencies).
echo.
cd /d "%ROOT%backend"
call "%MVN_CMD%" clean install -DskipTests
if errorlevel 1 (
    echo.
    echo [ERROR] Backend build or tests FAILED.
    echo         Read the errors above — they show exactly which file/line failed.
    echo.
    echo  Common fixes:
    echo    - Make sure you have JDK 11+ (not just JRE)
    echo    - Check your internet connection (Maven downloads dependencies)
    echo    - If a test failed, see the error message for which test
    echo.
    pause
    exit /b 1
)
echo.
echo [OK] Backend: compiled and installed (tests skipped).
echo.

REM ── Step 2: Build frontend fat jar ───────────────────────────────────
echo [2/2] Building frontend (compile + package as SCIS-app.jar)...
echo.
cd /d "%ROOT%frontend"
call "%MVN_CMD%" clean package -DskipTests
if errorlevel 1 (
    echo.
    echo [ERROR] Frontend build FAILED.
    echo         Make sure Step 1 (backend) completed successfully first.
    echo         The frontend depends on the backend jar being installed.
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================
echo   BUILD SUCCESSFUL
echo   JAR  : frontend\target\SCIS-app.jar
echo   Tests: Skipped (to avoid duplicate email conflicts)
echo   Next : Double-click run.bat to launch app
echo ============================================
echo.
pause
