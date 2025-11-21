@echo off
REM Script to copy generated files from outputs/ to genoutput/ folder
REM This follows the structure described in genoutput/README.md

setlocal

REM Get the project root directory (parent of scripts/)
cd /d "%~dp0\.."
set PROJECT_ROOT=%CD%

echo ========================================
echo Copying files from outputs to genoutput
echo ========================================
echo.

REM Create genoutput directory structure if it doesn't exist
if not exist "%PROJECT_ROOT%\..\genoutput" (
    echo Creating genoutput directory...
    mkdir "%PROJECT_ROOT%\..\genoutput"
)

if not exist "%PROJECT_ROOT%\..\genoutput\in\ac\iiitb\plproject\atc\generated" (
    echo Creating package directory structure...
    mkdir "%PROJECT_ROOT%\..\genoutput\in\ac\iiitb\plproject\atc\generated"
)

REM Copy GeneratedATCs.java to package structure
if exist "%PROJECT_ROOT%\outputs\GeneratedATCs.java" (
    echo Copying GeneratedATCs.java...
    copy /Y "%PROJECT_ROOT%\outputs\GeneratedATCs.java" "%PROJECT_ROOT%\..\genoutput\in\ac\iiitb\plproject\atc\generated\GeneratedATCs.java"
    echo   [OK] GeneratedATCs.java copied
) else (
    echo   [WARNING] GeneratedATCs.java not found in outputs/
)

REM Copy .jpf files to genoutput root
if exist "%PROJECT_ROOT%\outputs\GeneratedATCs_increment_helper.jpf" (
    echo Copying GeneratedATCs_increment_helper.jpf...
    copy /Y "%PROJECT_ROOT%\outputs\GeneratedATCs_increment_helper.jpf" "%PROJECT_ROOT%\..\genoutput\GeneratedATCs_increment_helper.jpf"
    echo   [OK] GeneratedATCs_increment_helper.jpf copied
) else (
    echo   [WARNING] GeneratedATCs_increment_helper.jpf not found in outputs/
)

if exist "%PROJECT_ROOT%\outputs\GeneratedATCs_process_helper.jpf" (
    echo Copying GeneratedATCs_process_helper.jpf...
    copy /Y "%PROJECT_ROOT%\outputs\GeneratedATCs_process_helper.jpf" "%PROJECT_ROOT%\..\genoutput\GeneratedATCs_process_helper.jpf"
    echo   [OK] GeneratedATCs_process_helper.jpf copied
) else (
    echo   [WARNING] GeneratedATCs_process_helper.jpf not found in outputs/
)

REM Copy main.jpf file (primary test file for the complete sequence)
if exist "%PROJECT_ROOT%\outputs\GeneratedATCs_main.jpf" (
    echo Copying GeneratedATCs_main.jpf...
    copy /Y "%PROJECT_ROOT%\outputs\GeneratedATCs_main.jpf" "%PROJECT_ROOT%\..\genoutput\GeneratedATCs_main.jpf"
    echo   [OK] GeneratedATCs_main.jpf copied (PRIMARY TEST FILE)
) else (
    echo   [WARNING] GeneratedATCs_main.jpf not found in outputs/
)

REM Check if Helper.java exists, if not warn
if not exist "%PROJECT_ROOT%\..\genoutput\in\ac\iiitb\plproject\atc\generated\Helper.java" (
    echo   [WARNING] Helper.java not found in genoutput - make sure it exists!
)

echo.
echo ========================================
echo Copy completed!
echo ========================================
echo.
echo Files copied to: %PROJECT_ROOT%\..\genoutput
echo.
echo Structure:
echo   genoutput/
echo     in/ac/iiitb/plproject/atc/generated/
echo       GeneratedATCs.java
echo       Helper.java
echo     GeneratedATCs_main.jpf (PRIMARY - test complete sequence)
echo     GeneratedATCs_increment_helper.jpf
echo     GeneratedATCs_process_helper.jpf
echo.

pause

