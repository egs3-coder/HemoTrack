#ifndef DADOS_EXEMPLO_H
#define DADOS_EXEMPLO_H
#include "rota_vital.h"
#include <stdio.h>
/* Apenas fixtures; não faz parte das regras de produção. */
static Bolsa bolsa_exemplo(int id, TipoSanguineo tipo, int volume) {
    Bolsa bolsa = {.id=id, .tipo=tipo, .volume_ml=volume,
        .hemocomponente=CONCENTRADO_HEMACIAS,.data_coleta="2026-01-01",.data_validade="2026-02-01"};
    snprintf(bolsa.codigo, sizeof bolsa.codigo, "B-%d", id);
    return bolsa;
}
#endif
