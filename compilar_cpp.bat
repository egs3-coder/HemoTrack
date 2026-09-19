@echo off
cd /d "%~dp0"
if not exist build-cpp mkdir build-cpp
gcc -std=c11 -Wall -Wextra -Werror -pedantic -c c\rota_vital.c -o build-cpp\rota_vital.o
if errorlevel 1 exit /b 1
g++ -std=c++17 -O1 -pthread cpp\programa.cpp build-cpp\rota_vital.o -o build-cpp\hemotrack_cpp.exe -lws2_32
if errorlevel 1 exit /b 1
echo Compilado. Terminal: build-cpp\hemotrack_cpp.exe --terminal
echo Telas: build-cpp\hemotrack_cpp.exe
