package br.edu.rotavital;
public record Hospital(int id, String nome, String cidade) {
    public Hospital {
        if (id <= 0) throw new IllegalArgumentException("ID inválido.");
        Validacao.texto(nome, 79); Validacao.texto(cidade, 59);
    }
    public Hospital(int id, String nome) { this(id, nome, "Recife"); }
}
