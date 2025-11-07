@echo off
REM Batch file to run all test cases
REM Run from project root or scripts folder

setlocal

REM Change to project root directory (parent of scripts folder)
cd /d "%~dp0.."

REM Call run-tests.bat with 'all' argument (from scripts folder)
call scripts\run-tests.bat all

