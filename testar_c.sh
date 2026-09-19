#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build-c
cc -std=c11 -Wall -Wextra -Werror -pedantic c/rota_vital.c c/testes.c -o build-c/testes
./build-c/testes
cc -std=c11 -Wall -Wextra -Werror -pedantic -DTESTAR_FALHAS -fsanitize=address,undefined -fno-omit-frame-pointer -g c/rota_vital.c c/testes.c -o build-c/testes_memoria
ASAN_OPTIONS=detect_leaks=0 ./build-c/testes_memoria
