package br.edu.rotavital;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratamentoErros {
    public record Erro(String mensagem) { }
    @ExceptionHandler(FalhaDominio.class)
    public ResponseEntity<Erro> dominio(FalhaDominio erro) {
        int codigo = switch (erro.motivo()) { case NAO_ENCONTRADO -> 404; case CONFLITO -> 409; case INVALIDO -> 400; };
        return ResponseEntity.status(codigo).body(new Erro(erro.getMessage()));
    }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Erro> invalido(Exception erro) {
        return ResponseEntity.badRequest().body(new Erro("Dados inválidos. Confira IDs, quantidades e tipos."));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Erro> corpoInvalido(HttpMessageNotReadableException erro) {
        return ResponseEntity.badRequest().body(new Erro("JSON ou campos inválidos. Use IDs positivos, quantidade positiva e tipos cadastrados."));
    }
}
