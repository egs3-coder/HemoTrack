package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.hemotrack.model.Perfil;
import br.com.hemotrack.model.Usuario;
import br.com.hemotrack.repository.UsuarioRepository;
import br.com.hemotrack.service.AutenticacaoService;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * US02 - A conta criada pela tela fica gravada no banco em arquivo: depois de parar e iniciar
 * a aplicacao de novo, a pessoa entra com o mesmo usuario e senha (nao precisa criar outra conta).
 */
@DisplayName("US02 - Conta criada continua existindo depois de reiniciar a aplicação")
class US02PersistenciaContaTest {

    @TempDir
    Path pasta;

    private ConfigurableApplicationContext iniciar() {
        return new SpringApplicationBuilder(HemoTrackApplication.class)
                .web(WebApplicationType.NONE)
                .properties("HEMOTRACK_DB=" + pasta.resolve("hemotrack").toString().replace('\\', '/'),
                        "hemotrack.dados-demo=false")
                .run();
    }

    @Test
    @DisplayName("Cenário 1 + 5: cria a conta, reinicia e entra com usuário e senha")
    void contaPersisteEntreExecucoes() {
        try (ConfigurableApplicationContext primeiraExecucao = iniciar()) {
            primeiraExecucao.getBean(AutenticacaoService.class)
                    .cadastrar("Marina Costa", "Marina", "segredo123", "segredo123", Perfil.PROFISSIONAL_HOSPITALAR);
        }

        try (ConfigurableApplicationContext segundaExecucao = iniciar()) {
            assertTrue(segundaExecucao.getBean(UsuarioRepository.class).existsByLogin("marina"));
            Usuario marina = segundaExecucao.getBean(AutenticacaoService.class).autenticar("marina", "segredo123");
            assertEquals("Marina Costa (Profissional hospitalar)",
                    marina.getNome() + " (" + marina.getPerfil().getRotulo() + ")");
        }
    }
}
