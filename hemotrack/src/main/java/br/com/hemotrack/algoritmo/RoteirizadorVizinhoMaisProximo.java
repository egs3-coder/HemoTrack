package br.com.hemotrack.algoritmo;

import br.com.hemotrack.model.Rota;
import br.com.hemotrack.model.Trecho;
import java.util.ArrayList;
import java.util.List;

/**
 * ALGORITMO 1 (U2) - ROTA POR VIZINHO MAIS PROXIMO (heuristica gulosa).
 *
 * A malha (trechos de mao dupla) vira uma MATRIZ DE ADJACENCIA tempo[P][P], P = pontos.
 * Partindo do hemocentro, a cada passo o caminhao vai para o VIZINHO AINDA NAO VISITADO
 * com o MENOR tempo de trecho a partir do ponto atual. Para quando chega ao hospital.
 *
 * Beco sem saida: se o ponto atual nao tem vizinho nao visitado, o algoritmo volta um
 * ponto (desempilha) e tenta o proximo vizinho mais proximo de la. Como cada ponto e
 * marcado como visitado uma unica vez, isso nunca entra em ciclo.
 *
 * Por ser guloso, NAO garante o menor tempo total em toda malha (olha so o proximo passo).
 * Em troca e simples e O(P^2). O teste US05 "vizinhoMaisProximoEGuloso" mostra um caso.
 *
 * Estruturas feitas a mao: vetor de nomes (indice de cada ponto), matriz int[][], vetor
 * boolean[] de visitados e um vetor int[] usado como PILHA do caminho. Sem HashMap,
 * sem PriorityQueue.
 *
 * Versao C/C++ (regra geral da U2): repositorio AED, cpp/rota_vital.cpp ->
 *   rotaVizinhoMaisProximo(adj, origem, destino, caminho[], *tamanho). No C++ a malha ja e uma
 *   matriz fixa ESTRADAS[6][6]; aqui ela e montada a partir dos trechos cadastrados (passos 1 e 2).
 */
public final class RoteirizadorVizinhoMaisProximo {

    static final int SEM_LIGACAO = Integer.MAX_VALUE;

    private RoteirizadorVizinhoMaisProximo() {
    }

    /**
     * @return a rota do vizinho mais proximo de 'origem' ate 'destino',
     *         ou null se nao existe ligacao entre os dois pela malha.
     */
    public static Rota calcular(List<Trecho> trechos, String origem, String destino) {
        // (1) Indexa os pontos: cada nome recebe um numero 0..P-1. O(T * P).
        String[] nomes = new String[2 * trechos.size() + 2];
        int p = 0;
        p = registrar(nomes, p, origem);
        p = registrar(nomes, p, destino);
        for (Trecho trecho : trechos) {
            p = registrar(nomes, p, trecho.getOrigem());
            p = registrar(nomes, p, trecho.getDestino());
        }

        // (2) Matriz de adjacencia. O(P^2) para iniciar + O(T * P) para preencher.
        int[][] tempo = new int[p][p];
        double[][] km = new double[p][p];
        for (int i = 0; i < p; i++) {
            for (int j = 0; j < p; j++) {
                tempo[i][j] = SEM_LIGACAO;
            }
        }
        for (Trecho trecho : trechos) {
            int a = indice(nomes, p, trecho.getOrigem());
            int b = indice(nomes, p, trecho.getDestino());
            if (trecho.getTempoMin() < tempo[a][b]) {   // trecho repetido: fica o mais rapido
                tempo[a][b] = trecho.getTempoMin();
                tempo[b][a] = trecho.getTempoMin();      // mao dupla
                km[a][b] = trecho.getDistanciaKm();
                km[b][a] = trecho.getDistanciaKm();
            }
        }

        // (3) Vizinho mais proximo com retrocesso. Cada ponto entra na pilha no maximo 1 vez
        //     e cada iteracao varre uma linha da matriz (P colunas): O(P^2) no total.
        int alvo = indice(nomes, p, destino);
        boolean[] visitado = new boolean[p];
        int[] pilha = new int[p];
        int topo = 0;
        pilha[topo++] = 0;              // a origem foi registrada primeiro: indice 0
        visitado[0] = true;

        while (topo > 0) {
            int atual = pilha[topo - 1];
            if (atual == alvo) {
                break;
            }
            int vizinho = -1;
            for (int v = 0; v < p; v++) {
                if (!visitado[v] && tempo[atual][v] != SEM_LIGACAO
                        && (vizinho == -1 || tempo[atual][v] < tempo[atual][vizinho])) {
                    vizinho = v;
                }
            }
            if (vizinho == -1) {
                topo--;                 // beco sem saida: volta um ponto
            } else {
                visitado[vizinho] = true;
                pilha[topo++] = vizinho;
            }
        }

        if (topo == 0) {
            return null;                // a pilha esvaziou sem chegar ao destino
        }

        // (4) A pilha, de baixo para cima, e o caminho. Soma tempo e km dos trechos. O(P).
        List<String> pontos = new ArrayList<>(topo);
        int tempoTotal = 0;
        double kmTotal = 0;
        pontos.add(nomes[pilha[0]]);
        for (int i = 1; i < topo; i++) {
            tempoTotal += tempo[pilha[i - 1]][pilha[i]];
            kmTotal += km[pilha[i - 1]][pilha[i]];
            pontos.add(nomes[pilha[i]]);
        }
        return new Rota(pontos, tempoTotal, kmTotal);
    }

    /** Busca sequencial do nome no vetor de pontos. O(P). */
    private static int indice(String[] nomes, int quantidade, String nome) {
        for (int i = 0; i < quantidade; i++) {
            if (nomes[i].equals(nome)) {
                return i;
            }
        }
        return -1;
    }

    /** Acrescenta o nome se ainda nao existe; devolve a nova quantidade. O(P). */
    private static int registrar(String[] nomes, int quantidade, String nome) {
        if (indice(nomes, quantidade, nome) == -1) {
            nomes[quantidade++] = nome;
        }
        return quantidade;
    }
}
