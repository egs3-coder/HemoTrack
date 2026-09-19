package br.edu.rotavital;
public record Requisicao(int id, int hospitalId, TipoSanguineo tipo, int quantidade,
                         Hemocomponente hemocomponente, Urgencia urgencia) {
    public Requisicao {
        if (id <= 0 || hospitalId <= 0 || tipo == null || quantidade <= 0
                || hemocomponente == null || urgencia == null)
            throw new IllegalArgumentException("Requisição inválida.");
    }
    public Requisicao(int id, int hospitalId, TipoSanguineo tipo, int quantidade) {
        this(id,hospitalId,tipo,quantidade,Hemocomponente.CONCENTRADO_HEMACIAS,Urgencia.NORMAL);
    }
}
