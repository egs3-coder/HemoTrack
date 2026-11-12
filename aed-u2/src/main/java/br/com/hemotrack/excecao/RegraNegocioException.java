package br.com.hemotrack.excecao;

/** Erro de regra de negocio; a mensagem e mostrada ao usuario. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
