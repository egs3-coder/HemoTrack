package br.com.hemotrack.api;

import br.com.hemotrack.model.LeituraTemperatura;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.service.RequisicaoService;
import br.com.hemotrack.service.TelemetriaService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US06: API REST usada pelo sensor de telemetria.
 * POST /api/requisicoes/REQ-0001/leituras   corpo: {"temperatura": 4.2}
 * GET  /api/requisicoes/REQ-0001/leituras
 */
@RestController
@RequestMapping("/api/requisicoes/{codigo}/leituras")
public class TelemetriaApiController {

    private final RequisicaoService requisicaoService;
    private final TelemetriaService telemetriaService;

    public TelemetriaApiController(RequisicaoService requisicaoService, TelemetriaService telemetriaService) {
        this.requisicaoService = requisicaoService;
        this.telemetriaService = telemetriaService;
    }

    @PostMapping
    public Map<String, Object> registrar(@PathVariable String codigo, @RequestBody Map<String, Double> corpo) {
        Requisicao requisicao = requisicaoService.buscarPorCodigo(codigo);
        LeituraTemperatura leitura = telemetriaService.registrar(requisicao, corpo.get("temperatura"));
        Map<String, Object> resposta = paraMapa(leitura);
        resposta.put("statusConservacao", requisicao.getStatusConservacao().getRotulo());
        if (leitura.isForaDaFaixa()) {
            resposta.put("alerta", telemetriaService.mensagemAlerta(requisicao, leitura.getTemperatura()));
        }
        return resposta;
    }

    @GetMapping
    public List<Map<String, Object>> historico(@PathVariable String codigo) {
        Requisicao requisicao = requisicaoService.buscarPorCodigo(codigo);
        List<Map<String, Object>> lista = new ArrayList<>();
        for (LeituraTemperatura leitura : telemetriaService.historico(requisicao)) {
            lista.add(paraMapa(leitura));
        }
        return lista;
    }

    private Map<String, Object> paraMapa(LeituraTemperatura leitura) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("temperatura", leitura.getTemperatura());
        mapa.put("dataHora", leitura.getDataHora().toString());
        mapa.put("foraDaFaixa", leitura.isForaDaFaixa());
        return mapa;
    }
}
