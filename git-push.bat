@echo off
setlocal

REM Use the first command-line argument as the commit message.
REM If no message is provided, use a safe default.
set "COMMIT_MESSAGE=%~1"

if "%COMMIT_MESSAGE%"=="" (
    set "COMMIT_MESSAGE=Update project files"
)

echo.
echo === Git status before staging ===
git status

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