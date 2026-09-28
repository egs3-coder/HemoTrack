/*
 * ============================================================================
 *  HemoTrack / Rota Vital - Projeto Integrador - Unidade 1
 * ============================================================================
 *  Estruturas de domínio implementadas manualmente (sem STL de containers):
 *
 *    - ESTOQUE de bolsas ........ Lista simplesmente encadeada (inicio/fim)
 *    - REQUISIÇÕES hospitalares . Fila (FIFO) - enqueue no fim, dequeue no início
 *    - HISTÓRICO de operações ... Pilha (LIFO) - permite "desfazer" a última ação
 *
 *  Toda a memória dos nós é alocada com malloc() e liberada com free().
 *  Por isso os textos são vetores de char (e não std::string): malloc apenas
 *  reserva bytes e não chama construtores, o que tornaria std::string inválida.
 *
 *  Fora do escopo desta entrega (proibido pelo professor):
 *    FEFO, compatibilidade ABO/Rh, hash e roteirização.
 *    O tipo sanguíneo é armazenado apenas como DADO INFORMATIVO; a escolha da
 *    bolsa no atendimento é feita MANUALMENTE pelo operador.
 *
 *  Histórias de usuário cobertas:
 *    US01 - Registrar requisição hospitalar ............ registrarRequisicao()
 *    US02 - Cadastrar bolsa no estoque ................. cadastrarBolsa()
 *    US03 - Atender requisições por ordem de chegada ... atenderProximaRequisicao()
 *    US04 - Consultar estoque e buscar bolsa ........... listarEstoque() / consultarBolsa()
 *    US05 - Descartar bolsa do estoque ................. descartarBolsa()
 *    US06 - Histórico de operações e desfazer .......... exibirHistorico() / desfazerUltimaOperacao()
 *    US07 - Relatório consolidado (contagens recursivas) gerarRelatorio()
 *
 *  Compilar:  g++ -std=c++17 -Wall -Wextra -o hemotrack hemotrack.cpp
 *  Executar:  ./hemotrack
 * ============================================================================
 */

#include <iostream>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cctype>
#include <ctime>
#include <climits>

using namespace std;

#define TAM_COD 16
#define TAM_TXT 48
#define TAM_TIPO 8
#define TAM_DESC 128
#define MAX_BOLSAS_REQ 10

#define ST_DISPONIVEL "Disponível"
#define ST_RESERVADA  "Reservada para Despacho"
#define ST_PENDENTE   "Pendente de Alocação"
#define ST_ATENDIDA   "Atendida"

const char* HEMOCOMPONENTES[] = {"Concentrado de Hemácias", "Plaquetas",
                                 "Plasma Fresco Congelado", "Crioprecipitado"};
const int QTD_HEMOCOMPONENTES = 4;

const char* TIPOS_SANGUINEOS[] = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
const int QTD_TIPOS = 8;

const char* URGENCIAS[] = {"Emergência", "Urgência", "Eletiva"};
const int QTD_URGENCIAS = 3;

const char* MOTIVOS_DESCARTE[] = {"Validade expirada", "Violação da embalagem",
                                  "Falha de armazenamento", "Outro"};
const int QTD_MOTIVOS = 4;

bool fimEntrada = false; // vira true se a entrada padrão terminar (EOF)

/* ============================================================================
 *  DEFINIÇÃO DAS ESTRUTURAS
 * ========================================================================== */

// Nó da LISTA de estoque: cada nó é uma bolsa de sangue.
typedef struct Bolsa {
    char codigo[TAM_COD];
    char hemocomponente[TAM_TXT];
    char tipoSanguineo[TAM_TIPO];   // apenas informativo
    int dataColeta;                 // formato aaaammdd
    int dataValidade;               // formato aaaammdd
    char status[TAM_TXT];
    struct Bolsa* prox;             // ponteiro para a próxima bolsa
} Bolsa;

typedef struct ListaEstoque {
    Bolsa* inicio;
    Bolsa* fim;                     // permite inserir no final em O(1)
    int tamanho;
} ListaEstoque;

// Nó da FILA de requisições.
typedef struct Requisicao {
    int id;
    char hospital[TAM_TXT];
    char hemocomponente[TAM_TXT];
    char tipoSanguineo[TAM_TIPO];
    int quantidade;
    char urgencia[TAM_TXT];
    char status[TAM_TXT];
    char bolsasAlocadas[MAX_BOLSAS_REQ][TAM_COD];
    int qtdAlocada;
    struct Requisicao* prox;
} Requisicao;

typedef struct FilaRequisicoes {
    Requisicao* inicio;             // de onde sai (dequeue)
    Requisicao* fim;                // onde entra (enqueue)
    int tamanho;
    int proximoId;
} FilaRequisicoes;

