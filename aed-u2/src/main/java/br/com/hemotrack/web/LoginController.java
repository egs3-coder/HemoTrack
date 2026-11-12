package br.com.hemotrack.web;

import br.com.hemotrack.config.AutenticacaoInterceptor;
import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Perfil;
import br.com.hemotrack.model.Usuario;
import br.com.hemotrack.service.AutenticacaoService;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * US02 - Criacao de conta e acesso a plataforma.
 * Em caso de erro a propria tela e exibida de novo com a mensagem e com o que a pessoa ja digitou
 * (as senhas nunca sao devolvidas).
 */
@Controller
public class LoginController {

    private final AutenticacaoService autenticacaoService;

    public LoginController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    // ---------- Entrada (cenarios 5, 6 e 7) ----------

    @GetMapping("/login")
    public String tela(HttpSession sessao) {
        if (sessao.getAttribute(AutenticacaoInterceptor.ATRIBUTO_NOME) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String entrar(@RequestParam(required = false) String login,
                         @RequestParam(required = false) String senha,
                         HttpSession sessao, Model model) {
        try {
            iniciarSessao(sessao, autenticacaoService.autenticar(login, senha));
            return "redirect:/";
        } catch (RegraNegocioException erro) {
            model.addAttribute("erro", erro.getMessage());
            model.addAttribute("login", login);
            return "login";
        }
    }

    // ---------- Criacao de conta (cenarios 1 a 4) ----------

    @GetMapping("/cadastro")
    public String telaCadastro(Model model) {
        model.addAttribute("perfis", Perfil.values());
        return "cadastro";
    }

    /** Cria a conta e ja entra na plataforma com ela (cenario 1). */
    @PostMapping("/cadastro")
    public String cadastrar(@RequestParam(required = false) String nome,
                            @RequestParam(required = false) String login,
                            @RequestParam(required = false) String senha,
                            @RequestParam(required = false) String confirmacao,
                            @RequestParam(required = false) Perfil perfil,
                            HttpSession sessao, Model model) {
        try {
            iniciarSessao(sessao, autenticacaoService.cadastrar(nome, login, senha, confirmacao, perfil));
            return "redirect:/";
        } catch (RegraNegocioException erro) {
            model.addAttribute("erro", erro.getMessage());
            model.addAttribute("perfis", Perfil.values());
            model.addAttribute("nome", nome);
            model.addAttribute("login", login);
            model.addAttribute("perfilSelecionado", perfil);
            return "cadastro";
        }
    }

    /**
     * Cenario 2 antecipado: a tela de criacao de conta consulta este endereco enquanto a pessoa digita
     * e avisa na hora "Ja existe uma conta com o usuario ...". A validacao definitiva continua no envio.
     */
    @GetMapping("/cadastro/verificar-usuario")
    @ResponseBody
    public Map<String, Object> verificarUsuario(@RequestParam(required = false) String login) {
        String mensagem = autenticacaoService.verificarUsuario(login);
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("disponivel", mensagem == null);
        resposta.put("mensagem", mensagem == null ? "Usuário disponível" : mensagem);
        return resposta;
    }

    // ---------- Esqueci minha senha ----------

    @GetMapping("/recuperar-senha")
    public String telaRecuperarSenha() {
        return "recuperar-senha";
    }

    @PostMapping("/recuperar-senha")
    public String recuperarSenha(@RequestParam(required = false) String login,
                                 @RequestParam(required = false) String nome,
                                 @RequestParam(required = false) String senha,
                                 @RequestParam(required = false) String confirmacao,
                                 Model model, RedirectAttributes redirect) {
        try {
            Usuario usuario = autenticacaoService.redefinirSenha(login, nome, senha, confirmacao);
            redirect.addFlashAttribute("sucesso", "Senha redefinida. Entre com o usuário "
                    + usuario.getLogin() + " e a nova senha");
            redirect.addFlashAttribute("login", usuario.getLogin());
            return "redirect:/login";
        } catch (RegraNegocioException erro) {
            model.addAttribute("erro", erro.getMessage());
            model.addAttribute("login", login);
            model.addAttribute("nome", nome);
            return "recuperar-senha";
        }
    }

    // ---------- Saida (cenario 8) ----------

    @PostMapping("/logout")
    public String sair(HttpSession sessao) {
        sessao.invalidate();
        return "redirect:/login";
    }

    private void iniciarSessao(HttpSession sessao, Usuario usuario) {
        sessao.setAttribute(AutenticacaoInterceptor.ATRIBUTO_NOME, usuario.getNome());
        sessao.setAttribute(AutenticacaoInterceptor.ATRIBUTO_PERFIL, usuario.getPerfil().getRotulo());
    }
}
