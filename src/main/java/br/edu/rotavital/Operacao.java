package br.edu.rotavital;

/** Registro de atendimento. Remover este registro não desfaz a entrega. */
public record Operacao(int requisicaoId, int hospitalId, int quantidade, TipoSanguineo tipo) {
    public Operacao {
        if (requisicaoId <= 0 || hospitalId <= 0 || quantidade <= 0 || tipo == null)
            throw new IllegalArgumentException("Operação inválida");
    }
}