// Nó da PILHA de histórico: guarda uma CÓPIA dos dados para poder desfazer.
typedef enum {
    OP_CADASTRO_BOLSA,
    OP_DESCARTE_BOLSA,
    OP_NOVA_REQUISICAO,
    OP_ATENDIMENTO
} TipoOperacao;

typedef struct Operacao {
    TipoOperacao tipo;
    char descricao[TAM_DESC];
    Bolsa bolsa;                    // cópia da bolsa (cadastro/descarte)
    Requisicao requisicao;          // cópia da requisição (registro/atendimento)
    struct Operacao* prox;          // aponta para a operação abaixo na pilha
} Operacao;

typedef struct PilhaHistorico {
    Operacao* topo;
    int tamanho;
} PilhaHistorico;

/* ============================================================================
 *  FUNÇÕES AUXILIARES (texto, entrada e datas)
 * ========================================================================== */

void copiar(char* destino, const char* origem, int tamanho) {
    strncpy(destino, origem, tamanho - 1);
    destino[tamanho - 1] = '\0';
}

void removerEspacosBorda(char* s) {
    int ini = 0;
    while (s[ini] == ' ' || s[ini] == '\t') ini++;
    if (ini > 0) memmove(s, s + ini, strlen(s + ini) + 1);
    int fim = (int)strlen(s) - 1;
    while (fim >= 0 && (s[fim] == ' ' || s[fim] == '\t' || s[fim] == '\r')) {
        s[fim] = '\0';
        fim--;
    }
}

void lerLinha(const char* prompt, char* buffer, int tamanho) {
    cout << prompt;
    cout.flush();
    buffer[0] = '\0';
    if (!cin.getline(buffer, tamanho)) {
        if (cin.eof()) {
            fimEntrada = true;
            buffer[0] = '\0';
            return;
        }
        // linha maior que o buffer: mantém o que coube e descarta o resto
        cin.clear();
        cin.ignore(INT_MAX, '\n');
    }
    removerEspacosBorda(buffer);
}

int lerInteiro(const char* prompt, int minimo, int maximo) {
    char buffer[32];
    while (true) {
        lerLinha(prompt, buffer, sizeof(buffer));
        if (fimEntrada) return INT_MIN;
        char* resto;
        long valor = strtol(buffer, &resto, 10);
        if (resto != buffer && *resto == '\0' && valor >= minimo && valor <= maximo)
            return (int)valor;
        cout << "  Valor inválido. Digite um número entre " << minimo
             << " e " << maximo << ".\n";
    }
}

int dataParaInt(int dia, int mes, int ano) { return ano * 10000 + mes * 100 + dia; }

int dataHoje() {
    time_t agora = time(NULL);
    struct tm* local = localtime(&agora);
    return dataParaInt(local->tm_mday, local->tm_mon + 1, local->tm_year + 1900);
}

