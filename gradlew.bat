@echo off
REM Minimal Windows bootstrapper to download Gradle and run it
setlocal enabledelayedexpansion
set ROOT_DIR=%~dp0
set GRADLE_VERSION=8.5.1
set DIST_DIR=%ROOT_DIR%\.gradle-dist
set GRADLE_HOME=%DIST_DIR%\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat
if not exist "%GRADLE_BIN%" (
  echo Gradle %GRADLE_VERSION% no encontrado. Descargando...
  powershell -Command "
    $u=\"https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip\"; 
    $o=Join-Path \"%TEMP%\" \"gradle-%GRADLE_VERSION%-bin.zip\"; 
    Invoke-WebRequest -Uri $u -OutFile $o -UseBasicParsing; 
    Expand-Archive -Path $o -DestinationPath \"%DIST_DIR%\" -Force; 
    Remove-Item $o -Force;"
)
"%GRADLE_BIN%" %*
