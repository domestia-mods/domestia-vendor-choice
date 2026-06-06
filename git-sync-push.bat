@echo off
setlocal

REM Pull latest remote changes, then stage, commit, and push local changes.
REM The first command-line argument is used as the commit message.
REM If no message is provided, a safe default is used.

set "COMMIT_MESSAGE=%~1"

if "%COMMIT_MESSAGE%"=="" (
    set "COMMIT_MESSAGE=Update project documentation"
)

echo.
echo === Git status before pull ===
git status

echo.
echo === Pulling latest changes from origin/main ===
git pull origin main

if errorlevel 1 (
    echo.
    echo Pull failed. Resolve conflicts or local changes first.
    pause
    exit /b 1
)

echo.
echo === Staging all changes ===
git add .

echo.
echo === Git status after staging ===
git status

echo.
echo === Creating commit ===
git commit -m "%COMMIT_MESSAGE%"

if errorlevel 1 (
    echo.
    echo Commit was not created. There may be no changes to commit or Git reported an error.
    pause
    exit /b 1
)

echo.
echo === Pushing to remote ===
git push

if errorlevel 1 (
    echo.
    echo Push failed.
    pause
    exit /b 1
)

echo.
echo Done.
pause