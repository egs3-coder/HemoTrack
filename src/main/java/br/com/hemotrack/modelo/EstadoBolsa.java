package br.com.hemotrack.modelo;

public enum EstadoBolsa {
    DISPONIVEL("Disponível"),
    RESERVADA_PARA_DESPACHO("Reservada para despacho"),
    DISTRIBUIDA("Distribuída"),
    VENCIDA("Vencida"),
    DESCARTADA("Descartada");

    private final String descricao;

    EstadoBolsa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
