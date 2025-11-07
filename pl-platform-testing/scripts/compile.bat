@echo off
REM Batch file wrapper for Maven compile (via WSL)
REM This is now a simple wrapper around Maven
REM Run from project root or scripts folder

setlocal

REM Change to project root directory (parent of scripts folder)
cd /d "%~dp0.."

echo.
echo ========================================
echo   Compiling with Maven (via WSL)
echo ========================================
echo.

wsl mvn compile

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Compilation successful!
echo.
pause

