package br.com.hemotrack.api;

import br.com.hemotrack.excecao.RecursoNaoEncontradoException;
import br.com.hemotrack.excecao.RegraNegocioException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converte excecoes de negocio em respostas JSON com o codigo HTTP adequado. */
@RestControllerAdvice(basePackages = "br.com.hemotrack.api")
public class TratadorErrosApi {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(RecursoNaoEncontradoException excecao) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", excecao.getMessage()));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, String>> regraNegocio(RegraNegocioException excecao) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("erro", excecao.getMessage()));
    }
}
