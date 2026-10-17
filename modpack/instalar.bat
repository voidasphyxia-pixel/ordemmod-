@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0instalar.ps1" -Instancia %1