bool dataExiste(int dia, int mes, int ano) {
    if (ano < 1900 || mes < 1 || mes > 12 || dia < 1) return false;
    int dias[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    bool bissexto = (ano % 4 == 0 && ano % 100 != 0) || ano % 400 == 0;
    if (bissexto) dias[1] = 29;
    return dia <= dias[mes - 1];
}

// Lê uma data no formato dd/mm/aaaa. Retorna aaaammdd ou -1 se inválida.
int lerData(const char* prompt) {
    char buffer[32];
    lerLinha(prompt, buffer, sizeof(buffer));
    if (fimEntrada) return -1;
    int d, m, a;
    char extra;
    if (sscanf(buffer, "%d/%d/%d%c", &d, &m, &a, &extra) != 3) return -1;
    if (!dataExiste(d, m, a)) return -1;
    return dataParaInt(d, m, a);
}

void formatarData(int data, char* saida) {
    snprintf(saida, 16, "%02d/%02d/%04d", data % 100, (data / 100) % 100, data / 10000);
}

void formatarIdRequisicao(int id, char* saida) {
    snprintf(saida, 16, "REQ-%04d", id);
}

// Valida apenas o FORMATO do tipo sanguíneo (não há regra de compatibilidade).
bool tipoSanguineoValido(char* tipo) {
    for (int i = 0; tipo[i] != '\0'; i++)
        tipo[i] = (char)toupper((unsigned char)tipo[i]);
    for (int i = 0; i < QTD_TIPOS; i++)
        if (strcmp(tipo, TIPOS_SANGUINEOS[i]) == 0) return true;
    return false;
}

const char* escolherOpcao(const char* titulo, const char* opcoes[], int quantidade) {
    cout << titulo << "\n";
    for (int i = 0; i < quantidade; i++)
        cout << "  " << (i + 1) << " - " << opcoes[i] << "\n";
    int escolha = lerInteiro("  Opção: ", 1, quantidade);
    if (fimEntrada) return NULL;
    return opcoes[escolha - 1];
}

/* ============================================================================
 *  LISTA ENCADEADA - ESTOQUE DE BOLSAS
 * ========================================================================== */

void inicializarEstoque(ListaEstoque* lista) {
    lista->inicio = NULL;
    lista->fim = NULL;
    lista->tamanho = 0;
}

Bolsa* criarBolsa(const char* codigo, const char* hemocomponente, const char* tipo,
                  int dataColeta, int dataValidade, const char* status) {
    Bolsa* nova = (Bolsa*)malloc(sizeof(Bolsa));
    if (nova == NULL) {
        cout << "  Erro: memória insuficiente para criar a bolsa.\n";
        return NULL;
    }
    copiar(nova->codigo, codigo, TAM_COD);
    copiar(nova->hemocomponente, hemocomponente, TAM_TXT);
    copiar(nova->tipoSanguineo, tipo, TAM_TIPO);
    nova->dataColeta = dataColeta;
    nova->dataValidade = dataValidade;
    copiar(nova->status, status, TAM_TXT);
    nova->prox = NULL;
    return nova;
}

void inserirFimEstoque(ListaEstoque* lista, Bolsa* nova) {
    nova->prox = NULL;
    if (lista->inicio == NULL) {       // lista vazia: nó é início e fim
        lista->inicio = nova;
        lista->fim = nova;
    } else {                           // encadeia após o último
        lista->fim->prox = nova;
        lista->fim = nova;
    }
    lista->tamanho++;
}

// Busca RECURSIVA: caso base = fim da lista ou código encontrado.
Bolsa* buscarBolsaRec(Bolsa* no, const char* codigo) {
    if (no == NULL) return NULL;
    if (strcmp(no->codigo, codigo) == 0) return no;
    return buscarBolsaRec(no->prox, codigo);
}

// Remove a bolsa pelo código. Se "copia" não for NULL, salva os dados antes do free.
bool removerBolsa(ListaEstoque* lista, const char* codigo, Bolsa* copia) {
    Bolsa* anterior = NULL;
    Bolsa* atual = lista->inicio;
    while (atual != NULL && strcmp(atual->codigo, codigo) != 0) {
        anterior = atual;
        atual = atual->prox;
    }
    if (atual == NULL) return false;

    if (anterior == NULL) lista->inicio = atual->prox;   // era o primeiro
    else anterior->prox = atual->prox;                   // "pula" o nó removido
    if (atual == lista->fim) lista->fim = anterior;      // era o último

    if (copia != NULL) {
        *copia = *atual;
        copia->prox = NULL;
    }
    free(atual);
    lista->tamanho--;
    return true;
}

// Contagem RECURSIVA por status.
int contarPorStatusRec(Bolsa* no, const char* status) {
    if (no == NULL) return 0;
    int atual = (strcmp(no->status, status) == 0) ? 1 : 0;
    return atual + contarPorStatusRec(no->prox, status);
}

// Contagem RECURSIVA de bolsas disponíveis de um hemocomponente.
int contarDisponiveisPorHemoRec(Bolsa* no, const char* hemocomponente) {
    if (no == NULL) return 0;
    int atual = (strcmp(no->hemocomponente, hemocomponente) == 0 &&
                 strcmp(no->status, ST_DISPONIVEL) == 0) ? 1 : 0;
    return atual + contarDisponiveisPorHemoRec(no->prox, hemocomponente);
}

void imprimirBolsa(Bolsa* b) {
    char coleta[16], validade[16];
    formatarData(b->dataColeta, coleta);
    formatarData(b->dataValidade, validade);
    cout << "  [" << b->codigo << "] " << b->hemocomponente << " | " << b->tipoSanguineo
         << " | coleta " << coleta << " | validade " << validade
         << " | " << b->status << "\n";
}

void imprimirBolsasComStatus(ListaEstoque* lista, const char* status) {
    for (Bolsa* atual = lista->inicio; atual != NULL; atual = atual->prox)
        if (status == NULL || strcmp(atual->status, status) == 0)
            imprimirBolsa(atual);
}

void liberarEstoque(ListaEstoque* lista) {
    Bolsa* atual = lista->inicio;
    while (atual != NULL) {
        Bolsa* proximo = atual->prox;  // guarda antes de liberar
        free(atual);
        atual = proximo;
    }
    inicializarEstoque(lista);
}

/* ============================================================================
 *  FILA - REQUISIÇÕES HOSPITALARES (FIFO)
 * ========================================================================== */

void inicializarFila(FilaRequisicoes* fila) {
    fila->inicio = NULL;
    fila->fim = NULL;
    fila->tamanho = 0;
    fila->proximoId = 1;
}

void enqueue(FilaRequisicoes* fila, Requisicao* nova) {
    nova->prox = NULL;
    if (fila->fim == NULL) {
        fila->inicio = nova;
        fila->fim = nova;
    } else {
        fila->fim->prox = nova;
        fila->fim = nova;
    }
    fila->tamanho++;
}

// Retira o primeiro da fila. Quem chama é responsável pelo free().
Requisicao* dequeue(FilaRequisicoes* fila) {
    if (fila->inicio == NULL) return NULL;
    Requisicao* removida = fila->inicio;
    fila->inicio = removida->prox;
    if (fila->inicio == NULL) fila->fim = NULL;  // fila ficou vazia
    removida->prox = NULL;
    fila->tamanho--;
    return removida;
}

Requisicao* peekFila(FilaRequisicoes* fila) { return fila->inicio; }

// Usada apenas pelo "desfazer atendimento": devolve a requisição à frente da fila,
// exatamente a posição de onde ela saiu.
void devolverAoInicioFila(FilaRequisicoes* fila, Requisicao* req) {
    req->prox = fila->inicio;
    fila->inicio = req;
    if (fila->fim == NULL) fila->fim = req;
    fila->tamanho++;
}

// Usada apenas pelo "desfazer registro de requisição".
bool removerRequisicaoPorId(FilaRequisicoes* fila, int id) {
    Requisicao* anterior = NULL;
    Requisicao* atual = fila->inicio;
    while (atual != NULL && atual->id != id) {
        anterior = atual;
        atual = atual->prox;
    }
    if (atual == NULL) return false;
    if (anterior == NULL) fila->inicio = atual->prox;
    else anterior->prox = atual->prox;
    if (atual == fila->fim) fila->fim = anterior;
    free(atual);
    fila->tamanho--;
    return true;
}

void imprimirRequisicao(Requisicao* r) {
    char id[16];
    formatarIdRequisicao(r->id, id);
    cout << "  " << id << " | " << r->hospital << " | " << r->hemocomponente
         << " | " << r->tipoSanguineo << " | " << r->quantidade << " bolsa(s) | "
         << r->urgencia << " | " << r->status << "\n";
}

void liberarFila(FilaRequisicoes* fila) {
    Requisicao* atual = fila->inicio;
    while (atual != NULL) {
        Requisicao* proximo = atual->prox;
        free(atual);
        atual = proximo;
    }
    fila->inicio = NULL;
    fila->fim = NULL;
    fila->tamanho = 0;
}

/* ============================================================================
 *  PILHA - HISTÓRICO DE OPERAÇÕES (LIFO)
 * ========================================================================== */

void inicializarHistorico(PilhaHistorico* pilha) {
    pilha->topo = NULL;
    pilha->tamanho = 0;
}

Operacao* criarOperacao(TipoOperacao tipo, const char* descricao) {
    Operacao* op = (Operacao*)malloc(sizeof(Operacao));
    if (op == NULL) {
        cout << "  Erro: memória insuficiente para registrar o histórico.\n";
        return NULL;
    }
    memset(op, 0, sizeof(Operacao));
    op->tipo = tipo;
    copiar(op->descricao, descricao, TAM_DESC);
    op->prox = NULL;
    return op;
}

void push(PilhaHistorico* pilha, Operacao* op) {
    op->prox = pilha->topo;   // novo nó aponta para o antigo topo
    pilha->topo = op;         // e passa a ser o topo
    pilha->tamanho++;
}

Operacao* pop(PilhaHistorico* pilha) {
    if (pilha->topo == NULL) return NULL;
    Operacao* removida = pilha->topo;
    pilha->topo = removida->prox;
    removida->prox = NULL;
    pilha->tamanho--;
    return removida;          // quem chama faz o free()
}

// Contagem RECURSIVA de operações de um tipo, percorrendo do topo à base.
int contarOperacoesRec(Operacao* no, TipoOperacao tipo) {
    if (no == NULL) return 0;
    return (no->tipo == tipo ? 1 : 0) + contarOperacoesRec(no->prox, tipo);
}

void liberarHistorico(PilhaHistorico* pilha) {
    Operacao* op;
    while ((op = pop(pilha)) != NULL) free(op);
}

/* ============================================================================
 *  CASOS DE USO (HISTÓRIAS DE USUÁRIO)
 * ========================================================================== */

// US02 - Cadastro e entrada de bolsas no estoque
void cadastrarBolsa(ListaEstoque* estoque, PilhaHistorico* historico) {
    char codigo[TAM_COD], tipo[TAM_TIPO];
    cout << "\n--- Cadastro de bolsa no estoque ---\n";

    lerLinha("Código da bolsa (ex: BOL-9821): ", codigo, TAM_COD);
    if (fimEntrada) return;
    if (codigo[0] == '\0') {
        cout << "  Cadastro bloqueado: o código da bolsa é obrigatório.\n";
        return;
    }
    if (buscarBolsaRec(estoque->inicio, codigo) != NULL) {
        cout << "  Cadastro bloqueado: já existe uma bolsa com o código " << codigo << ".\n";
        return;
    }

    const char* hemo = escolherOpcao("Hemocomponente:", HEMOCOMPONENTES, QTD_HEMOCOMPONENTES);
    if (hemo == NULL) return;

    lerLinha("Tipo ABO/Rh (A+, A-, B+, B-, AB+, AB-, O+, O-): ", tipo, TAM_TIPO);
    if (fimEntrada) return;
    if (!tipoSanguineoValido(tipo)) {
        cout << "  Cadastro bloqueado: tipo sanguíneo inválido.\n";
        return;
    }

    int hoje = dataHoje();
    int coleta = lerData("Data de coleta (dd/mm/aaaa): ");
    if (fimEntrada) return;
    if (coleta == -1 || coleta > hoje) {
        cout << "  Cadastro bloqueado: data de coleta inválida.\n";
        return;
    }

    int validade = lerData("Data de validade (dd/mm/aaaa): ");
    if (fimEntrada) return;
    if (validade == -1 || validade < hoje || validade < coleta) {
        cout << "  Data de validade inválida. Não é permitido cadastrar bolsas vencidas.\n";
        return;
    }

    Bolsa* nova = criarBolsa(codigo, hemo, tipo, coleta, validade, ST_DISPONIVEL);
    if (nova == NULL) return;

    char desc[TAM_DESC];
    snprintf(desc, TAM_DESC, "Cadastro da bolsa %s (%s %s)", codigo, hemo, tipo);
    Operacao* op = criarOperacao(OP_CADASTRO_BOLSA, desc);
    if (op == NULL) {
        free(nova);
        return;
    }

    inserirFimEstoque(estoque, nova);
    op->bolsa = *nova;
    op->bolsa.prox = NULL;
    push(historico, op);

    cout << "  Bolsa " << codigo << " adicionada ao estoque com status \"" << ST_DISPONIVEL << "\".\n";
    cout << "  Saldo disponível de " << hemo << ": "
         << contarDisponiveisPorHemoRec(estoque->inicio, hemo) << " bolsa(s).\n";
}

// US04 - Consulta do estoque
void listarEstoque(ListaEstoque* estoque) {
    cout << "\n--- Estoque (" << estoque->tamanho << " bolsa(s)) ---\n";
    if (estoque->inicio == NULL) {
        cout << "  Estoque vazio.\n";
        return;
    }
    imprimirBolsasComStatus(estoque, NULL);
}

void consultarBolsa(ListaEstoque* estoque) {
    char codigo[TAM_COD];
    lerLinha("\nCódigo da bolsa a consultar: ", codigo, TAM_COD);
    if (fimEntrada) return;
    Bolsa* b = buscarBolsaRec(estoque->inicio, codigo);
    if (b == NULL) cout << "  Bolsa " << codigo << " não encontrada no estoque.\n";
    else imprimirBolsa(b);
}

// US05 - Descarte de bolsa
void descartarBolsa(ListaEstoque* estoque, PilhaHistorico* historico) {
    cout << "\n--- Descarte de bolsa ---\n";
    if (estoque->inicio == NULL) {
        cout << "  Estoque vazio.\n";
        return;
    }
    char codigo[TAM_COD];
    lerLinha("Código da bolsa a descartar: ", codigo, TAM_COD);
    if (fimEntrada) return;

    Bolsa* b = buscarBolsaRec(estoque->inicio, codigo);
    if (b == NULL) {
        cout << "  Bolsa " << codigo << " não encontrada no estoque.\n";
        return;
    }
    if (strcmp(b->status, ST_DISPONIVEL) != 0) {
        cout << "  Descarte bloqueado: somente bolsas com status \"" << ST_DISPONIVEL
             << "\" podem ser descartadas.\n";
        return;
    }

    const char* motivo = escolherOpcao("Motivo do descarte:", MOTIVOS_DESCARTE, QTD_MOTIVOS);
    if (motivo == NULL) return;

    char desc[TAM_DESC];
    snprintf(desc, TAM_DESC, "Descarte da bolsa %s (motivo: %s)", codigo, motivo);
    Operacao* op = criarOperacao(OP_DESCARTE_BOLSA, desc);
    if (op == NULL) return;

    removerBolsa(estoque, codigo, &op->bolsa);  // cópia vai para o histórico, nó é liberado
    push(historico, op);
    cout << "  Bolsa " << codigo << " removida do estoque. Motivo: " << motivo << ".\n";
}

// US01 - Requisição de hemocomponentes
void registrarRequisicao(FilaRequisicoes* fila, PilhaHistorico* historico) {
    char hospital[TAM_TXT], tipo[TAM_TIPO];
    cout << "\n--- Nova requisição hospitalar ---\n";

    lerLinha("Hospital de destino: ", hospital, TAM_TXT);
    if (fimEntrada) return;
    if (hospital[0] == '\0') {
        cout << "  Requisição NÃO registrada: o hospital de destino é obrigatório.\n";
        return;
    }

    const char* hemo = escolherOpcao("Hemocomponente:", HEMOCOMPONENTES, QTD_HEMOCOMPONENTES);
    if (hemo == NULL) return;

    lerLinha("Tipo sanguíneo do paciente: ", tipo, TAM_TIPO);
    if (fimEntrada) return;
    if (tipo[0] == '\0') {
        cout << "  Requisição NÃO registrada: o tipo sanguíneo do paciente é obrigatório.\n";
        return;
    }
    if (!tipoSanguineoValido(tipo)) {
        cout << "  Requisição NÃO registrada: tipo sanguíneo inválido.\n";
        return;
    }

    int quantidade = lerInteiro("Quantidade de bolsas (1 a 10): ", 1, MAX_BOLSAS_REQ);
    if (fimEntrada) return;

    const char* urgencia = escolherOpcao("Nível de urgência:", URGENCIAS, QTD_URGENCIAS);
    if (urgencia == NULL) return;

    Requisicao* nova = (Requisicao*)malloc(sizeof(Requisicao));
    if (nova == NULL) {
        cout << "  Erro: memória insuficiente para registrar a requisição.\n";
        return;
    }
    memset(nova, 0, sizeof(Requisicao));
    nova->id = fila->proximoId;
    copiar(nova->hospital, hospital, TAM_TXT);
    copiar(nova->hemocomponente, hemo, TAM_TXT);
    copiar(nova->tipoSanguineo, tipo, TAM_TIPO);
    nova->quantidade = quantidade;
    copiar(nova->urgencia, urgencia, TAM_TXT);
    copiar(nova->status, ST_PENDENTE, TAM_TXT);
    nova->qtdAlocada = 0;
    nova->prox = NULL;

    char id[16], desc[TAM_DESC];
    formatarIdRequisicao(nova->id, id);
    snprintf(desc, TAM_DESC, "Registro da requisição %s (%s)", id, hospital);
    Operacao* op = criarOperacao(OP_NOVA_REQUISICAO, desc);
    if (op == NULL) {
        free(nova);
        return;
    }

    enqueue(fila, nova);
    fila->proximoId++;
    op->requisicao = *nova;
    op->requisicao.prox = NULL;
    push(historico, op);

    cout << "  Requisição cadastrada com status \"" << ST_PENDENTE << "\".\n";
    cout << "  Identificador de rastreamento: " << id << "\n";
    cout << "  Posição na fila: " << fila->tamanho << "\n";
}

void exibirFila(FilaRequisicoes* fila) {
    cout << "\n--- Fila de requisições (" << fila->tamanho << " pendente(s)) ---\n";
    if (fila->inicio == NULL) {
        cout << "  Nenhuma requisição pendente.\n";
        return;
    }
    for (Requisicao* r = fila->inicio; r != NULL; r = r->prox) imprimirRequisicao(r);

    cout << "  Ordem: ";
    for (Requisicao* r = fila->inicio; r != NULL; r = r->prox) {
        char id[16];
        formatarIdRequisicao(r->id, id);
        cout << id << " <- ";
    }
    cout << "NULL\n";
}

bool codigoJaEscolhido(char escolhidas[][TAM_COD], int quantidade, const char* codigo) {
    for (int i = 0; i < quantidade; i++)
        if (strcmp(escolhidas[i], codigo) == 0) return true;
    return false;
}

// US03 - Atendimento por ordem de chegada com seleção MANUAL das bolsas
void atenderProximaRequisicao(FilaRequisicoes* fila, ListaEstoque* estoque,
                              PilhaHistorico* historico) {
    cout << "\n--- Atendimento da próxima requisição ---\n";
    Requisicao* req = peekFila(fila);
    if (req == NULL) {
        cout << "  Não há requisições pendentes.\n";
        return;
    }
    cout << "Próxima da fila:\n";
    imprimirRequisicao(req);

    // Compara apenas o PRODUTO pedido (hemocomponente), sem regra ABO/Rh.
    int disponiveis = contarDisponiveisPorHemoRec(estoque->inicio, req->hemocomponente);
    if (disponiveis < req->quantidade) {
        cout << "  Estoque insuficiente: há " << disponiveis << " bolsa(s) de "
             << req->hemocomponente << " disponível(is). A requisição permanece na fila.\n";
        return;
    }

    cout << "Bolsas disponíveis de " << req->hemocomponente << ":\n";
    for (Bolsa* b = estoque->inicio; b != NULL; b = b->prox)
        if (strcmp(b->status, ST_DISPONIVEL) == 0 &&
            strcmp(b->hemocomponente, req->hemocomponente) == 0)
            imprimirBolsa(b);
    cout << "Informe o código de cada bolsa a separar (em branco cancela):\n";

    char escolhidas[MAX_BOLSAS_REQ][TAM_COD];
    int n = 0;
    while (n < req->quantidade) {
        char prompt[48], codigo[TAM_COD];
        snprintf(prompt, sizeof(prompt), "  Bolsa %d de %d: ", n + 1, req->quantidade);
        lerLinha(prompt, codigo, TAM_COD);
        if (fimEntrada) return;
        if (codigo[0] == '\0') {
            cout << "  Atendimento cancelado. Nenhuma alteração foi feita.\n";
            return;
        }
        Bolsa* b = buscarBolsaRec(estoque->inicio, codigo);
        if (b == NULL) {
            cout << "  Bolsa não encontrada.\n";
            continue;
        }
        if (strcmp(b->status, ST_DISPONIVEL) != 0) {
            cout << "  Essa bolsa não está disponível.\n";
            continue;
        }
        if (strcmp(b->hemocomponente, req->hemocomponente) != 0) {
            cout << "  Essa bolsa não é do hemocomponente solicitado.\n";
            continue;
        }
        if (codigoJaEscolhido(escolhidas, n, codigo)) {
            cout << "  Essa bolsa já foi selecionada neste atendimento.\n";
            continue;
        }
        copiar(escolhidas[n], codigo, TAM_COD);
        n++;
    }

    char id[16], desc[TAM_DESC];
    formatarIdRequisicao(req->id, id);
    snprintf(desc, TAM_DESC, "Atendimento da requisição %s (%d bolsa(s))", id, n);
    Operacao* op = criarOperacao(OP_ATENDIMENTO, desc);
    if (op == NULL) return;

    // Aplica as mudanças: bolsas reservadas e requisição atendida
    for (int i = 0; i < n; i++) {
        Bolsa* b = buscarBolsaRec(estoque->inicio, escolhidas[i]);
        copiar(b->status, ST_RESERVADA, TAM_TXT);
        copiar(req->bolsasAlocadas[i], escolhidas[i], TAM_COD);
    }
    req->qtdAlocada = n;
    copiar(req->status, ST_ATENDIDA, TAM_TXT);

    Requisicao* atendida = dequeue(fila);   // sai do início da fila (FIFO)
    op->requisicao = *atendida;             // cópia fica no histórico
    op->requisicao.prox = NULL;
    push(historico, op);
    free(atendida);                         // libera o nó da fila

    cout << "  " << id << " atendida. Bolsas com status \"" << ST_RESERVADA << "\": ";
    for (int i = 0; i < n; i++) cout << escolhidas[i] << (i < n - 1 ? ", " : "\n");
}

// US06 - Histórico e desfazer
void exibirHistorico(PilhaHistorico* historico) {
    cout << "\n--- Histórico de operações (" << historico->tamanho << ") ---\n";
    if (historico->topo == NULL) {
        cout << "  Nenhuma operação registrada.\n";
        return;
    }
    int i = 1;
    for (Operacao* op = historico->topo; op != NULL; op = op->prox, i++)
        cout << "  " << i << ". " << op->descricao << (i == 1 ? "  <- topo" : "") << "\n";
}

void desfazerUltimaOperacao(PilhaHistorico* historico, ListaEstoque* estoque,
                            FilaRequisicoes* fila) {
    cout << "\n--- Desfazer última operação ---\n";
    Operacao* op = pop(historico);
    if (op == NULL) {
        cout << "  Nenhuma operação para desfazer.\n";
        return;
    }

    switch (op->tipo) {
        case OP_CADASTRO_BOLSA:
            removerBolsa(estoque, op->bolsa.codigo, NULL);
            break;

        case OP_DESCARTE_BOLSA: {
            Bolsa* b = criarBolsa(op->bolsa.codigo, op->bolsa.hemocomponente,
                                  op->bolsa.tipoSanguineo, op->bolsa.dataColeta,
                                  op->bolsa.dataValidade, op->bolsa.status);
            if (b == NULL) {
                push(historico, op);  // não perde a operação
                return;
            }
            inserirFimEstoque(estoque, b);
            break;
        }

        case OP_NOVA_REQUISICAO:
            removerRequisicaoPorId(fila, op->requisicao.id);
            break;

        case OP_ATENDIMENTO: {
            Requisicao* r = (Requisicao*)malloc(sizeof(Requisicao));
            if (r == NULL) {
                cout << "  Erro: memória insuficiente.\n";
                push(historico, op);
                return;
            }
            for (int i = 0; i < op->requisicao.qtdAlocada; i++) {
                Bolsa* b = buscarBolsaRec(estoque->inicio, op->requisicao.bolsasAlocadas[i]);
                if (b != NULL) copiar(b->status, ST_DISPONIVEL, TAM_TXT);
            }
            *r = op->requisicao;
            r->qtdAlocada = 0;
            copiar(r->status, ST_PENDENTE, TAM_TXT);
            devolverAoInicioFila(fila, r);
            break;
        }
    }

    cout << "  Desfeito: " << op->descricao << "\n";
    free(op);
}

// US07 - Relatório consolidado (sem previsão estatística; só contagens)
void gerarRelatorio(ListaEstoque* estoque, FilaRequisicoes* fila, PilhaHistorico* historico) {
    cout << "\n--- Relatório consolidado ---\n";
    cout << "  Bolsas no estoque ............ " << estoque->tamanho << "\n";
    cout << "    Disponíveis ................ " << contarPorStatusRec(estoque->inicio, ST_DISPONIVEL) << "\n";
    cout << "    Reservadas p/ despacho ..... " << contarPorStatusRec(estoque->inicio, ST_RESERVADA) << "\n";
    cout << "  Disponíveis por hemocomponente:\n";
    for (int i = 0; i < QTD_HEMOCOMPONENTES; i++)
        cout << "    " << HEMOCOMPONENTES[i] << ": "
             << contarDisponiveisPorHemoRec(estoque->inicio, HEMOCOMPONENTES[i]) << "\n";
    cout << "  Requisições pendentes na fila  " << fila->tamanho << "\n";
    cout << "  Requisições atendidas ........ " << contarOperacoesRec(historico->topo, OP_ATENDIMENTO) << "\n";
    cout << "  Bolsas descartadas ........... " << contarOperacoesRec(historico->topo, OP_DESCARTE_BOLSA) << "\n";
}

/* ============================================================================
 *  MENU PRINCIPAL
 * ========================================================================== */

void exibirMenu() {
    cout << "\n========== HemoTrack - Rota Vital ==========\n"
         << "  ESTOQUE (lista)\n"
         << "   1 - Cadastrar bolsa\n"
         << "   2 - Listar estoque\n"
         << "   3 - Buscar bolsa por código\n"
         << "   4 - Descartar bolsa\n"
         << "  REQUISIÇÕES (fila)\n"
         << "   5 - Registrar requisição\n"
         << "   6 - Exibir fila de requisições\n"
         << "   7 - Atender próxima requisição\n"
         << "  HISTÓRICO (pilha)\n"
         << "   8 - Exibir histórico\n"
         << "   9 - Desfazer última operação\n"
         << "  RELATÓRIO\n"
         << "  10 - Relatório consolidado\n"
         << "   0 - Sair\n";
}

int main() {
    ListaEstoque estoque;
    FilaRequisicoes fila;
    PilhaHistorico historico;

    inicializarEstoque(&estoque);
    inicializarFila(&fila);
    inicializarHistorico(&historico);

    int opcao = -1;
    while (opcao != 0 && !fimEntrada) {
        exibirMenu();
        opcao = lerInteiro("Escolha: ", 0, 10);
        if (fimEntrada) break;

        switch (opcao) {
            case 1:  cadastrarBolsa(&estoque, &historico); break;
            case 2:  listarEstoque(&estoque); break;
            case 3:  consultarBolsa(&estoque); break;
            case 4:  descartarBolsa(&estoque, &historico); break;
            case 5:  registrarRequisicao(&fila, &historico); break;
            case 6:  exibirFila(&fila); break;
            case 7:  atenderProximaRequisicao(&fila, &estoque, &historico); break;
            case 8:  exibirHistorico(&historico); break;
            case 9:  desfazerUltimaOperacao(&historico, &estoque, &fila); break;
            case 10: gerarRelatorio(&estoque, &fila, &historico); break;
            case 0:  cout << "Encerrando...\n"; break;
        }
    }

    // Libera toda a memória alocada com malloc
    liberarEstoque(&estoque);
    liberarFila(&fila);
    liberarHistorico(&historico);
    cout << "Memória liberada. Até logo!\n";
    return 0;
}
