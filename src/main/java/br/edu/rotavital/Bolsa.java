package br.edu.rotavital;
/** Metadados são armazenados; não existe ordenação FEFO nem compatibilidade. */
public record Bolsa(int id, TipoSanguineo tipo, int volumeMl, String codigo,
                    Hemocomponente hemocomponente, String dataColeta, String dataValidade) {
    public Bolsa {
        if (id <= 0 || tipo == null || volumeMl <= 0 || hemocomponente == null)
            throw new IllegalArgumentException("Bolsa inválida.");
        Validacao.texto(codigo, 47);
        if (!Validacao.data(dataValidade).isAfter(Validacao.data(dataColeta)))
            throw new IllegalArgumentException("Validade deve ser posterior à coleta.");
    }
    /** Construtor para os exemplos acadêmicos reproduzíveis. */
    public Bolsa(int id, TipoSanguineo tipo, int volumeMl) {
        this(id,tipo,volumeMl,"B-"+id,Hemocomponente.CONCENTRADO_HEMACIAS,"2026-01-01","2026-02-01");
    }
}
