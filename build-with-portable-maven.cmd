@echo off
REM Build script that downloads portable Maven if needed
REM This allows building without requiring Maven to be installed system-wide

setlocal enabledelayedexpansion

set "MAVEN_VERSION=3.9.6"
set "MAVEN_HOME=%~dp0.maven\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"
set "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

echo ========================================
echo SysML v2 Validator Build Script
echo ========================================
echo.

REM Check if Maven is already in PATH
where mvn >nul 2>&1
if %ERRORLEVEL%==0 (
    echo [INFO] Found Maven in PATH
    set "MVN_CMD=mvn"
    goto :build
)

REM Check if portable Maven exists
if exist "%MAVEN_BIN%" (
    echo [INFO] Found portable Maven at %MAVEN_HOME%
    set "MVN_CMD=%MAVEN_BIN%"
    goto :build
)

echo [INFO] Maven not found. Downloading portable Maven %MAVEN_VERSION%...
echo [INFO] This is a one-time download (~9 MB)
echo.

REM Create .maven directory
if not exist "%~dp0.maven" mkdir "%~dp0.maven"

REM Download Maven using PowerShell
echo [INFO] Downloading from %MAVEN_URL%...
powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%~dp0.maven\maven.zip'"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to download Maven
    echo [ERROR] Please install Maven manually or check internet connection
    pause
    exit /b 1
)

echo [INFO] Download complete. Extracting...

REM Extract using PowerShell
powershell -Command "Expand-Archive -Path '%~dp0.maven\maven.zip' -DestinationPath '%~dp0.maven' -Force"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to extract Maven
    pause
    exit /b 1
)

REM Clean up zip file
del "%~dp0.maven\maven.zip"

echo [INFO] Maven extracted successfully
echo.

if not exist "%MAVEN_BIN%" (
    echo [ERROR] Maven binary not found after extraction
    echo [ERROR] Expected at: %MAVEN_BIN%
    pause
    exit /b 1
)

set "MVN_CMD=%MAVEN_BIN%"

:build
echo ========================================
echo Building Project
echo ========================================
echo Maven Command: %MVN_CMD%
echo Project Directory: %~dp0
echo.

REM Run Maven build
cd /d "%~dp0"
call "%MVN_CMD%" clean test

set "BUILD_RESULT=%ERRORLEVEL%"

echo.
echo ========================================
echo Build Summary
echo ========================================
if %BUILD_RESULT%==0 (
    echo Status: SUCCESS
    echo All tests passed!
) else (
    echo Status: FAILED
    echo Exit code: %BUILD_RESULT%
)
echo ========================================

pause
exit /b %BUILD_RESULT%
