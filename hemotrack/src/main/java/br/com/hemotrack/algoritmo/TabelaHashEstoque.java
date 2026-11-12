package br.com.hemotrack.algoritmo;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.TipoSanguineo;

/**
 * ALGORITMO 3 (U2) - INDICE DE ESTOQUE POR HASH (tabela propria, sem java.util.HashMap).
 *
 * Chave: (hemocomponente, tipo sanguineo) -> numero inteiro
 *        chave = hemocomponente.ordinal() * 8 + tipo.ordinal()      (4 x 8 = 32 chaves possiveis: 0..31)
 * Funcao hash: h(chave) = chave % TAMANHO, com TAMANHO = 11 (primo).
 * Colisao: ENCADEAMENTO SEPARADO. Cada posicao do vetor guarda uma lista ligada de entradas;
 *          chaves diferentes com o mesmo h(chave) ficam na mesma cadeia.
 * Valor:   cada entrada guarda a ListaFefo das bolsas daquela combinacao, ja em ordem de validade.
 *
 * Exemplo de colisao: Hemacias O- (chave 0*8+0 = 0) e Plaquetas A+ (chave 1*8+3 = 11)
 *                     -> 0 % 11 = 0 e 11 % 11 = 0: as duas ficam na cadeia do slot 0.
 */
public class TabelaHashEstoque {

    public static final int TAMANHO = 11;

    /** Entrada da cadeia: chave, lista FEFO daquela chave e proxima entrada do mesmo slot. */
    private static final class Entrada {
        private final int chave;
        private final ListaFefo fefo = new ListaFefo();
        private Entrada prox;

        private Entrada(int chave) {
            this.chave = chave;
        }
    }

    private final Entrada[] slots = new Entrada[TAMANHO];
    private int chavesDistintas;
    private int totalBolsas;
    private int colisoes;

    /** Monta o indice a partir das bolsas lidas do banco (em qualquer ordem). */
    public static TabelaHashEstoque construir(Iterable<Bolsa> bolsas) {
        TabelaHashEstoque tabela = new TabelaHashEstoque();
        for (Bolsa bolsa : bolsas) {
            tabela.inserir(bolsa);
        }
        return tabela;
    }

    /** Transforma (componente, tipo) em inteiro. O(1). */
    public static int chave(Hemocomponente componente, TipoSanguineo tipo) {
        return componente.ordinal() * MatrizCompatibilidade.N_TIPOS + tipo.ordinal();
    }

    /** Funcao hash por divisao. O(1). */
    static int hash(int chave) {
        return chave % TAMANHO;
    }

    /** Procura a entrada percorrendo SO a cadeia do slot. O(1 + alfa), alfa = fator de carga. */
    private Entrada buscarEntrada(int chave) {
        Entrada atual = slots[hash(chave)];
        while (atual != null) {
            if (atual.chave == chave) {
                return atual;
            }
            atual = atual.prox;
        }
        return null;
    }

    /**
     * Coloca a bolsa no indice: O(1 + alfa) para achar/criar a entrada + O(m) da insercao
     * ordenada na ListaFefo daquela chave (m = bolsas com o mesmo componente e tipo).
     */
    public void inserir(Bolsa bolsa) {
        int chave = chave(bolsa.getHemocomponente(), bolsa.getTipoSanguineo());
        Entrada entrada = buscarEntrada(chave);
        if (entrada == null) {
            int indice = hash(chave);
            if (slots[indice] != null) {
                colisoes++;
            }
            entrada = new Entrada(chave);
            entrada.prox = slots[indice];   // insere no inicio da cadeia: O(1)
            slots[indice] = entrada;
            chavesDistintas++;
        }
        entrada.fefo.inserir(bolsa);
        totalBolsas++;
    }

    /** Lista FEFO de (componente, tipo), ou null se nao ha bolsas dessa combinacao. O(1 + alfa). */
    public ListaFefo buscar(Hemocomponente componente, TipoSanguineo tipo) {
        Entrada entrada = buscarEntrada(chave(componente, tipo));
        return entrada == null ? null : entrada.fefo;
    }

    public int getTotalBolsas() {
        return totalBolsas;
    }

    public int getChavesDistintas() {
        return chavesDistintas;
    }

    public int getColisoes() {
        return colisoes;
    }

    /** alfa = chaves / slots. No maximo 32 / 11 ~ 2,9: limitado por uma constante. */
    public double fatorDeCarga() {
        return (double) chavesDistintas / TAMANHO;
    }
}
