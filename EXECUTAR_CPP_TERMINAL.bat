@echo off
cd /d "%~dp0"
if not exist build-cpp\hemotrack_cpp.exe call compilar_cpp.bat
if errorlevel 1 exit /b 1
build-cpp\hemotrack_cpp.exe --terminal
pause
