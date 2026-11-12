package br.com.hemotrack.model;

public enum StatusRequisicao {
    PENDENTE("Pendente de Alocação"),
    ALOCADA("Alocada"),
    EM_TRANSPORTE("Em Transporte"),
    ENTREGUE("Entregue");

    private final String rotulo;

    StatusRequisicao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
