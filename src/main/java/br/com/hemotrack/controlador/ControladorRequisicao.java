package br.com.hemotrack.controlador;

import br.com.hemotrack.modelo.*;
import br.com.hemotrack.repositorio.RepositorioHospital;
import br.com.hemotrack.servico.ServicoAlocacao;
import br.com.hemotrack.servico.ServicoRequisicao;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/requisicoes")
public class ControladorRequisicao {

    private final ServicoRequisicao servicoRequisicao;
    private final RepositorioHospital repositorioHospital;
    private final ServicoAlocacao servicoAlocacao;

    public ControladorRequisicao(ServicoRequisicao servicoRequisicao,
                                 RepositorioHospital repositorioHospital,
                                 ServicoAlocacao servicoAlocacao) {
        this.servicoRequisicao = servicoRequisicao;
        this.repositorioHospital = repositorioHospital;
        this.servicoAlocacao = servicoAlocacao;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute(
                "resumosRequisicoes",
                servicoAlocacao.criarResumos(servicoRequisicao.listarTodas()));
        return "requisicoes/lista";
    }

    @GetMapping("/nova")
    public String formulario(Model modelo) {
        prepararFormulario(modelo, new Requisicao());
        return "requisicoes/formulario";
    }

    @PostMapping
    public String cadastrar(@ModelAttribute Requisicao requisicao, Model modelo) {
        try {
            servicoRequisicao.cadastrar(requisicao);
            return "redirect:/requisicoes?sucesso=1";
        } catch (IllegalArgumentException excecao) {
            modelo.addAttribute("erro", excecao.getMessage());
            prepararFormulario(modelo, requisicao);
            return "requisicoes/formulario";
        }
    }

    @PostMapping("/{id}/alocar")
    public String alocar(@PathVariable Long id,
                         @RequestParam(defaultValue = "1") Integer quantidadeAlocar,
                         RedirectAttributes atributosRedirecionamento) {
        try {
            var alocacoes = servicoAlocacao.alocar(id, quantidadeAlocar);
            int quantidadeAlocada = alocacoes.size();

            atributosRedirecionamento.addFlashAttribute(
                    "mensagem",
                    quantidadeAlocada
                            + (quantidadeAlocada == 1
                            ? " bolsa alocada com sucesso."
                            : " bolsas alocadas com sucesso."));

        } catch (RuntimeException excecao) {
            atributosRedirecionamento.addFlashAttribute(
                    "erro",
                    excecao.getMessage());
        }

        return "redirect:/requisicoes";
    }

    @PostMapping("/{id}/remover")
    public String remover(@PathVariable Long id,
                          RedirectAttributes atributosRedirecionamento) {
        try {
            servicoRequisicao.remover(id);
            atributosRedirecionamento.addFlashAttribute(
                    "mensagem",
                    "Requisição removida com sucesso.");
        } catch (RuntimeException excecao) {
            atributosRedirecionamento.addFlashAttribute(
                    "erro",
                    excecao.getMessage());
        }

        return "redirect:/requisicoes";
    }

    private void prepararFormulario(Model modelo, Requisicao requisicao) {
        modelo.addAttribute("requisicao", requisicao);
        modelo.addAttribute("hospitais", repositorioHospital.findAll());
        modelo.addAttribute("tiposSanguineos", TipoSanguineo.values());
        modelo.addAttribute("hemocomponentes", TipoHemocomponente.values());
    }
}
