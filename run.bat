@echo off
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
if errorlevel 1 (
  echo Compilation failed. Please install JDK 17 or newer.
  pause
  exit /b 1
)
xcopy /e /i /y data out\data >nul
xcopy /e /i /y web out\web >nul
java -cp out sentiment.Main
