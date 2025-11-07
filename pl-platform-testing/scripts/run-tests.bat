@echo off
REM Batch file wrapper for Maven exec:java (via WSL)
REM Usage: run-tests.bat [simple|complex|all]
REM This is now a simple wrapper around Maven
REM Run from project root or scripts folder

setlocal

REM Change to project root directory (parent of scripts folder)
cd /d "%~dp0.."

echo.
echo ========================================
echo   NewGenATC Algorithm - Test Runner
echo ========================================
echo.

REM Determine test case to run
set TEST_CASE=%1
if "%TEST_CASE%"=="" set TEST_CASE=simple

REM Compile and run with Maven via WSL
echo Compiling and running test case: %TEST_CASE%
echo.

if "%TEST_CASE%"=="" (
    wsl mvn compile exec:java
) else (
    wsl mvn compile exec:java -Dexec.args="%TEST_CASE%"
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Test execution failed!
    pause
    exit /b 1
)

echo.
echo Test completed successfully!
echo.
pause

