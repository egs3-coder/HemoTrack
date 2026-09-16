package br.com.hemotrack.controlador;

import br.com.hemotrack.modelo.BolsaSangue;
import br.com.hemotrack.modelo.TipoHemocomponente;
import br.com.hemotrack.modelo.TipoSanguineo;
import br.com.hemotrack.servico.ServicoBolsa;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bolsas")
public class ControladorBolsa {

    private final ServicoBolsa servicoBolsa;

    public ControladorBolsa(ServicoBolsa servicoBolsa) {
        this.servicoBolsa = servicoBolsa;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("bolsas", servicoBolsa.listarTodas());
        return "bolsas/lista";
    }

    @GetMapping("/nova")
    public String formulario(Model modelo) {
        prepararFormulario(modelo, new BolsaSangue());
        return "bolsas/formulario";
    }

    @PostMapping
    public String cadastrar(@ModelAttribute BolsaSangue bolsa,
                            Model modelo,
                            RedirectAttributes atributosRedirecionamento) {
        try {
            var bolsasCadastradas = servicoBolsa.cadastrarLote(bolsa);
            int quantidadeCadastrada = bolsasCadastradas.size();

            atributosRedirecionamento.addFlashAttribute(
                    "mensagem",
                    quantidadeCadastrada
                            + (quantidadeCadastrada == 1
                            ? " bolsa cadastrada com sucesso."
                            : " bolsas cadastradas com sucesso."));

            return "redirect:/bolsas";
        } catch (IllegalArgumentException excecao) {
            modelo.addAttribute("erro", excecao.getMessage());
            prepararFormulario(modelo, bolsa);
            return "bolsas/formulario";
        }
    }

    @PostMapping("/{id}/remover")
    public String remover(@PathVariable Long id,
                          RedirectAttributes atributosRedirecionamento) {
        try {
            servicoBolsa.remover(id);
            atributosRedirecionamento.addFlashAttribute(
                    "mensagem",
                    "Bolsa removida com sucesso.");
        } catch (RuntimeException excecao) {
            atributosRedirecionamento.addFlashAttribute(
                    "erro",
                    excecao.getMessage());
        }

        return "redirect:/bolsas";
    }

    private void prepararFormulario(Model modelo, BolsaSangue bolsa) {
        modelo.addAttribute("bolsa", bolsa);
        modelo.addAttribute("tiposSanguineos", TipoSanguineo.values());
        modelo.addAttribute("hemocomponentes", TipoHemocomponente.values());
    }
}
