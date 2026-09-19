package br.edu.rotavital;

import java.util.Objects;
import java.util.Optional;

/** FIFO: inserir no fim e remover no início. */
public final class FilaRequisicoes {
    private static final class NoRequisicao {
        final Requisicao dado;
        NoRequisicao proximo;
        NoRequisicao(Requisicao dado) { this.dado = dado; }
    }
    private NoRequisicao inicio;
    private NoRequisicao fim;

    public boolean inserir(Requisicao requisicao) {
        Objects.requireNonNull(requisicao);
        if (buscar(requisicao.id()).isPresent()) return false;
        NoRequisicao novo = new NoRequisicao(requisicao);
        if (fim == null) inicio = novo;
        else fim.proximo = novo;
        fim = novo;
        return true;
    }
    public Optional<Requisicao> buscar(int id) {
        for (NoRequisicao atual = inicio; atual != null; atual = atual.proximo)
            if (atual.dado.id() == id) return Optional.of(atual.dado);
        return Optional.empty();
    }
    public Optional<Requisicao> consultar() {
        return inicio == null ? Optional.empty() : Optional.of(inicio.dado);
    }
    public Optional<Requisicao> remover() {
        if (inicio == null) return Optional.empty();
        Optional<Requisicao> saida = Optional.of(inicio.dado);
        descartarInicio();
        return saida;
    }
    // Usado depois de preparar o histórico; não aloca memória.
    void descartarInicio() {
        inicio = inicio.proximo;
        if (inicio == null) fim = null;
    }
    public void limpar() { inicio = null; fim = null; }
    /** Cópia para transporte: o vetor não substitui a estrutura encadeada. */
    public Requisicao[] todos() {
        int quantidade = 0;
        for (NoRequisicao atual = inicio; atual != null; atual = atual.proximo) quantidade++;
        Requisicao[] copia = new Requisicao[quantidade];
        int indice = 0;
        for (NoRequisicao atual = inicio; atual != null; atual = atual.proximo) copia[indice++] = atual.dado;
        return copia;
    }
}
