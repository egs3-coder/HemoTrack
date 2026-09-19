#include "rota_vital.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <limits.h>
#include <ctype.h>
/* O teste pode substituir somente o alocador, sem mudar a API de produção. */
#ifdef TESTAR_FALHAS
extern void *alocar_teste(size_t tamanho);
extern void liberar_teste(void *ponteiro);
#define ALOCAR alocar_teste
#define LIBERAR liberar_teste
#else
#define ALOCAR malloc
#define LIBERAR free
#endif
static int tipo_valido(TipoSanguineo tipo) { return tipo >= O_NEG && tipo <= AB_POS; }

static int texto_valido(const char *texto, size_t capacidade) {
    if (!memchr(texto, '\0', capacidade)) return 0;
    for (size_t i = 0; texto[i]; i++) if (!isspace((unsigned char)texto[i])) return 1;
    return 0;
}
static int data_valida(const char *data) {
    if (!memchr(data, '\0', 11) || strlen(data) != 10 || data[4] != '-' || data[7] != '-') return 0;
    for (int i = 0; i < 10; i++) if (i != 4 && i != 7 && !isdigit((unsigned char)data[i])) return 0;
    int ano, mes, dia;
    if (sscanf(data, "%4d-%2d-%2d", &ano, &mes, &dia) != 3 || ano < 1 || mes < 1 || mes > 12) return 0;
    int dias[] = {31,28,31,30,31,30,31,31,30,31,30,31};
    if (ano % 400 == 0 || (ano % 4 == 0 && ano % 100 != 0)) dias[1] = 29;
    return dia >= 1 && dia <= dias[mes-1];
}
static int bolsa_valida(Bolsa bolsa) {
    return bolsa.id > 0 && bolsa.volume_ml > 0 && tipo_valido(bolsa.tipo)
        && bolsa.hemocomponente >= CONCENTRADO_HEMACIAS && bolsa.hemocomponente <= PLASMA
        && texto_valido(bolsa.codigo, sizeof bolsa.codigo)
        && data_valida(bolsa.data_coleta) && data_valida(bolsa.data_validade)
        && strcmp(bolsa.data_validade, bolsa.data_coleta) > 0;
}
Resultado lista_consultar(const ListaEstoque *lista, int id, Bolsa *saida) {
    for (const NoBolsa *atual = lista->inicio; atual; atual = atual->proximo) {
        if (atual->dado.id == id) { *saida = atual->dado; return SUCESSO; }
    }
    return NAO_ENCONTRADO;
}
Resultado lista_inserir(ListaEstoque *lista, Bolsa bolsa) {
    Bolsa existente;
    if (!bolsa_valida(bolsa)) return INVALIDO;
    if (lista_consultar(lista, bolsa.id, &existente) == SUCESSO) return DUPLICADO;
    for (NoBolsa *no = lista->inicio; no; no = no->proximo)
        if (strcmp(no->dado.codigo, bolsa.codigo) == 0) return DUPLICADO;
    NoBolsa *novo = ALOCAR(sizeof *novo);
    if (!novo) return SEM_MEMORIA;
    novo->dado = bolsa;
    novo->proximo = lista->inicio; /* Preserva o restante da lista. */
    lista->inicio = novo;
    return SUCESSO;
}
Resultado lista_inserir_lote(ListaEstoque *lista, Bolsa modelo, int quantidade) {
    if (!bolsa_valida(modelo) || quantidade < 1 || quantidade > 1000
        || modelo.id > INT_MAX - quantidade + 1) return INVALIDO;
    ListaEstoque temporaria = {0};
    Resultado resultado = SUCESSO;
    for (int i = 0; i < quantidade; i++) {
        Bolsa bolsa = modelo;
        bolsa.id += i;
        if (quantidade > 1) {
            int n = snprintf(bolsa.codigo, sizeof bolsa.codigo, "%s-%03d", modelo.codigo, i+1);
            if (n < 0 || n >= (int)sizeof bolsa.codigo) { resultado = INVALIDO; break; }
        }
        for (NoBolsa *no = lista->inicio; no; no = no->proximo)
            if (no->dado.id == bolsa.id || strcmp(no->dado.codigo, bolsa.codigo) == 0) { resultado = DUPLICADO; break; }
        if (resultado != SUCESSO) break;
        resultado = lista_inserir(&temporaria, bolsa);
        if (resultado != SUCESSO) break;
    }
    if (resultado != SUCESSO) { lista_liberar(&temporaria); return resultado; }
    NoBolsa *ultimo = temporaria.inicio;
    while (ultimo->proximo) ultimo = ultimo->proximo;
    ultimo->proximo = lista->inicio;
    lista->inicio = temporaria.inicio;
    return SUCESSO;
}
Resultado lista_remover(ListaEstoque *lista, int id, Bolsa *saida) {
    NoBolsa **ligacao = &lista->inicio;
    /* Aponta para o campo que leva ao nó atual: início ou próximo do anterior. */
    while (*ligacao && (*ligacao)->dado.id != id) ligacao = &(*ligacao)->proximo;
    if (!*ligacao) return NAO_ENCONTRADO;
    NoBolsa *removido = *ligacao;
    *saida = removido->dado;
    *ligacao = removido->proximo;
    LIBERAR(removido);
    return SUCESSO;
}
size_t lista_contar_tipo(const ListaEstoque *lista, TipoSanguineo tipo) {
    size_t quantidade = 0;
    for (const NoBolsa *atual = lista->inicio; atual; atual = atual->proximo)
        if (atual->dado.tipo == tipo) quantidade++;
    return quantidade;
}
void lista_liberar(ListaEstoque *lista) {
    while (lista->inicio) {
        NoBolsa *removido = lista->inicio;
        lista->inicio = removido->proximo;
        LIBERAR(removido);
    }
}
Resultado fila_buscar(const FilaRequisicoes *fila, int id, Requisicao *saida) {
    for (const NoRequisicao *atual = fila->inicio; atual; atual = atual->proximo)
        if (atual->dado.id == id) { *saida = atual->dado; return SUCESSO; }
    return NAO_ENCONTRADO;
}
Resultado fila_inserir(FilaRequisicoes *fila, Requisicao requisicao) {
    Requisicao existente;
    if (requisicao.id <= 0 || requisicao.hospital_id <= 0 || requisicao.quantidade <= 0
        || !tipo_valido(requisicao.tipo) || requisicao.hemocomponente < CONCENTRADO_HEMACIAS
        || requisicao.hemocomponente > PLASMA || requisicao.urgencia < NORMAL || requisicao.urgencia > EMERGENCIA) return INVALIDO;
    if (fila_buscar(fila, requisicao.id, &existente) == SUCESSO) return DUPLICADO;
    NoRequisicao *novo = ALOCAR(sizeof *novo);
    if (!novo) return SEM_MEMORIA;
    novo->dado = requisicao;
    novo->proximo = NULL;
    if (fila->fim) fila->fim->proximo = novo;
    else fila->inicio = novo;
    fila->fim = novo;
    return SUCESSO;
}
Resultado fila_consultar(const FilaRequisicoes *fila, Requisicao *saida) {
    if (!fila->inicio) return NAO_ENCONTRADO;
    *saida = fila->inicio->dado;
    return SUCESSO;
}
Resultado fila_remover(FilaRequisicoes *fila, Requisicao *saida) {
    if (!fila->inicio) return NAO_ENCONTRADO;
    NoRequisicao *removido = fila->inicio;
    *saida = removido->dado;
    fila->inicio = removido->proximo;
    if (!fila->inicio) fila->fim = NULL; /* Nunca deixar fim pendurado. */
    LIBERAR(removido);
    return SUCESSO;
}
void fila_liberar(FilaRequisicoes *fila) {
    Requisicao descartada;
    while (fila_remover(fila, &descartada) == SUCESSO) { }
}
Resultado pilha_inserir(PilhaHistorico *pilha, Operacao operacao) {
    if (operacao.requisicao_id <= 0 || operacao.hospital_id <= 0 || operacao.quantidade <= 0
        || !tipo_valido(operacao.tipo)) return INVALIDO;
    NoOperacao *novo = ALOCAR(sizeof *novo);
    if (!novo) return SEM_MEMORIA;
    novo->dado = operacao;
    novo->proximo = pilha->topo;
    pilha->topo = novo;
    return SUCESSO;
}
Resultado pilha_consultar(const PilhaHistorico *pilha, Operacao *saida) {
    if (!pilha->topo) return NAO_ENCONTRADO;
    *saida = pilha->topo->dado;
    return SUCESSO;
}
Resultado pilha_remover(PilhaHistorico *pilha, Operacao *saida) {
    if (!pilha->topo) return NAO_ENCONTRADO;
    NoOperacao *removido = pilha->topo;
    *saida = removido->dado;
    pilha->topo = removido->proximo;
    LIBERAR(removido);
    return SUCESSO;
}
void pilha_liberar(PilhaHistorico *pilha) {
    Operacao descartada;
    while (pilha_remover(pilha, &descartada) == SUCESSO) { }
}
static Resultado inserir_no(NoHospital **ligacao, Hospital hospital) {
    if (!*ligacao) {
        NoHospital *novo = ALOCAR(sizeof *novo);
        if (!novo) return SEM_MEMORIA;
        *novo = (NoHospital){hospital, NULL, NULL};
        *ligacao = novo;
        return SUCESSO;
    }
    if (hospital.id == (*ligacao)->dado.id) return DUPLICADO;
    if (hospital.id < (*ligacao)->dado.id) return inserir_no(&(*ligacao)->esquerda, hospital);
    return inserir_no(&(*ligacao)->direita, hospital);
}
Resultado arvore_inserir(ArvoreHospitais *arvore, Hospital hospital) {
    if (hospital.id <= 0 || !texto_valido(hospital.nome, sizeof hospital.nome)
        || !texto_valido(hospital.cidade, sizeof hospital.cidade)) return INVALIDO;
    return inserir_no(&arvore->raiz, hospital);
}
static Resultado consultar_no(const NoHospital *no, int id, Hospital *saida) {
    if (!no) return NAO_ENCONTRADO; /* Caso base da recursão. */
    if (id == no->dado.id) { *saida = no->dado; return SUCESSO; }
    return consultar_no(id < no->dado.id ? no->esquerda : no->direita, id, saida);
}
Resultado arvore_consultar(const ArvoreHospitais *arvore, int id, Hospital *saida) {
    return consultar_no(arvore->raiz, id, saida);
}
static Resultado remover_no(NoHospital **ligacao, int id, Hospital *saida) {
    NoHospital *no = *ligacao;
    if (!no) return NAO_ENCONTRADO;
    if (id < no->dado.id) return remover_no(&no->esquerda, id, saida);
    if (id > no->dado.id) return remover_no(&no->direita, id, saida);
    *saida = no->dado;
    if (!no->esquerda || !no->direita) {
        *ligacao = no->esquerda ? no->esquerda : no->direita;
        LIBERAR(no); /* Inclui folha: o filho escolhido será NULL. */
    } else {
        NoHospital *sucessor = no->direita;
        while (sucessor->esquerda) sucessor = sucessor->esquerda;
        no->dado = sucessor->dado; /* Copia o hospital inteiro, inclusive o nome. */
        Hospital descartado;
        remover_no(&no->direita, sucessor->dado.id, &descartado);
    }
    return SUCESSO;
}
Resultado arvore_remover(ArvoreHospitais *arvore, int id, Hospital *saida) {
    return remover_no(&arvore->raiz, id, saida);
}
static void percorrer_no(const NoHospital *no, Ordem ordem, VisitanteHospital visitar, void *contexto) {
    if (!no) return;
    if (ordem == PRE_ORDEM) visitar(no->dado, contexto);
    percorrer_no(no->esquerda, ordem, visitar, contexto);
    if (ordem == EM_ORDEM) visitar(no->dado, contexto);
    percorrer_no(no->direita, ordem, visitar, contexto);
    if (ordem == POS_ORDEM) visitar(no->dado, contexto);
}
void arvore_percorrer(const ArvoreHospitais *arvore, Ordem ordem, VisitanteHospital visitar, void *contexto) {
    if (visitar) percorrer_no(arvore->raiz, ordem, visitar, contexto);
}
static size_t contar_no(const NoHospital *no) {
    return no ? 1 + contar_no(no->esquerda) + contar_no(no->direita) : 0;
}
size_t arvore_contar(const ArvoreHospitais *arvore) { return contar_no(arvore->raiz); }
static void liberar_no(NoHospital *no) {
    if (!no) return;
    liberar_no(no->esquerda);
    liberar_no(no->direita);
    LIBERAR(no); /* Pós-ordem: libera filhos antes do pai. */
}
void arvore_liberar(ArvoreHospitais *arvore) {
    liberar_no(arvore->raiz);
    arvore->raiz = NULL;
}
Resultado rota_solicitar(RotaVital *rota, Requisicao requisicao) {
    Hospital hospital;
    if (arvore_consultar(&rota->hospitais, requisicao.hospital_id, &hospital) != SUCESSO)
        return NAO_ENCONTRADO;
    for (NoOperacao *no = rota->historico.topo; no; no = no->proximo)
        if (no->dado.requisicao_id == requisicao.id) return DUPLICADO;
    return fila_inserir(&rota->requisicoes, requisicao);
}
Resultado rota_atender(RotaVital *rota, int requisicao_id, const int *ids, size_t quantidade, Operacao *saida) {
    Requisicao requisicao;
    Resultado resultado = fila_consultar(&rota->requisicoes, &requisicao);
    if (resultado != SUCESSO) return resultado;
    if (requisicao.id != requisicao_id) return INVALIDO;
    if (!ids || quantidade != (size_t)requisicao.quantidade) return INVALIDO;
    Bolsa bolsa;
    for (size_t i = 0; i < quantidade; i++) {
        for (size_t j = 0; j < i; j++) if (ids[i] == ids[j]) return INVALIDO;
        if (lista_consultar(&rota->estoque, ids[i], &bolsa) != SUCESSO) return NAO_ENCONTRADO;
    }
    Operacao operacao = {requisicao.id, requisicao.hospital_id, requisicao.quantidade, requisicao.tipo};
    /* Só altera o estoque depois de validar todos os IDs e reservar o histórico. */
    resultado = pilha_inserir(&rota->historico, operacao);
    if (resultado != SUCESSO) return resultado;
    for (size_t i = 0; i < quantidade; i++) lista_remover(&rota->estoque, ids[i], &bolsa);
    fila_remover(&rota->requisicoes, &requisicao);
    *saida = operacao;
    return SUCESSO;
}
Resultado rota_remover_hospital(RotaVital *rota, int id, Hospital *saida) {
    for (NoRequisicao *no = rota->requisicoes.inicio; no; no = no->proximo)
        if (no->dado.hospital_id == id) return INVALIDO;
    for (NoOperacao *no = rota->historico.topo; no; no = no->proximo)
        if (no->dado.hospital_id == id) return INVALIDO;
    return arvore_remover(&rota->hospitais, id, saida);
}
void rota_liberar(RotaVital *rota) {
    lista_liberar(&rota->estoque);
    fila_liberar(&rota->requisicoes);
    pilha_liberar(&rota->historico);
    arvore_liberar(&rota->hospitais);
}
