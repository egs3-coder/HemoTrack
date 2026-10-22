package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Perfil;
import br.com.hemotrack.model.Usuario;
import br.com.hemotrack.repository.UsuarioRepository;
import br.com.hemotrack.service.AutenticacaoService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cenarios 1 a 6 e "Esqueci minha senha". Os cenarios 7 e 8 (telas) estao em US02TelasTest. */
@DisplayName("US02 - Criação de conta e acesso à plataforma")
class US02ContaEAcessoTest {

    private UsuarioRepository repositorio;
    private AutenticacaoService service;
    private Usuario marina;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        service = new AutenticacaoService(repositorio);
        when(repositorio.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
        marina = new Usuario("Marina Costa", "marina", "segredo123", Perfil.PROFISSIONAL_HOSPITALAR);
    }

    private void contaMarinaExiste() {
        when(repositorio.existsByLogin("marina")).thenReturn(true);
        when(repositorio.findByLogin("marina")).thenReturn(Optional.of(marina));
    }

    @Test
    @DisplayName("Cenário 1: Criação de conta com sucesso")
    void criacaoDeContaComSucesso() {
        Usuario conta = service.cadastrar("Marina Costa", "marina", "segredo123", "segredo123",
                Perfil.PROFISSIONAL_HOSPITALAR);

        assertEquals("Marina Costa (Profissional hospitalar)", conta.getNome() + " (" + conta.getPerfil().getRotulo() + ")");
        assertTrue(conta.senhaConfere("segredo123"));
    }

