@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1"
set "taskExitCode=%ERRORLEVEL%"
if not "%taskExitCode%"=="0" pause
exit /b %taskExitCode%
