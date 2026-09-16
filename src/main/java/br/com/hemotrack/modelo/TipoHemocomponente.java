package br.com.hemotrack.modelo;

public enum TipoHemocomponente {
    CONCENTRADO_HEMACIAS("Concentrado de Hemácias"),
    PLAQUETAS("Plaquetas"),
    PLASMA("Plasma");

    private final String descricao;

    TipoHemocomponente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
