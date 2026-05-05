@echo off
setlocal enabledelayedexpansion

set TEST_DIR=E:\_Documents\git\SysML-v2-Release\sysml\src\examples\Simple Tests
set JAR_PATH=E:\_Documents\git\sysml-validator\validator-cli\target\sysml-validator.jar

set PASSED=0
set FAILED=0

for %%f in ("%TEST_DIR%\*.sysml") do (
    java -jar "%JAR_PATH%" "%%f" > nul 2>&1
    if !errorlevel! equ 0 (
        echo PASS: %%~nxf
        set /a PASSED+=1
    ) else (
        echo FAIL: %%~nxf
        set /a FAILED+=1
    )
)

set /a TOTAL=PASSED+FAILED
echo.
echo Summary: %PASSED% passed, %FAILED% failed out of %TOTAL% total
