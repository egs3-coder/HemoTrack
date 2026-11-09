package br.com.hemotrack.web;

import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.service.BolsaService;
import br.com.hemotrack.service.DemandaService;
import br.com.hemotrack.service.RequisicaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Painel do hemocentro (tela exibida apos entrar - US02 cenarios 1 e 5).
 * Mostra o saldo por tipo sanguineo (US03) e uma visao geral da operacao.
 */
@Controller
public class InicioController {

    static final int DIAS_ALERTA_VALIDADE = 3;

    private final BolsaService bolsaService;
    private final RequisicaoService requisicaoService;
    private final DemandaService demandaService;

    public InicioController(BolsaService bolsaService, RequisicaoService requisicaoService,
                            DemandaService demandaService) {
        this.bolsaService = bolsaService;
        this.requisicaoService = requisicaoService;
        this.demandaService = demandaService;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("saldo", bolsaService.saldoPorTipo());
        model.addAttribute("pendentes", requisicaoService.contarPorStatus(StatusRequisicao.PENDENTE));
        model.addAttribute("alocadas", requisicaoService.contarPorStatus(StatusRequisicao.ALOCADA));
        model.addAttribute("emTransporte", requisicaoService.contarPorStatus(StatusRequisicao.EM_TRANSPORTE));
        model.addAttribute("vencendo", bolsaService.vencendoNosProximosDias(DIAS_ALERTA_VALIDADE));
        model.addAttribute("diasAlerta", DIAS_ALERTA_VALIDADE);
        model.addAttribute("emRisco", demandaService.prioridadesDeCampanha(demandaService.projetar()));
        return "index";
    }
}
