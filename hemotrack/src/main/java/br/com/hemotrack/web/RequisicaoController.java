package br.com.hemotrack.web;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.LeituraTemperatura;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.model.Urgencia;
import br.com.hemotrack.service.HospitalService;
import br.com.hemotrack.service.RequisicaoService;
import br.com.hemotrack.service.TelemetriaService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** US01 (requisicao), US04 (alocacao), US05 (rota), US06 (temperatura) e US11 (acompanhamento). */
@Controller
@RequestMapping("/requisicoes")
public class RequisicaoController {

    private final RequisicaoService requisicaoService;
    private final HospitalService hospitalService;
    private final TelemetriaService telemetriaService;

    public RequisicaoController(RequisicaoService requisicaoService, HospitalService hospitalService,
                                TelemetriaService telemetriaService) {
        this.requisicaoService = requisicaoService;
        this.hospitalService = hospitalService;
        this.telemetriaService = telemetriaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("requisicoes", requisicaoService.listar());
        return "requisicoes";
    }

    @GetMapping("/nova")
    public String formulario(Model model) {
        prepararFormulario(model);
        model.addAttribute("quantidade", 1);
        return "requisicao-form";
    }

    /**
     * US01: em caso de erro (cenarios 2 e 3) a requisicao NAO e registrada e o formulario volta
     * com a mensagem e com os dados ja preenchidos.
     */
    @PostMapping
    public String criar(@RequestParam(required = false) Long hospitalId,
                        @RequestParam(required = false) Hemocomponente hemocomponente,
                        @RequestParam(required = false) TipoSanguineo tipoSanguineo,
                        @RequestParam(required = false) Integer quantidade,
                        @RequestParam(required = false) Urgencia urgencia,
                        Model model, RedirectAttributes redirect) {
        try {
            Requisicao requisicao = requisicaoService.criar(hospitalId, hemocomponente, tipoSanguineo,
                    quantidade, urgencia);
            redirect.addFlashAttribute("sucesso", "Requisição " + requisicao.getCodigo()
                    + " cadastrada com status " + requisicao.getStatus().getRotulo());
            return "redirect:/requisicoes/" + requisicao.getId();
        } catch (RegraNegocioException erro) {
            prepararFormulario(model);
            model.addAttribute("erro", erro.getMessage());
            model.addAttribute("hospitalId", hospitalId);
            model.addAttribute("hemocomponente", hemocomponente);
            model.addAttribute("tipoSanguineo", tipoSanguineo);
            model.addAttribute("quantidade", quantidade);
            model.addAttribute("urgencia", urgencia);
            return "requisicao-form";
        }
    }

    private void prepararFormulario(Model model) {
        model.addAttribute("hospitais", hospitalService.listarAtivos());
        model.addAttribute("hemocomponentes", Hemocomponente.values());
        model.addAttribute("tipos", TipoSanguineo.values());
        model.addAttribute("urgencias", Urgencia.values());
    }

    /** US11 cenarios 1 e 4: consulta pelo identificador. */
    @GetMapping("/consulta")
    public String consultar(@RequestParam(required = false) String codigo) {
        Requisicao requisicao = requisicaoService.buscarPorCodigo(codigo);
        return "redirect:/requisicoes/" + requisicao.getId();
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Requisicao requisicao = requisicaoService.buscar(id);
        model.addAttribute("r", requisicao);
        model.addAttribute("etapas", StatusRequisicao.values());
        model.addAttribute("bolsas", requisicaoService.bolsasDa(requisicao));
        List<LeituraTemperatura> leituras = telemetriaService.historico(requisicao);
        model.addAttribute("leituras", leituras);
        // US06 cenario 2: as leituras tambem chegam pelo sensor (API); se a ultima esta fora da faixa,
        // a tela do pedido exibe o alerta "Temperatura de 8,5 °C fora da faixa segura (2,0 °C a 6,0 °C)".
        if (!leituras.isEmpty() && leituras.get(0).isForaDaFaixa()) {
            model.addAttribute("alertaTemperatura",
                    telemetriaService.mensagemAlerta(requisicao, leituras.get(0).getTemperatura()));
        }
        return "requisicao-detalhe";
    }

    @PostMapping("/{id}/alocar")
    public String alocar(@PathVariable Long id, RedirectAttributes redirect) {
        Requisicao requisicao = requisicaoService.alocar(id);
        List<String> codigos = new ArrayList<>();
        for (Bolsa bolsa : requisicaoService.bolsasDa(requisicao)) {
            codigos.add(bolsa.getCodigo());
        }
        redirect.addFlashAttribute("sucesso", (codigos.size() == 1 ? "Bolsa " : "Bolsas ")
                + String.join(", ", codigos) + " selecionada" + (codigos.size() == 1 ? "" : "s")
                + " e reservada" + (codigos.size() == 1 ? "" : "s") + " para despacho. Requisição Alocada");
        return "redirect:/requisicoes/" + id;
    }

    @PostMapping("/{id}/rota")
    public String gerarRota(@PathVariable Long id, RedirectAttributes redirect) {
        Requisicao requisicao = requisicaoService.gerarRota(id);
        redirect.addFlashAttribute("sucesso", "Rota gerada: " + requisicao.getItinerario() + " ("
                + requisicao.getDistanciaFormatada() + " km, " + requisicao.getTempoPrevistoMin() + " min)");
        return "redirect:/requisicoes/" + id;
    }

    @PostMapping("/{id}/despachar")
    public String despachar(@PathVariable Long id, RedirectAttributes redirect) {
        requisicaoService.despachar(id);
        redirect.addFlashAttribute("sucesso", "Pedido despachado. Status: Em Transporte");
        return "redirect:/requisicoes/" + id;
    }

    @PostMapping("/{id}/leituras")
    public String registrarLeitura(@PathVariable Long id, @RequestParam(required = false) Double temperatura,
                                   RedirectAttributes redirect) {
        Requisicao requisicao = requisicaoService.buscar(id);
        LeituraTemperatura leitura = telemetriaService.registrar(requisicao, temperatura);
        // Leitura fora da faixa: o alerta e exibido pela propria tela do pedido (ver detalhe()),
        // tanto para leituras da tela quanto para as enviadas pelo sensor.
        if (!leitura.isForaDaFaixa()) {
            redirect.addFlashAttribute("sucesso", "Leitura de " + leitura.getTemperaturaFormatada() + " registrada");
        }
        return "redirect:/requisicoes/" + id;
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Long id, RedirectAttributes redirect) {
        requisicaoService.confirmarRecebimento(id);
        redirect.addFlashAttribute("sucesso", "Recebimento confirmado. Status: Entregue");
        return "redirect:/requisicoes/" + id;
    }
}
