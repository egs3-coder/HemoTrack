#include "rota_vital.h"
#include <stdio.h>
#include "dados_exemplo.h"
#include <stdlib.h>
#include <string.h>
static int verificacoes;
#define VERIFICAR(condicao) do { verificacoes++; if (!(condicao)) { \
    fprintf(stderr, "Falha na linha %d: %s\n", __LINE__, #condicao); exit(1); } } while (0)
#ifdef TESTAR_FALHAS
static int falhar;
static size_t alocacoes_ativas;
void *alocar_teste(size_t tamanho) {
    void *ponteiro = falhar ? NULL : malloc(tamanho);
    if (ponteiro) alocacoes_ativas++;
    return ponteiro;
}
void liberar_teste(void *ponteiro) {
    if (ponteiro) { VERIFICAR(alocacoes_ativas > 0); alocacoes_ativas--; }
    free(ponteiro);
}
#endif
static Hospital hospital(int id) {
    Hospital h = {0}; h.id = id; snprintf(h.cidade, sizeof h.cidade, "Recife");
    snprintf(h.nome, sizeof h.nome, "Hospital %d", id);
    return h;
}
typedef struct { int ids[20]; size_t quantidade; } Coleta;
static void coletar(Hospital h, void *contexto) {
    Coleta *coleta = contexto;
    coleta->ids[coleta->quantidade++] = h.id;
}
static void verificar_ordem(ArvoreHospitais *arvore, Ordem ordem, const int *esperado, size_t tamanho) {
    Coleta coleta = {0};
    arvore_percorrer(arvore, ordem, coletar, &coleta);
    VERIFICAR(coleta.quantidade == tamanho);
    for (size_t i = 0; i < tamanho; i++) VERIFICAR(coleta.ids[i] == esperado[i]);
}
int main(void) {
    ListaEstoque lista = {0}; Bolsa bolsa;
    VERIFICAR(lista_consultar(&lista, 1, &bolsa) == NAO_ENCONTRADO);
    VERIFICAR(lista_remover(&lista, 1, &bolsa) == NAO_ENCONTRADO);
    VERIFICAR(lista_inserir(&lista, bolsa_exemplo(0, O_POS, 450)) == INVALIDO);
    VERIFICAR(lista_inserir(&lista, bolsa_exemplo(1, O_POS, 0)) == INVALIDO);
    VERIFICAR(lista_inserir(&lista, bolsa_exemplo(1, (TipoSanguineo)99, 450)) == INVALIDO);
    for (int i = 1; i <= 3; i++) VERIFICAR(lista_inserir(&lista, bolsa_exemplo(i, O_POS, 450)) == SUCESSO);
    VERIFICAR(lista_inserir(&lista, bolsa_exemplo(1, O_POS, 450)) == DUPLICADO);
    VERIFICAR(lista_contar_tipo(&lista, O_POS) == 3);
    VERIFICAR(lista_consultar(&lista, 2, &bolsa) == SUCESSO && bolsa.volume_ml == 450);
    bolsa.volume_ml = 1; /* Consulta por cópia não altera a lista. */
    VERIFICAR(lista_consultar(&lista, 2, &bolsa) == SUCESSO && bolsa.volume_ml == 450);
    VERIFICAR(lista_remover(&lista, 2, &bolsa) == SUCESSO && bolsa.id == 2); /* meio */
    VERIFICAR(lista_remover(&lista, 1, &bolsa) == SUCESSO && bolsa.id == 1); /* fim */
    VERIFICAR(lista_remover(&lista, 3, &bolsa) == SUCESSO && bolsa.id == 3); /* início */
    VERIFICAR(lista_contar_tipo(&lista, O_POS) == 0);
    puts("LISTA: vazia, validacao, duplicidade, consulta e remocoes OK");

    FilaRequisicoes fila = {0}; Requisicao requisicao;
    VERIFICAR(fila_consultar(&fila, &requisicao) == NAO_ENCONTRADO);
    VERIFICAR(fila_remover(&fila, &requisicao) == NAO_ENCONTRADO);
    VERIFICAR(fila_inserir(&fila, (Requisicao){1, 10, O_POS, 0, CONCENTRADO_HEMACIAS, NORMAL}) == INVALIDO);
    VERIFICAR(fila_inserir(&fila, (Requisicao){1, 10, O_POS, 2, CONCENTRADO_HEMACIAS, NORMAL}) == SUCESSO);
    VERIFICAR(fila_inserir(&fila, (Requisicao){2, 10, O_POS, 1, CONCENTRADO_HEMACIAS, NORMAL}) == SUCESSO);
    VERIFICAR(fila_inserir(&fila, (Requisicao){1, 10, O_POS, 1, CONCENTRADO_HEMACIAS, NORMAL}) == DUPLICADO);
    VERIFICAR(fila_buscar(&fila, 2, &requisicao) == SUCESSO && requisicao.id == 2);
    VERIFICAR(fila_consultar(&fila, &requisicao) == SUCESSO && requisicao.id == 1);
    VERIFICAR(fila_remover(&fila, &requisicao) == SUCESSO && requisicao.id == 1);
    VERIFICAR(fila_remover(&fila, &requisicao) == SUCESSO && requisicao.id == 2);
    VERIFICAR(fila.inicio == NULL && fila.fim == NULL);
    VERIFICAR(fila_inserir(&fila, (Requisicao){3, 10, O_POS, 1, CONCENTRADO_HEMACIAS, NORMAL}) == SUCESSO);
    VERIFICAR(fila_remover(&fila, &requisicao) == SUCESSO && requisicao.id == 3);
    puts("FILA: FIFO 1,2; reinsercao 3 OK");

    PilhaHistorico pilha = {0}; Operacao operacao;
    VERIFICAR(pilha_consultar(&pilha, &operacao) == NAO_ENCONTRADO);
    VERIFICAR(pilha_remover(&pilha, &operacao) == NAO_ENCONTRADO);
    VERIFICAR(pilha_inserir(&pilha, (Operacao){0, 10, 1, O_POS}) == INVALIDO);
    VERIFICAR(pilha_inserir(&pilha, (Operacao){1, 10, 2, O_POS}) == SUCESSO);
    VERIFICAR(pilha_inserir(&pilha, (Operacao){2, 10, 1, O_POS}) == SUCESSO);
    VERIFICAR(pilha_consultar(&pilha, &operacao) == SUCESSO && operacao.requisicao_id == 2);
    VERIFICAR(pilha_remover(&pilha, &operacao) == SUCESSO && operacao.requisicao_id == 2);
    VERIFICAR(pilha_remover(&pilha, &operacao) == SUCESSO && operacao.requisicao_id == 1);
    VERIFICAR(pilha_remover(&pilha, &operacao) == NAO_ENCONTRADO);
    puts("PILHA: LIFO 2,1 OK");

    ArvoreHospitais arvore = {0}; Hospital h;
    int ids[] = {50,30,70,20,40,60,80};
    int pre[] = {50,30,20,40,70,60,80};
    int em[] = {20,30,40,50,60,70,80};
    int pos[] = {20,40,30,60,80,70,50};
    VERIFICAR(arvore_contar(&arvore) == 0);
    VERIFICAR(arvore_consultar(&arvore, 1, &h) == NAO_ENCONTRADO);
    VERIFICAR(arvore_remover(&arvore, 1, &h) == NAO_ENCONTRADO);
    VERIFICAR(arvore_inserir(&arvore, (Hospital){0, "Invalido", "Recife"}) == INVALIDO);
    for (size_t i = 0; i < 7; i++) VERIFICAR(arvore_inserir(&arvore, hospital(ids[i])) == SUCESSO);
    VERIFICAR(arvore_inserir(&arvore, hospital(50)) == DUPLICADO);
    VERIFICAR(arvore_contar(&arvore) == 7);
    verificar_ordem(&arvore, PRE_ORDEM, pre, 7);
    verificar_ordem(&arvore, EM_ORDEM, em, 7);
    verificar_ordem(&arvore, POS_ORDEM, pos, 7);
    VERIFICAR(arvore_consultar(&arvore, 60, &h) == SUCESSO && h.id == 60);
    VERIFICAR(arvore_consultar(&arvore, 25, &h) == NAO_ENCONTRADO);
    VERIFICAR(arvore_remover(&arvore, 20, &h) == SUCESSO && h.id == 20);
    VERIFICAR(arvore_remover(&arvore, 30, &h) == SUCESSO && h.id == 30);
    VERIFICAR(arvore_remover(&arvore, 50, &h) == SUCESSO && h.id == 50);
    int restantes[] = {40,60,70,80};
    verificar_ordem(&arvore, EM_ORDEM, restantes, 4);
    VERIFICAR(arvore_consultar(&arvore, 60, &h) == SUCESSO && strcmp(h.nome, "Hospital 60") == 0);
    for (size_t i = 0; i < 4; i++) VERIFICAR(arvore_remover(&arvore, restantes[i], &h) == SUCESSO);
    VERIFICAR(arvore_contar(&arvore) == 0);
    puts("ABB: tres percursos; folha, um filho, dois filhos e raiz OK");

    /* Lote: colisão no último ID não insere os anteriores. */
    VERIFICAR(lista_inserir(&lista, bolsa_exemplo(102,O_POS,450)) == SUCESSO);
    VERIFICAR(lista_inserir_lote(&lista, bolsa_exemplo(100,O_POS,450),3) == DUPLICADO);
    VERIFICAR(lista_consultar(&lista,100,&bolsa) == NAO_ENCONTRADO);
    VERIFICAR(lista_inserir_lote(&lista, bolsa_exemplo(200,O_POS,450),3) == SUCESSO);
    VERIFICAR(lista_consultar(&lista,202,&bolsa) == SUCESSO && strcmp(bolsa.codigo,"B-200-003") == 0);
    Bolsa invalida = bolsa_exemplo(300,O_POS,450);
    snprintf(invalida.data_coleta,sizeof invalida.data_coleta,"2026-02-30");
    VERIFICAR(lista_inserir(&lista,invalida) == INVALIDO);
#ifdef TESTAR_FALHAS
    falhar=1;
    VERIFICAR(lista_inserir_lote(&lista,bolsa_exemplo(400,O_POS,450),3) == SEM_MEMORIA);
    VERIFICAR(lista_consultar(&lista,400,&bolsa) == NAO_ENCONTRADO);
    falhar=0;
#endif
    RotaVital rota = {0};
    int ids_bolsas[] = {101,102};
    VERIFICAR(rota_atender(&rota, 1, ids_bolsas, 2, &operacao) == NAO_ENCONTRADO);
    VERIFICAR(arvore_inserir(&rota.hospitais, hospital(10)) == SUCESSO);
    VERIFICAR(rota_solicitar(&rota, (Requisicao){1,10,O_POS,2, CONCENTRADO_HEMACIAS, NORMAL}) == SUCESSO);
    VERIFICAR(rota_solicitar(&rota, (Requisicao){2,10,O_POS,1, CONCENTRADO_HEMACIAS, NORMAL}) == SUCESSO);
    VERIFICAR(lista_inserir(&rota.estoque, bolsa_exemplo(101,O_POS,450)) == SUCESSO);
    VERIFICAR(rota_atender(&rota, 1, ids_bolsas, 2, &operacao) == NAO_ENCONTRADO);
    VERIFICAR(lista_consultar(&rota.estoque, 101, &bolsa) == SUCESSO);
    VERIFICAR(rota.historico.topo == NULL && rota.requisicoes.inicio->dado.id == 1);
    VERIFICAR(lista_inserir(&rota.estoque, bolsa_exemplo(102,A_POS,450)) == SUCESSO);
    VERIFICAR(rota_atender(&rota, 1, ids_bolsas, 1, &operacao) == INVALIDO);
    int repetidos[] = {101,101};
    VERIFICAR(rota_atender(&rota, 1, repetidos, 2, &operacao) == INVALIDO);
    VERIFICAR(rota_atender(&rota, 2, ids_bolsas, 2, &operacao) == INVALIDO);
    VERIFICAR(rota_remover_hospital(&rota, 10, &h) == INVALIDO);
#ifdef TESTAR_FALHAS
    falhar = 1;
    VERIFICAR(rota_atender(&rota, 1, ids_bolsas, 2, &operacao) == SEM_MEMORIA);
    VERIFICAR(rota.historico.topo == NULL && rota.requisicoes.inicio->dado.id == 1);
    VERIFICAR(lista_consultar(&rota.estoque, 101, &bolsa) == SUCESSO);
    VERIFICAR(lista_consultar(&rota.estoque, 102, &bolsa) == SUCESSO);
    falhar = 0;
#endif
    /* Tipos diferentes são aceitos: nenhum algoritmo de compatibilidade existe. */
    VERIFICAR(rota_atender(&rota, 1, ids_bolsas, 2, &operacao) == SUCESSO);
    VERIFICAR(operacao.requisicao_id == 1 && operacao.quantidade == 2);
    VERIFICAR(rota.estoque.inicio == NULL && rota.requisicoes.inicio->dado.id == 2);
    VERIFICAR(rota_solicitar(&rota, (Requisicao){1,10,O_POS,2, CONCENTRADO_HEMACIAS, NORMAL}) == DUPLICADO);
    VERIFICAR(lista_inserir(&rota.estoque, bolsa_exemplo(103,AB_NEG,300)) == SUCESSO);
    int ultimo[] = {103};
    VERIFICAR(rota_atender(&rota, 2, ultimo, 1, &operacao) == SUCESSO);
    VERIFICAR(pilha_remover(&rota.historico, &operacao) == SUCESSO && operacao.requisicao_id == 2);
    VERIFICAR(pilha_remover(&rota.historico, &operacao) == SUCESSO && operacao.requisicao_id == 1);
    VERIFICAR(rota_remover_hospital(&rota, 10, &h) == SUCESSO);
    puts("FLUXO MANUAL: erros preservam estado; FIFO e LIFO; sem compatibilidade OK");
    lista_liberar(&lista); fila_liberar(&fila); pilha_liberar(&pilha); arvore_liberar(&arvore);
    rota_liberar(&rota); rota_liberar(&rota);
#ifdef TESTAR_FALHAS
    VERIFICAR(alocacoes_ativas == 0);
    puts("MEMORIA: zero alocacoes pendentes OK");
#endif
    printf("C: %d verificacoes aprovadas\n", verificacoes);
    return 0;
}
