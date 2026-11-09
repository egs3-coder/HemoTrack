package br.com.hemotrack.web;

import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.service.HospitalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** US09. */
@Controller
@RequestMapping("/hospitais")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("hospitais", hospitalService.listar());
        return "hospitais";
    }

    @PostMapping
    public String cadastrar(@RequestParam(required = false) String nome,
                            @RequestParam(required = false) String cnpj,
                            @RequestParam(required = false) String regiao,
                            RedirectAttributes redirect) {
        Hospital hospital = hospitalService.cadastrar(nome, cnpj, regiao);
        redirect.addFlashAttribute("sucesso", hospital.getNome() + " cadastrado com situação Ativo");
        return "redirect:/hospitais";
    }

    @PostMapping("/{id}/situacao")
    public String alternarSituacao(@PathVariable Long id, RedirectAttributes redirect) {
        Hospital hospital = hospitalService.alternarSituacao(id);
        redirect.addFlashAttribute("sucesso", hospital.getNome() + " agora está " + hospital.getSituacao());
        return "redirect:/hospitais";
    }
}
