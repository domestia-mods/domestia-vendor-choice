@echo off
setlocal

REM =====================================================================
REM Git helper: pull latest changes from origin/main, then stage, commit,
REM and push local changes.
REM
REM Usage:
REM   git-sync-push.bat
REM   git-sync-push.bat "Update README layout"
REM
REM Notes:
REM   This script uses the full path to git.exe because Git is installed
REM   but is not currently available through PATH.
REM =====================================================================

set "GIT_EXE=C:\Program Files\Git\cmd\git.exe"

REM Use the first command-line argument as the commit message.
REM If no message is provided, use a safe default.
set "COMMIT_MESSAGE=%~1"

if "%COMMIT_MESSAGE%"=="" (
    set "COMMIT_MESSAGE=Update project documentation"
)

if not exist "%GIT_EXE%" (
    echo.
    echo ERROR: Git executable was not found:
    echo %GIT_EXE%
    echo.
    echo Update GIT_EXE in this file to match your Git installation path.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Git executable
echo ============================================================
"%GIT_EXE%" --version

echo.
echo ============================================================
echo Git status before pull
echo ============================================================
"%GIT_EXE%" status

if errorlevel 1 (
    echo.
    echo ERROR: Git status failed.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Pulling latest changes from origin/main
echo ============================================================
"%GIT_EXE%" pull origin main

if errorlevel 1 (
    echo.
    echo ERROR: Pull failed.
    echo Resolve conflicts or local changes first.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Staging all changes
echo ============================================================
"%GIT_EXE%" add .

if errorlevel 1 (
    echo.
    echo ERROR: Git add failed.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Git status after staging
echo ============================================================
"%GIT_EXE%" status

if errorlevel 1 (
    echo.
    echo ERROR: Git status failed after staging.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Creating commit
echo Message: %COMMIT_MESSAGE%
echo ============================================================
"%GIT_EXE%" commit -m "%COMMIT_MESSAGE%"

if errorlevel 1 (
    echo.
    echo Commit was not created.
    echo This usually means there are no staged changes, or Git reported an error.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Pushing to remote
echo ============================================================
"%GIT_EXE%" push

if errorlevel 1 (
    echo.
    echo ERROR: Push failed.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo Done.
echo ============================================================
pause