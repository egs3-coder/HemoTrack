package br.edu.rotavital;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** ABB própria, sem balanceamento. Chave única: identificador do hospital. */
public final class ArvoreHospitais {
    public enum Ordem { PRE_ORDEM, EM_ORDEM, POS_ORDEM }
    private static final class NoHospital {
        Hospital dado;
        NoHospital esquerda, direita;
        NoHospital(Hospital dado) { this.dado = dado; }
    }
    private NoHospital raiz;
    public boolean inserir(Hospital hospital) {
        Objects.requireNonNull(hospital);
        if (consultar(hospital.id()).isPresent()) return false;
        raiz = inserirNo(raiz, hospital);
        return true;
    }
    private NoHospital inserirNo(NoHospital no, Hospital hospital) {
        if (no == null) return new NoHospital(hospital);
        if (hospital.id() < no.dado.id()) no.esquerda = inserirNo(no.esquerda, hospital);
        else no.direita = inserirNo(no.direita, hospital);
        return no;
    }
    public Optional<Hospital> consultar(int id) { return consultarNo(raiz, id); }
    private Optional<Hospital> consultarNo(NoHospital no, int id) {
        if (no == null) return Optional.empty();
        if (no.dado.id() == id) return Optional.of(no.dado);
        return consultarNo(id < no.dado.id() ? no.esquerda : no.direita, id);
    }
    public Optional<Hospital> remover(int id) {
        Optional<Hospital> saida = consultar(id); // Guarda o hospital original.
        if (saida.isPresent()) raiz = removerNo(raiz, id);
        return saida;
    }
    private NoHospital removerNo(NoHospital no, int id) {
        if (no == null) return null;
        if (id < no.dado.id()) no.esquerda = removerNo(no.esquerda, id);
        else if (id > no.dado.id()) no.direita = removerNo(no.direita, id);
        else {
            if (no.esquerda == null) return no.direita;
            if (no.direita == null) return no.esquerda;
            NoHospital sucessor = no.direita;
            while (sucessor.esquerda != null) sucessor = sucessor.esquerda;
            no.dado = sucessor.dado;
            no.direita = removerNo(no.direita, sucessor.dado.id());
        }
        return no;
    }
    public void percorrer(Ordem ordem, Consumer<Hospital> visitar) {
        percorrerNo(raiz, Objects.requireNonNull(ordem), Objects.requireNonNull(visitar));
    }
    private void percorrerNo(NoHospital no, Ordem ordem, Consumer<Hospital> visitar) {
        if (no == null) return;
        if (ordem == Ordem.PRE_ORDEM) visitar.accept(no.dado);
        percorrerNo(no.esquerda, ordem, visitar);
        if (ordem == Ordem.EM_ORDEM) visitar.accept(no.dado);
        percorrerNo(no.direita, ordem, visitar);
        if (ordem == Ordem.POS_ORDEM) visitar.accept(no.dado);
    }
    public long contar() { return contarNo(raiz); }
    private long contarNo(NoHospital no) {
        return no == null ? 0 : 1 + contarNo(no.esquerda) + contarNo(no.direita);
    }
    public void limpar() { raiz = null; }
    public record Ramo(Hospital hospital, Ramo esquerda, Ramo direita) { }
    public Ramo estrutura() { return copiarRamo(raiz); }
    private Ramo copiarRamo(NoHospital no) {
        return no == null ? null : new Ramo(no.dado, copiarRamo(no.esquerda), copiarRamo(no.direita));
    }
    public Hospital[] todos(Ordem ordem) {
        Hospital[] copia = new Hospital[Math.toIntExact(contar())];
        int[] indice = {0};
        percorrer(ordem, h -> copia[indice[0]++] = h);
        return copia;
    }
}
