package br.com.hemotrack.modelo;

public class ResumoRequisicao {

    private final Requisicao requisicao;
    private final int quantidadeAlocada;
    private final int quantidadeDisponivel;

    public ResumoRequisicao(Requisicao requisicao,
                            int quantidadeAlocada,
                            int quantidadeDisponivel) {
        this.requisicao = requisicao;
        this.quantidadeAlocada = quantidadeAlocada;
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    public Requisicao getRequisicao() {
        return requisicao;
    }

    public int getQuantidadeAlocada() {
        return quantidadeAlocada;
    }

    public int getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public int getQuantidadePendente() {
        return Math.max(requisicao.getQuantidade() - quantidadeAlocada, 0);
    }

    public int getQuantidadeMaximaAlocacao() {
        return Math.min(getQuantidadePendente(), quantidadeDisponivel);
    }

    public boolean isPodeAlocar() {
        return getQuantidadeMaximaAlocacao() > 0
                && (requisicao.getEstado() == EstadoRequisicao.PENDENTE_ALOCACAO
                || requisicao.getEstado() == EstadoRequisicao.PARCIALMENTE_ALOCADA);
    }
}
