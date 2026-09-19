#ifndef ROTA_VITAL_H
#define ROTA_VITAL_H
#include <stddef.h>
/* Inicialize as estruturas com {0}. Os ponteiros de estrutura e de saída
   devem ser válidos. As consultas copiam dados: nunca expõem nós internos. */
typedef enum { O_NEG, O_POS, A_NEG, A_POS, B_NEG, B_POS, AB_NEG, AB_POS } TipoSanguineo;
typedef enum { SUCESSO, NAO_ENCONTRADO, DUPLICADO, INVALIDO,
               SEM_MEMORIA, ESTOQUE_INSUFICIENTE } Resultado;
typedef enum { CONCENTRADO_HEMACIAS, PLAQUETAS, PLASMA } Hemocomponente;
typedef enum { NORMAL, URGENTE, EMERGENCIA } Urgencia;
typedef struct { int id; TipoSanguineo tipo; int volume_ml; char codigo[48];
    Hemocomponente hemocomponente; char data_coleta[11], data_validade[11]; } Bolsa;
typedef struct { int id; char nome[80]; char cidade[60]; } Hospital;
typedef struct { int id, hospital_id; TipoSanguineo tipo; int quantidade; Hemocomponente hemocomponente; Urgencia urgencia; } Requisicao;
/* Registra um atendimento; remover o registro NÃO desfaz a entrega. */
typedef struct { int requisicao_id, hospital_id, quantidade; TipoSanguineo tipo; } Operacao;
typedef struct NoBolsa { Bolsa dado; struct NoBolsa *proximo; } NoBolsa;
typedef struct { NoBolsa *inicio; } ListaEstoque;
typedef struct NoRequisicao { Requisicao dado; struct NoRequisicao *proximo; } NoRequisicao;
typedef struct { NoRequisicao *inicio, *fim; } FilaRequisicoes;
typedef struct NoOperacao { Operacao dado; struct NoOperacao *proximo; } NoOperacao;
typedef struct { NoOperacao *topo; } PilhaHistorico;
typedef struct NoHospital { Hospital dado; struct NoHospital *esquerda, *direita; } NoHospital;
typedef struct { NoHospital *raiz; } ArvoreHospitais;
typedef void (*VisitanteHospital)(Hospital hospital, void *contexto);
typedef enum { PRE_ORDEM, EM_ORDEM, POS_ORDEM } Ordem;
Resultado lista_inserir(ListaEstoque *lista, Bolsa bolsa);
Resultado lista_inserir_lote(ListaEstoque *lista, Bolsa modelo, int quantidade);
Resultado lista_consultar(const ListaEstoque *lista, int id, Bolsa *saida);
Resultado lista_remover(ListaEstoque *lista, int id, Bolsa *saida);
size_t lista_contar_tipo(const ListaEstoque *lista, TipoSanguineo tipo);
void lista_liberar(ListaEstoque *lista);
Resultado fila_inserir(FilaRequisicoes *fila, Requisicao requisicao);
Resultado fila_consultar(const FilaRequisicoes *fila, Requisicao *saida);
Resultado fila_buscar(const FilaRequisicoes *fila, int id, Requisicao *saida);
Resultado fila_remover(FilaRequisicoes *fila, Requisicao *saida);
void fila_liberar(FilaRequisicoes *fila);
Resultado pilha_inserir(PilhaHistorico *pilha, Operacao operacao);
Resultado pilha_consultar(const PilhaHistorico *pilha, Operacao *saida);
Resultado pilha_remover(PilhaHistorico *pilha, Operacao *saida);
void pilha_liberar(PilhaHistorico *pilha);
Resultado arvore_inserir(ArvoreHospitais *arvore, Hospital hospital);
Resultado arvore_consultar(const ArvoreHospitais *arvore, int id, Hospital *saida);
Resultado arvore_remover(ArvoreHospitais *arvore, int id, Hospital *saida);
void arvore_percorrer(const ArvoreHospitais *arvore, Ordem ordem,
                      VisitanteHospital visitar, void *contexto);
size_t arvore_contar(const ArvoreHospitais *arvore);
void arvore_liberar(ArvoreHospitais *arvore);
typedef struct {
    ListaEstoque estoque;
    FilaRequisicoes requisicoes;
    PilhaHistorico historico;
    ArvoreHospitais hospitais;
} RotaVital;
Resultado rota_solicitar(RotaVital *rota, Requisicao requisicao);
/* Seleção MANUAL de IDs: não compara tipo sanguíneo nem validade. */
Resultado rota_atender(RotaVital *rota, int requisicao_id, const int *ids, size_t quantidade, Operacao *saida);
Resultado rota_remover_hospital(RotaVital *rota, int id, Hospital *saida);
void rota_liberar(RotaVital *rota);
#endif
