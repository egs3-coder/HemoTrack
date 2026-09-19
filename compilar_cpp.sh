#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build-cpp
cc -std=c11 -Wall -Wextra -Werror -pedantic -c c/rota_vital.c -o build-cpp/rota_vital.o
c++ -std=c++17 -O1 -pthread -Wall -Wextra cpp/programa.cpp build-cpp/rota_vital.o -o build-cpp/hemotrack_cpp
printf 'Compilado. Terminal: ./build-cpp/hemotrack_cpp --terminal\nTelas: ./build-cpp/hemotrack_cpp\n'
