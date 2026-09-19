package br.edu.rotavital;

import java.util.Objects;
import java.util.Optional;

/** LIFO: atende consultas do evento mais recente primeiro. */
public final class PilhaHistorico {
    private static final class NoOperacao {
        final Operacao dado;
        final NoOperacao proximo;
        NoOperacao(Operacao dado, NoOperacao proximo) { this.dado = dado; this.proximo = proximo; }
    }
    private NoOperacao topo;
    public void inserir(Operacao operacao) {
        topo = new NoOperacao(Objects.requireNonNull(operacao), topo);
    }
    public Optional<Operacao> consultar() {
        return topo == null ? Optional.empty() : Optional.of(topo.dado);
    }
    public Optional<Operacao> remover() {
        if (topo == null) return Optional.empty();
        Optional<Operacao> saida = Optional.of(topo.dado);
        topo = topo.proximo;
        return saida;
    }
    public void limpar() { topo = null; }
    /** Cópia para transporte: o vetor não substitui a estrutura encadeada. */
    public Operacao[] todos() {
        int quantidade = 0;
        for (NoOperacao atual = topo; atual != null; atual = atual.proximo) quantidade++;
        Operacao[] copia = new Operacao[quantidade];
        int indice = 0;
        for (NoOperacao atual = topo; atual != null; atual = atual.proximo) copia[indice++] = atual.dado;
        return copia;
    }
}
