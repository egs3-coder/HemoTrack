package br.com.hemotrack.modelo;

public enum EstadoRequisicao {
    PENDENTE_ALOCACAO("Pendente de alocação"),
    PARCIALMENTE_ALOCADA("Parcialmente alocada"),
    ALOCADA("Alocada"),
    EM_TRANSPORTE("Em transporte"),
    ENTREGUE("Entregue"),
    CANCELADA("Cancelada");

    private final String descricao;

    EstadoRequisicao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
