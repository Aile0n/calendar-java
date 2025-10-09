@echo off
setlocal

REM Build script to create a runnable shaded JAR for Calendar Java
REM Requires a local Maven installation (mvn) in PATH.

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven (mvn) wurde nicht gefunden.^([0m^)
  echo Bitte Maven installieren und in den PATH aufnehmen, oder das Projekt mit einer IDE bauen.
  echo Anleitung: https://maven.apache.org/install.html
  exit /b 1
)

echo [INFO] Baue Shaded JAR mit Maven...
call mvn clean package
if errorlevel 1 (
  echo [ERROR] Build fehlgeschlagen. Bitte die Maven-Ausgabe oben prfen.
  exit /b %ERRORLEVEL%
)

set "JAR="
for /f "delims=" %%f in ('dir /b /a:-d target\*-shaded.jar 2^>nul') do set "JAR=%%f"
if not defined JAR (
  echo [WARN] Es wurde keine *-shaded.jar im Ordner target\ gefunden.
  echo Bitte das target-Verzeichnis prfen.
  exit /b 0
)

echo [INFO] Fertige JAR: target\%JAR%
echo [INFO] Starten mit:
echo     java -jar target\%JAR%

exit /b 0
