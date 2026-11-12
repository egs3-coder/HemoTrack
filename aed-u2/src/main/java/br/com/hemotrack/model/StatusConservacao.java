package br.com.hemotrack.model;

public enum StatusConservacao {
    NORMAL("Normal / Seguro"),
    ALERTA("Alerta - Fora da Faixa"),
    COMPROMETIDO("Comprometido");

    private final String rotulo;

    StatusConservacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
