package br.com.hemotrack.algoritmo;

import br.com.hemotrack.model.Bolsa;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ALGORITMO 2 (U2) - FEFO (First Expired, First Out) por INSERCAO ORDENADA.
 *
 * Lista simplesmente encadeada escrita a mao (sem LinkedList, sem sort, sem ORDER BY).
 * A ordem e garantida no momento da insercao: cada bolsa nova anda pela lista ate achar
 * a primeira bolsa que vence DEPOIS dela e entra antes dessa. Assim a cabeca e sempre a
 * bolsa de validade mais proxima.
 *
 * Desempate: mesma validade -> menor codigo primeiro (ordem deterministica).
 */
public class ListaFefo {

    /** No da lista: a bolsa e a referencia para o proximo no. */
    private static final class No {
        private final Bolsa bolsa;
        private No prox;

        private No(Bolsa bolsa) {
            this.bolsa = bolsa;
        }
    }

    private No cabeca;
    private int tamanho;

    /** true se 'a' deve ficar antes de 'b' na ordem FEFO. O(1). */
    public static boolean venceAntes(Bolsa a, Bolsa b) {
        int cmp = a.getDataValidade().compareTo(b.getDataValidade());
        if (cmp != 0) {
            return cmp < 0;
        }
        return a.getCodigo().compareTo(b.getCodigo()) < 0;
    }

    /**
     * Insercao ordenada. Pior caso O(m), m = tamanho da lista: o while anda no maximo m nos.
     * Melhor caso O(1): a bolsa vence antes da cabeca e vira a nova cabeca.
     */
    public void inserir(Bolsa bolsa) {
        No novo = new No(bolsa);
        if (cabeca == null || venceAntes(bolsa, cabeca.bolsa)) {
            novo.prox = cabeca;
            cabeca = novo;
        } else {
            No atual = cabeca;
            while (atual.prox != null && !venceAntes(bolsa, atual.prox.bolsa)) {
                atual = atual.prox;
            }
            novo.prox = atual.prox;
            atual.prox = novo;
        }
        tamanho++;
    }

    /** Bolsa que vence primeiro (sem remover). O(1). */
    public Bolsa primeira() {
        return cabeca == null ? null : cabeca.bolsa;
    }

    /** Remove e devolve a bolsa que vence primeiro. O(1): so mexe na cabeca. */
    public Bolsa removerPrimeira() {
        if (cabeca == null) {
            return null;
        }
        Bolsa bolsa = cabeca.bolsa;
        cabeca = cabeca.prox;
        tamanho--;
        return bolsa;
    }

    /**
     * Tira da lista as bolsas ja vencidas em 'hoje'. Como a lista esta ordenada por validade,
     * as vencidas formam um PREFIXO: o laco para na primeira bolsa valida. O(v), v = vencidas.
     * (Nao muda o status no banco: o descarte oficial continua sendo a rotina da US10.)
     */
    public int removerVencidas(LocalDate hoje) {
        int removidas = 0;
        while (cabeca != null && cabeca.bolsa.estaVencida(hoje)) {
            cabeca = cabeca.prox;
            tamanho--;
            removidas++;
        }
        return removidas;
    }

    public boolean estaVazia() {
        return cabeca == null;
    }

    public int tamanho() {
        return tamanho;
    }

    /** Copia a lista na ordem FEFO (para exibir ou testar). O(m). */
    public List<Bolsa> emOrdem() {
        List<Bolsa> saida = new ArrayList<>(tamanho);
        for (No no = cabeca; no != null; no = no.prox) {
            saida.add(no.bolsa);
        }
        return saida;
    }
}
