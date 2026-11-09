package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.Rota;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.service.Dashboard;
import br.com.hemotrack.service.RelatorioService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US07 - Painel gerencial de descarte e eficiência")
class US07PainelGerencialTest {

    private Requisicao entregue(Hospital hospital, int previstoMin, int duracaoMin) {
        Requisicao r = new Requisicao(hospital, br.com.hemotrack.model.Hemocomponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.AB_POS, 1, br.com.hemotrack.model.Urgencia.URGENTE);
        r.marcarAlocada();
        r.registrarRota(new Rota(List.of("Hemocentro Central", hospital.getNome()), previstoMin, 5));
        LocalDateTime despacho = LocalDateTime.now().minusDays(10);
        r.despachar(despacho);
        r.confirmarRecebimento(despacho.plusMinutes(duracaoMin));
        return r;
    }

    @Test
    @DisplayName("Cenário 1: Indicadores dos últimos 30 dias")
    void indicadoresDosUltimos30Dias() {
        BolsaRepository bolsas = mock(BolsaRepository.class);
        RequisicaoRepository requisicoes = mock(RequisicaoRepository.class);
        when(bolsas.countByDataEntregaBetween(any(), any())).thenReturn(36L);
        when(bolsas.countByMotivoDescarteAndDataDescarteBetween(eq(Bolsa.MOTIVO_VALIDADE), any(), any())).thenReturn(4L);

        // 20 pedidos entregues, 18 dentro do tempo previsto; Norte 20/24 min e Sul 30/40 min
        List<Requisicao> pedidos = new ArrayList<>();
        Hospital norte = Fixtures.santaClara();
        Hospital sul = Fixtures.boaVista();
        for (int i = 0; i < 5; i++) {
            pedidos.add(entregue(norte, 25, 20));
            pedidos.add(entregue(norte, 25, 24));
            pedidos.add(entregue(sul, 40, 30));
            pedidos.add(entregue(sul, i < 2 ? 35 : 40, 40));
        }
        when(requisicoes.findByStatusAndDataEntregaBetween(eq(StatusRequisicao.ENTREGUE), any(), any()))
                .thenReturn(pedidos);

        Dashboard d = new RelatorioService(bolsas, requisicoes).gerar(30);

        assertEquals(10, d.getTaxaDescarte());
        assertEquals(90, d.getSla());
        assertEquals(22L, d.getTempoMedioPorRegiao().get("Norte"));
        assertEquals(35L, d.getTempoMedioPorRegiao().get("Sul"));
        assertFalse(d.isVazio());
        String csv = new RelatorioService(bolsas, requisicoes).exportarCsv(d);
        assertTrue(csv.contains("Taxa de descarte por validade (%);10"));
        assertTrue(csv.contains("Atendimento no prazo - SLA (%);90"));
    }

    @Test
    @DisplayName("Cenário 2: Período sem movimentação")
    void periodoSemMovimentacao() {
        Dashboard d = new RelatorioService(mock(BolsaRepository.class), mock(RequisicaoRepository.class)).gerar(7);

        assertTrue(d.isVazio());
    }
}
