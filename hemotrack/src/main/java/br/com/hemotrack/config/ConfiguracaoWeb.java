package br.com.hemotrack.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {

    private final AutenticacaoInterceptor autenticacaoInterceptor;

    public ConfiguracaoWeb(AutenticacaoInterceptor autenticacaoInterceptor) {
        this.autenticacaoInterceptor = autenticacaoInterceptor;
    }

    /**
     * US02 - Telas publicas: entrada, criacao de conta (e a checagem de usuario existente) e
     * "Esqueci minha senha". A API do sensor (/api) fica fora do login: quem a chama e um
     * equipamento, nao uma pessoa (US06).
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacaoInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/cadastro", "/cadastro/**", "/recuperar-senha",
                        "/css/**", "/js/**", "/api/**", "/error");
    }

    /**
     * US02 - O console do H2 e um servlet separado (nao passa pelo interceptor do Spring MVC);
     * este filtro garante que ele tambem so abra para quem entrou na plataforma.
     */
    @Bean
    public FilterRegistrationBean<Filter> filtroConsoleH2() {
        Filter filtro = (requisicao, resposta, cadeia) -> {
            HttpServletRequest http = (HttpServletRequest) requisicao;
            HttpSession sessao = http.getSession(false);
            if (sessao == null || sessao.getAttribute(AutenticacaoInterceptor.ATRIBUTO_NOME) == null) {
                ((HttpServletResponse) resposta).sendRedirect(http.getContextPath() + "/login");
                return;
            }
            cadeia.doFilter(requisicao, resposta);
        };
        FilterRegistrationBean<Filter> registro = new FilterRegistrationBean<>(filtro);
        registro.addUrlPatterns("/h2-console/*");
        return registro;
    }
}
