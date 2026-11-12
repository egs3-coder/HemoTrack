package br.com.hemotrack.service;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * US07 - Painel gerencial de descarte e eficiencia operacional.
 *   - Taxa de descarte = bolsas descartadas por validade / bolsas que sairam do estoque
 *     (entregues + descartadas) no periodo.
 *   - SLA = pedidos entregues dentro do tempo previsto / pedidos entregues no periodo.
 */
@Service
public class RelatorioService {

    private final BolsaRepository bolsaRepository;
    private final RequisicaoRepository requisicaoRepository;

    public RelatorioService(BolsaRepository bolsaRepository, RequisicaoRepository requisicaoRepository) {
        this.bolsaRepository = bolsaRepository;
        this.requisicaoRepository = requisicaoRepository;
    }

    /** US07: periodos disponiveis ("Ultimos 7 dias", "Ultimos 30 dias" e "Ultimos 90 dias"). */
    public static final List<Integer> PERIODOS = List.of(7, 30, 90);

    public Dashboard gerar(int dias) {
        if (!PERIODOS.contains(dias)) {
            throw new RegraNegocioException("Selecione um período válido: Últimos 7, 30 ou 90 dias");
        }
        LocalDate hoje = LocalDate.now();
        // "Ultimos N dias" inclui hoje: de hoje-(N-1) ate hoje.
        LocalDate inicio = hoje.minusDays(dias - 1L);

        long entregues = bolsaRepository.countByDataEntregaBetween(inicio, hoje);
        long descartadas = bolsaRepository.countByMotivoDescarteAndDataDescarteBetween(
                Bolsa.MOTIVO_VALIDADE, inicio, hoje);

        List<Requisicao> pedidos = requisicaoRepository.findByStatusAndDataEntregaBetween(
                StatusRequisicao.ENTREGUE, inicio.atStartOfDay(), LocalDateTime.now());

        long noPrazo = 0;
        Map<String, List<Long>> duracoesPorRegiao = new TreeMap<>();
        for (Requisicao pedido : pedidos) {
            if (pedido.isEntregueNoPrazo()) {
                noPrazo++;
            }
            Long duracao = pedido.getDuracaoEntregaMin();
            if (duracao != null) {
                duracoesPorRegiao.computeIfAbsent(pedido.getHospital().getRegiao(), chave -> new ArrayList<>())
                        .add(duracao);
            }
        }

        Map<String, Long> mediaPorRegiao = new TreeMap<>();
        for (Map.Entry<String, List<Long>> entrada : duracoesPorRegiao.entrySet()) {
            long soma = 0;
            for (long duracao : entrada.getValue()) {
                soma += duracao;
            }
            mediaPorRegiao.put(entrada.getKey(), Math.round((double) soma / entrada.getValue().size()));
        }

        return new Dashboard(dias, entregues, descartadas, pedidos.size(), noPrazo, mediaPorRegiao);
    }

    /**
     * US07: resumo-operacional.csv, separado por ponto e virgula, com periodo, bolsas entregues,
     * bolsas descartadas, taxa de descarte, pedidos entregues, pedidos no prazo, SLA e tempo medio por regiao.
     */
    public String exportarCsv(Dashboard dashboard) {
        StringBuilder csv = new StringBuilder();
        csv.append("Indicador;Valor\n");
        csv.append("Período;").append(dashboard.getRotuloPeriodo()).append('\n');
        csv.append("Bolsas entregues;").append(dashboard.getBolsasEntregues()).append('\n');
        csv.append("Bolsas descartadas por validade;").append(dashboard.getBolsasDescartadas()).append('\n');
        csv.append("Taxa de descarte por validade (%);").append(dashboard.getTaxaDescarte()).append('\n');
        csv.append("Pedidos entregues;").append(dashboard.getPedidosEntregues()).append('\n');
        csv.append("Pedidos entregues no prazo;").append(dashboard.getPedidosNoPrazo()).append('\n');
        csv.append("Atendimento no prazo - SLA (%);").append(dashboard.getSlaFormatado()).append('\n');
        for (Map.Entry<String, Long> entrada : dashboard.getTempoMedioPorRegiao().entrySet()) {
            csv.append("Tempo médio de entrega - ").append(entrada.getKey()).append(" (min);")
                    .append(entrada.getValue()).append('\n');
        }
        return csv.toString();
    }
}
