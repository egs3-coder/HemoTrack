package br.com.hemotrack.web;

import br.com.hemotrack.service.HospitalService;
import br.com.hemotrack.service.RotaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** US05: manutencao da malha de trechos. */
@Controller
@RequestMapping("/trechos")
public class TrechoController {

    private final RotaService rotaService;
    private final HospitalService hospitalService;

    public TrechoController(RotaService rotaService, HospitalService hospitalService) {
        this.rotaService = rotaService;
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("trechos", rotaService.listarTrechos());
        model.addAttribute("hospitais", hospitalService.listar());
        model.addAttribute("hemocentro", RotaService.HEMOCENTRO);
        return "trechos";
    }

    @PostMapping
    public String cadastrar(@RequestParam(required = false) String origem,
                            @RequestParam(required = false) String destino,
                            @RequestParam(required = false) Integer tempoMin,
                            @RequestParam(required = false) Double distanciaKm,
                            RedirectAttributes redirect) {
        rotaService.cadastrarTrecho(origem, destino, tempoMin, distanciaKm);
        redirect.addFlashAttribute("sucesso", "Trecho cadastrado");
        return "redirect:/trechos";
    }

    @PostMapping("/{id}/remover")
    public String remover(@PathVariable Long id, RedirectAttributes redirect) {
        rotaService.removerTrecho(id);
        redirect.addFlashAttribute("sucesso", "Trecho removido");
        return "redirect:/trechos";
    }
}
