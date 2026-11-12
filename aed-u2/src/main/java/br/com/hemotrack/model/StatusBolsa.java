package br.com.hemotrack.model;

public enum StatusBolsa {
    DISPONIVEL("Disponível"),
    RESERVADA("Reservada para Despacho"),
    EM_TRANSPORTE("Em Transporte"),
    ENTREGUE("Entregue"),
    DESCARTADA("Descartada");

    private final String rotulo;

    StatusBolsa(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
