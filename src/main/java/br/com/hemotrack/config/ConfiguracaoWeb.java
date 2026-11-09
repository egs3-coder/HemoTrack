package br.com.hemotrack.config;

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
     * Telas publicas: entrada, criacao de conta (e a checagem de usuario existente) e "Esqueci minha senha".
     * A API do sensor (/api) fica fora do login: quem a chama e um equipamento, nao uma pessoa (US06).
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacaoInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/cadastro", "/cadastro/**", "/recuperar-senha", "/css/**", "/js/**", "/api/**", "/h2-console/**", "/error");
    }
}
