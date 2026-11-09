package br.com.hemotrack.web;

import br.com.hemotrack.excecao.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Tratamento centralizado das excecoes de negocio nas telas:
 * volta para a pagina anterior exibindo a mensagem de erro.
 */
@ControllerAdvice(basePackages = "br.com.hemotrack.web")
public class TratadorErrosWeb {

    @ExceptionHandler(RegraNegocioException.class)
    public String tratar(RegraNegocioException excecao, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("erro", excecao.getMessage());
        String paginaAnterior = request.getHeader("Referer");
        return "redirect:" + (paginaAnterior != null ? paginaAnterior : "/");
    }
}
