package br.edu.rotavital;
/** Erro de negócio independente do protocolo HTTP e do Spring. */
public final class FalhaDominio extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public enum Motivo { NAO_ENCONTRADO, CONFLITO, INVALIDO }
    private final Motivo motivo;
    public FalhaDominio(Motivo motivo, String mensagem) { super(mensagem); this.motivo = motivo; }
    public Motivo motivo() { return motivo; }
}
