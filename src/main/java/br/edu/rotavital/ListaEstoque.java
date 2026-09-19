package br.edu.rotavital;

import java.util.Objects;
import java.util.Optional;

/** Lista própria, simplesmente encadeada. Uso isolado não é concorrente. */
public final class ListaEstoque {
    private static final class NoBolsa {
        final Bolsa dado;
        NoBolsa proximo;
        NoBolsa(Bolsa dado, NoBolsa proximo) { this.dado = dado; this.proximo = proximo; }
    }
    private NoBolsa inicio;

    public boolean inserir(Bolsa bolsa) {
        Objects.requireNonNull(bolsa);
        if (consultar(bolsa.id()).isPresent()) return false;
        for (NoBolsa no = inicio; no != null; no = no.proximo)
            if (no.dado.codigo().equals(bolsa.codigo())) return false;
        inicio = new NoBolsa(bolsa, inicio);
        return true;
    }
    public Optional<Bolsa> consultar(int id) {
        for (NoBolsa atual = inicio; atual != null; atual = atual.proximo)
            if (atual.dado.id() == id) return Optional.of(atual.dado);
        return Optional.empty();
    }
    public Optional<Bolsa> remover(int id) {
        NoBolsa anterior = null;
        NoBolsa atual = inicio;
        while (atual != null && atual.dado.id() != id) {
            anterior = atual;
            atual = atual.proximo;
        }
        if (atual == null) return Optional.empty();
        if (anterior == null) inicio = atual.proximo;
        else anterior.proximo = atual.proximo;
        return Optional.of(atual.dado);
    }
    public long contarTipo(TipoSanguineo tipo) {
        long quantidade = 0;
        for (NoBolsa atual = inicio; atual != null; atual = atual.proximo)
            if (atual.dado.tipo() == tipo) quantidade++;
        return quantidade;
    }
    public void limpar() { inicio = null; }
    /** Cópia para transporte: o vetor não substitui a estrutura encadeada. */
    public Bolsa[] todos() {
        int quantidade = 0;
        for (NoBolsa atual = inicio; atual != null; atual = atual.proximo) quantidade++;
        Bolsa[] copia = new Bolsa[quantidade];
        int indice = 0;
        for (NoBolsa atual = inicio; atual != null; atual = atual.proximo) copia[indice++] = atual.dado;
        return copia;
    }
    /** Prepara todos os nós em outra cadeia; só conecta depois de validar o lote. */
    public boolean inserirLote(Bolsa[] bolsas) {
        ListaEstoque temporaria = new ListaEstoque();
        for (Bolsa bolsa : bolsas) {
            for (NoBolsa no = inicio; no != null; no = no.proximo)
                if (no.dado.id() == bolsa.id() || no.dado.codigo().equals(bolsa.codigo())) return false;
            if (!temporaria.inserir(bolsa)) return false;
        }
        if (temporaria.inicio != null) {
            NoBolsa ultimo = temporaria.inicio;
            while (ultimo.proximo != null) ultimo = ultimo.proximo;
            ultimo.proximo = inicio;
            inicio = temporaria.inicio;
        }
        return true;
    }
}
