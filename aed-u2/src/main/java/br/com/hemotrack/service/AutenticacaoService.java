package br.com.hemotrack.service;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Perfil;
import br.com.hemotrack.model.Usuario;
import br.com.hemotrack.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US02 - Criacao de conta e acesso a plataforma.
 *
 * Regras de negocio (historias.md):
 * - Nome, usuario, perfil, senha e confirmacao da senha sao obrigatorios para criar a conta.
 * - O usuario e unico, tem pelo menos 3 caracteres e nao contem espacos; a senha tem pelo menos 6 caracteres.
 * - Perfis: "Profissional hospitalar", "Operador do hemocentro", "Coordenador de logistica", "Gestor do hemocentro".
 * - Quem esqueceu a senha pode redefini-la confirmando usuario e nome completo da conta.
 */
@Service
public class AutenticacaoService {

    public static final int TAMANHO_MINIMO_USUARIO = 3;
    public static final int TAMANHO_MINIMO_SENHA = 6;

    static final String MSG_LOGIN_INVALIDO = "Usuário ou senha inválidos";
    static final String MSG_SENHA_CURTA = "A senha deve ter pelo menos 6 caracteres";
    static final String MSG_CONFIRMACAO = "A confirmação não confere com a senha";
    static final String MSG_RECUPERACAO_NAO_CONFERE = "Não encontramos uma conta com este usuário e nome completo";

    private final UsuarioRepository usuarioRepository;

    public AutenticacaoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Cenarios 5 e 6: entrada com usuario e senha. */
    public Usuario autenticar(String login, String senha) {
        Usuario usuario = usuarioRepository.findByLogin(limparLogin(login)).orElse(null);
        if (usuario == null || !usuario.senhaConfere(senha)) {
            throw new RegraNegocioException(MSG_LOGIN_INVALIDO);
        }
        return usuario;
    }

    /** Cenarios 1 a 4: criacao de conta pela tela "Criar conta". */
    @Transactional
    public Usuario cadastrar(String nome, String login, String senha, String confirmacao, Perfil perfil) {
        if (vazio(nome)) {
            throw new RegraNegocioException("O nome é obrigatório");
        }
        String loginLimpo = validarFormatoUsuario(login);
        // Verificado antes das senhas para avisar o quanto antes que o usuario ja esta em uso (cenario 2).
        if (usuarioRepository.existsByLogin(loginLimpo)) {
            throw new RegraNegocioException(mensagemUsuarioExistente(loginLimpo));
        }
        if (perfil == null) {
            throw new RegraNegocioException("O perfil é obrigatório");
        }
        validarNovaSenha(senha, confirmacao);
        return usuarioRepository.save(new Usuario(nome.trim().replaceAll("\\s+", " "), loginLimpo, senha, perfil));
    }

    /**
     * Consulta usada pela tela de criacao de conta para avisar, enquanto a pessoa digita,
     * que o usuario ja existe no banco de dados.
     *
     * @return null quando o usuario pode ser usado; caso contrario, a mensagem a exibir
     */
    public String verificarUsuario(String login) {
        try {
            String loginLimpo = validarFormatoUsuario(login);
            return usuarioRepository.existsByLogin(loginLimpo) ? mensagemUsuarioExistente(loginLimpo) : null;
        } catch (RegraNegocioException erro) {
            return erro.getMessage();
        }
    }

    /** "Esqueci minha senha": confere usuario + nome completo e grava a nova senha. */
    @Transactional
    public Usuario redefinirSenha(String login, String nome, String novaSenha, String confirmacao) {
        if (vazio(login)) {
            throw new RegraNegocioException("O usuário é obrigatório");
        }
        if (vazio(nome)) {
            throw new RegraNegocioException("O nome completo é obrigatório");
        }
        Usuario usuario = usuarioRepository.findByLogin(limparLogin(login)).orElse(null);
        // Mensagem unica para nao revelar qual dos dois dados esta errado.
        if (usuario == null || !usuario.nomeConfere(nome)) {
            throw new RegraNegocioException(MSG_RECUPERACAO_NAO_CONFERE);
        }
        validarNovaSenha(novaSenha, confirmacao);
        if (usuario.senhaConfere(novaSenha)) {
            throw new RegraNegocioException("A nova senha deve ser diferente da senha atual");
        }
        usuario.alterarSenha(novaSenha);
        return usuarioRepository.save(usuario);
    }

    public static String mensagemUsuarioExistente(String login) {
        return "Já existe uma conta com o usuário " + login;
    }

    private String validarFormatoUsuario(String login) {
        if (vazio(login)) {
            throw new RegraNegocioException("O usuário é obrigatório");
        }
        String loginLimpo = limparLogin(login);
        if (loginLimpo.matches(".*\\s.*")) {
            throw new RegraNegocioException("O usuário não pode conter espaços");
        }
        if (loginLimpo.length() < TAMANHO_MINIMO_USUARIO) {
            throw new RegraNegocioException("O usuário deve ter pelo menos 3 caracteres");
        }
        return loginLimpo;
    }

    private void validarNovaSenha(String senha, String confirmacao) {
        if (vazio(senha)) {
            throw new RegraNegocioException("A senha é obrigatória");
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new RegraNegocioException(MSG_SENHA_CURTA);
        }
        if (vazio(confirmacao)) {
            throw new RegraNegocioException("A confirmação da senha é obrigatória");
        }
        if (!senha.equals(confirmacao)) {
            throw new RegraNegocioException(MSG_CONFIRMACAO);
        }
    }

    private static String limparLogin(String login) {
        return login == null ? "" : login.trim().toLowerCase();
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
