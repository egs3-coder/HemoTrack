#include "rota_vital.h"
#include <stdio.h>
#include "dados_exemplo.h"
int main(void) {
    RotaVital rota = {0};
    Operacao operacao;
    int retorno = 1;
    if (arvore_inserir(&rota.hospitais, (Hospital){10,"Hospital Central", "Recife"}) != SUCESSO
        || lista_inserir(&rota.estoque, bolsa_exemplo(101,O_POS,450)) != SUCESSO
        || lista_inserir(&rota.estoque, bolsa_exemplo(102,O_POS,450)) != SUCESSO
        || rota_solicitar(&rota, (Requisicao){1,10,O_POS,2, CONCENTRADO_HEMACIAS, NORMAL}) != SUCESSO) goto finalizar;
    int escolhidas[] = {101,102}; /* IDs indicados pelo operador; sem seleção automática. */
    if (rota_atender(&rota, 1, escolhidas, 2, &operacao) == SUCESSO) {
        printf("Requisicao %d concluida com %d bolsas selecionadas manualmente.\n",
                operacao.requisicao_id, operacao.quantidade);
        retorno = 0;
    }
finalizar:
    rota_liberar(&rota);
    return retorno;
}