    @Test
    @DisplayName("Cenário 2: Usuário já cadastrado")
    void usuarioJaCadastrado() {
        contaMarinaExiste();

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina Souza", "marina", "outra1234", "outra1234", Perfil.GESTOR));

        assertEquals("Já existe uma conta com o usuário marina", erro.getMessage());
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Cenário 2 (aviso antecipado): a tela avisa enquanto a pessoa digita")
    void avisoDeUsuarioExistenteAoDigitar() {
        contaMarinaExiste();

        assertEquals("Já existe uma conta com o usuário marina", service.verificarUsuario("Marina "));
        assertNull(service.verificarUsuario("joana"));
        assertEquals("O usuário deve ter pelo menos 3 caracteres", service.verificarUsuario("jo"));
        assertEquals("O usuário não pode conter espaços", service.verificarUsuario("jo ana"));
    }

    @Test
    @DisplayName("Cenário 2 (prioridade): usuário existente é avisado mesmo com senha inválida")
    void usuarioExistenteAvisadoAntesDaSenha() {
        contaMarinaExiste();

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina Souza", "marina", "123", "123", Perfil.GESTOR));

        assertEquals("Já existe uma conta com o usuário marina", erro.getMessage());
    }

    @Test
    @DisplayName("Cenário 3: Senha curta")
    void senhaCurta() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina Costa", "marina", "12345", "12345", Perfil.PROFISSIONAL_HOSPITALAR));

        assertEquals("A senha deve ter pelo menos 6 caracteres", erro.getMessage());
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Cenário 4: Confirmação diferente da senha")
    void confirmacaoDiferente() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina Costa", "marina", "segredo123", "segredo124",
                        Perfil.PROFISSIONAL_HOSPITALAR));

        assertEquals("A confirmação não confere com a senha", erro.getMessage());
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Regra: nome, usuário, perfil, senha e confirmação são obrigatórios")
    void camposObrigatorios() {
        assertEquals("O nome é obrigatório", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar(" ", "marina", "segredo123", "segredo123", Perfil.GESTOR)).getMessage());
        assertEquals("O usuário é obrigatório", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina", "", "segredo123", "segredo123", Perfil.GESTOR)).getMessage());
        assertEquals("O perfil é obrigatório", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina", "marina", "segredo123", "segredo123", null)).getMessage());
        assertEquals("A senha é obrigatória", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina", "marina", "", "", Perfil.GESTOR)).getMessage());
        assertEquals("A confirmação da senha é obrigatória", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina", "marina", "segredo123", "", Perfil.GESTOR)).getMessage());
    }

    @Test
    @DisplayName("Regra: usuário com pelo menos 3 caracteres e sem espaços")
    void formatoDoUsuario() {
        assertEquals("O usuário deve ter pelo menos 3 caracteres", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina", "ma", "segredo123", "segredo123", Perfil.GESTOR)).getMessage());
        assertEquals("O usuário não pode conter espaços", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Marina", "ma rina", "segredo123", "segredo123", Perfil.GESTOR)).getMessage());
    }

    @Test
    @DisplayName("Regra: os 4 perfis disponíveis")
    void perfisDisponiveis() {
        assertEquals(4, Perfil.values().length);
        assertEquals("Profissional hospitalar", Perfil.PROFISSIONAL_HOSPITALAR.getRotulo());
        assertEquals("Operador do hemocentro", Perfil.OPERADOR_HEMOCENTRO.getRotulo());
        assertEquals("Coordenador de logística", Perfil.COORDENADOR_LOGISTICA.getRotulo());
        assertEquals("Gestor do hemocentro", Perfil.GESTOR.getRotulo());
    }

    @Test
    @DisplayName("Cenário 5: Entrada com usuário e senha corretos")
    void entradaComSucesso() {
        contaMarinaExiste();

        Usuario usuario = service.autenticar("marina", "segredo123");

        assertEquals("Marina Costa (Profissional hospitalar)", usuario.getNome() + " (" + usuario.getPerfil().getRotulo() + ")");
    }

    @Test
    @DisplayName("Cenário 6: Entrada com senha incorreta")
    void entradaComSenhaIncorreta() {
        contaMarinaExiste();

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.autenticar("marina", "errada999"));

        assertEquals("Usuário ou senha inválidos", erro.getMessage());
    }

    @Test
    @DisplayName("Esqueci minha senha: redefinição com usuário e nome completo corretos")
    void redefinicaoDeSenha() {
        contaMarinaExiste();

        service.redefinirSenha("marina", "marina  costa", "novaSenha9", "novaSenha9");

        assertTrue(marina.senhaConfere("novaSenha9"));
        assertFalse(marina.senhaConfere("segredo123"));
        assertEquals(marina, service.autenticar("marina", "novaSenha9"));
    }

    @Test
    @DisplayName("Esqueci minha senha: dados que não conferem não alteram a senha")
    void redefinicaoComDadosErrados() {
        contaMarinaExiste();

        RegraNegocioException nomeErrado = assertThrows(RegraNegocioException.class,
                () -> service.redefinirSenha("marina", "Marina Souza", "novaSenha9", "novaSenha9"));
        RegraNegocioException usuarioInexistente = assertThrows(RegraNegocioException.class,
                () -> service.redefinirSenha("joana", "Joana", "novaSenha9", "novaSenha9"));

        assertEquals("Não encontramos uma conta com este usuário e nome completo", nomeErrado.getMessage());
        assertEquals("Não encontramos uma conta com este usuário e nome completo", usuarioInexistente.getMessage());
        assertTrue(marina.senhaConfere("segredo123"));
    }

    @Test
    @DisplayName("Esqueci minha senha: nova senha segue as mesmas regras da criação de conta")
    void redefinicaoSegueRegrasDeSenha() {
        contaMarinaExiste();

        assertEquals("A senha deve ter pelo menos 6 caracteres", assertThrows(RegraNegocioException.class,
                () -> service.redefinirSenha("marina", "Marina Costa", "12345", "12345")).getMessage());
        assertEquals("A confirmação não confere com a senha", assertThrows(RegraNegocioException.class,
                () -> service.redefinirSenha("marina", "Marina Costa", "novaSenha9", "novaSenha8")).getMessage());
        assertEquals("A nova senha deve ser diferente da senha atual", assertThrows(RegraNegocioException.class,
                () -> service.redefinirSenha("marina", "Marina Costa", "segredo123", "segredo123")).getMessage());
        assertTrue(marina.senhaConfere("segredo123"));
    }
}
