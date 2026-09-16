package br.com.hemotrack.modelo;

public enum TipoSanguineo {
    O_NEGATIVO("O-"),
    O_POSITIVO("O+"),
    A_NEGATIVO("A-"),
    A_POSITIVO("A+"),
    B_NEGATIVO("B-"),
    B_POSITIVO("B+"),
    AB_NEGATIVO("AB-"),
    AB_POSITIVO("AB+");

    private final String descricao;

    TipoSanguineo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
