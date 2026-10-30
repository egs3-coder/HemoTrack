package br.com.hemotrack.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * US02 - Nenhuma tela do sistema pode ser acessada sem que a pessoa tenha entrado na plataforma.
 * Sem usuario na sessao, envia para a tela de entrada (cenario 7). As telas autenticadas nao ficam
 * guardadas no cache do navegador, para que depois de "Sair" o botao Voltar tambem exija login (cenario 8).
 */
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_NOME = "usuarioNome";
    public static final String ATRIBUTO_PERFIL = "usuarioPerfil";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession sessao = request.getSession(false);
        if (sessao != null && sessao.getAttribute(ATRIBUTO_NOME) != null) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
