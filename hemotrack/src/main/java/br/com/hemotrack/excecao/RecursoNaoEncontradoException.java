package br.com.hemotrack.excecao;

/** Especializacao usada quando um registro procurado nao existe. */
public class RecursoNaoEncontradoException extends RegraNegocioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
