package br.com.hemotrack;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.hemotrack.model.Perfil;
import br.com.hemotrack.model.Usuario;
import br.com.hemotrack.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Fluxo completo das telas da US02 (com o banco H2 em memoria). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:us02;DB_CLOSE_DELAY=-1",
        "hemotrack.dados-demo=false"})
@AutoConfigureMockMvc
@DisplayName("US02 - Telas de entrada, criação de conta e saída")
class US02TelasTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limpar() {
        usuarioRepository.findByLogin("marina").ifPresent(usuarioRepository::delete);
    }

    @Test
    @DisplayName("Cenário 1: conta criada e o painel exibe 'Marina Costa (Profissional hospitalar)'")
    void criaContaEEntra() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(post("/cadastro").session(sessao)
                        .param("nome", "Marina Costa").param("login", "marina")
                        .param("perfil", "PROFISSIONAL_HOSPITALAR")
                        .param("senha", "segredo123").param("confirmacao", "segredo123"))
                .andExpect(redirectedUrl("/"));

        mvc.perform(get("/").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Marina Costa (Profissional hospitalar)")))
                .andExpect(content().string(containsString("Painel do hemocentro")));
    }

    @Test
    @DisplayName("Cenário 2: a tela de criação de conta exibe 'Já existe uma conta com o usuário marina'")
    void usuarioExistenteNaTela() throws Exception {
        usuarioRepository.save(new Usuario("Marina Costa", "marina", "segredo123", Perfil.PROFISSIONAL_HOSPITALAR));

        mvc.perform(post("/cadastro")
                        .param("nome", "Outra Pessoa").param("login", "marina").param("perfil", "GESTOR")
                        .param("senha", "outra1234").param("confirmacao", "outra1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Já existe uma conta com o usuário marina")));

        mvc.perform(get("/cadastro/verificar-usuario").param("login", "marina"))
                .andExpect(jsonPath("$.disponivel").value(false))
                .andExpect(jsonPath("$.mensagem").value("Já existe uma conta com o usuário marina"));
    }

    @Test
    @DisplayName("Cenário 6: senha incorreta exibe 'Usuário ou senha inválidos'")
    void senhaIncorretaNaTela() throws Exception {
        usuarioRepository.save(new Usuario("Marina Costa", "marina", "segredo123", Perfil.PROFISSIONAL_HOSPITALAR));

        mvc.perform(post("/login").param("login", "marina").param("senha", "errada999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuário ou senha inválidos")));
    }

    @Test
    @DisplayName("Cenário 7: sem entrar, a tela de estoque leva para a tela de entrada")
    void acessoSemEntrar() throws Exception {
        mvc.perform(get("/bolsas")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/demanda")).andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Cenário 8: depois de Sair as telas voltam a exigir usuário e senha")
    void saida() throws Exception {
        usuarioRepository.save(new Usuario("Marina Costa", "marina", "segredo123", Perfil.PROFISSIONAL_HOSPITALAR));
        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(post("/login").session(sessao).param("login", "marina").param("senha", "segredo123"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(get("/bolsas").session(sessao)).andExpect(status().isOk());

        mvc.perform(post("/logout").session(sessao)).andExpect(redirectedUrl("/login"));

        mvc.perform(get("/bolsas").session(new MockHttpSession())).andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Esqueci minha senha: redefine e entra com a nova senha")
    void esqueciMinhaSenha() throws Exception {
        usuarioRepository.save(new Usuario("Marina Costa", "marina", "segredo123", Perfil.PROFISSIONAL_HOSPITALAR));

        mvc.perform(get("/recuperar-senha")).andExpect(status().isOk());
        mvc.perform(post("/recuperar-senha").param("login", "marina").param("nome", "Marina Costa")
                        .param("senha", "novaSenha9").param("confirmacao", "novaSenha9"))
                .andExpect(redirectedUrl("/login"));

        mvc.perform(post("/login").param("login", "marina").param("senha", "novaSenha9"))
                .andExpect(redirectedUrl("/"));
    }
}
