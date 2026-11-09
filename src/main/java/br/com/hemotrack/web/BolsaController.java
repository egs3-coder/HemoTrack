package br.com.hemotrack.web;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.service.BolsaService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** US03 (entrada de bolsas no estoque) e US10 (descarte de bolsas vencidas). */
@Controller
@RequestMapping("/bolsas")
public class BolsaController {

    private final BolsaService bolsaService;

    public BolsaController(BolsaService bolsaService) {
        this.bolsaService = bolsaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("bolsas", bolsaService.listar());
        model.addAttribute("saldo", bolsaService.saldoPorTipo());
        model.addAttribute("hemocomponentes", Hemocomponente.values());
        model.addAttribute("tipos", TipoSanguineo.values());
        model.addAttribute("hoje", LocalDate.now());
        return "bolsas";
    }

    @PostMapping
    public String cadastrar(@RequestParam(required = false) String codigo,
                            @RequestParam(required = false) Hemocomponente hemocomponente,
                            @RequestParam(required = false) TipoSanguineo tipoSanguineo,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataColeta,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataValidade,
                            RedirectAttributes redirect) {
        Bolsa bolsa = bolsaService.cadastrar(codigo, hemocomponente, tipoSanguineo, dataColeta, dataValidade);
        redirect.addFlashAttribute("sucesso", "Bolsa " + bolsa.getCodigo() + " ("
                + bolsa.getTipoSanguineo().getRotulo() + ") adicionada ao inventário com status "
                + bolsa.getStatus().getRotulo());
        return "redirect:/bolsas";
    }

    @PostMapping("/verificar-validade")
    public String verificarValidade(RedirectAttributes redirect) {
        int descartadas = bolsaService.verificarValidade();
        redirect.addFlashAttribute("sucesso", bolsaService.mensagemDescarte(descartadas));
        return "redirect:/bolsas";
    }
}
