@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process PowerShell -Verb RunAs -ArgumentList '-NoProfile -ExecutionPolicy Bypass -File ""%~dp0register-postgres-service.ps1""'" 
endlocal
