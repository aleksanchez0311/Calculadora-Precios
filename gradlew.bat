@echo off
setlocal enabledelayedexpansion
set VERSION=8.5.1
nset SCRIPT_DIR=%~dp0
nset DIST_DIR=%SCRIPT_DIR%gradle\gradle-%VERSION%
if not exist "%DIST_DIR%\bin\gradle.bat" (
  echo Gradle %VERSION% no encontrado localmente. Descargando...
  powershell -Command "(New-Object System.Net.WebClient).DownloadFile('https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip', '$env:TEMP\\gradle.zip')"
  powershell -Command "Expand-Archive -Path $env:TEMP\\gradle.zip -DestinationPath '%SCRIPT_DIR%gradle' -Force"
  del /f /q "%TEMP%\gradle.zip"
)
"%DIST_DIR%\bin\gradle.bat" %*
