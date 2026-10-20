package br.com.hemotrack.web;

import br.com.hemotrack.service.DemandaService;
import br.com.hemotrack.service.ProjecaoDemanda;
import br.com.hemotrack.service.RelatorioService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** US07 (painel gerencial) e US08 (demanda preditiva). */
@Controller
public class RelatorioController {

    private final RelatorioService relatorioService;
    private final DemandaService demandaService;

    public RelatorioController(RelatorioService relatorioService, DemandaService demandaService) {
        this.relatorioService = relatorioService;
        this.demandaService = demandaService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Integer dias, Model model) {
        model.addAttribute("dias", dias == null ? 30 : dias);
        if (dias != null) {
            model.addAttribute("d", relatorioService.gerar(dias));
        }
        return "dashboard";
    }

    @GetMapping("/dashboard/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(defaultValue = "30") int dias) {
        String csv = relatorioService.exportarCsv(relatorioService.gerar(dias));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resumo-operacional.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/demanda")
    public String demanda(Model model) {
        List<ProjecaoDemanda> projecoes = demandaService.projetar();
        model.addAttribute("projecoes", projecoes);
        model.addAttribute("prioridades", demandaService.prioridadesDeCampanha(projecoes));
        model.addAttribute("resumo", demandaService.resumir(projecoes));
        model.addAttribute("meses", demandaService.mesesAnalisados());
        model.addAttribute("maior", demandaService.maiorMedia(projecoes));
        return "demanda";
    }
}
