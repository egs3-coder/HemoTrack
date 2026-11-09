package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.service.DemandaService;
import br.com.hemotrack.service.ProjecaoDemanda;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US08 - Projeção de demanda por tipo sanguíneo")
class US08DemandaPreditivaTest {

    private final ProjecaoDemanda oNeg = new ProjecaoDemanda(TipoSanguineo.O_NEG, List.of(10, 14, 12), 8);
    private final ProjecaoDemanda aPos = new ProjecaoDemanda(TipoSanguineo.A_POS, List.of(20, 22, 24), 30);
    private final ProjecaoDemanda abNeg = new ProjecaoDemanda(TipoSanguineo.AB_NEG, List.of(0, 0, 2), 0);

    @Test
    @DisplayName("Cenário 1: Projeção com destaque de risco")
    void projecaoComDestaqueDeRisco() {
        assertEquals("12,0", oNeg.getMediaFormatada());
        assertEquals("1,63", oNeg.getDesvioFormatado());
        assertEquals("22,0", aPos.getMediaFormatada());
        assertEquals("1,63", aPos.getDesvioFormatado());
        assertEquals("Risco de ruptura", oNeg.getSituacao());
        assertTrue(oNeg.isRiscoRuptura());
        assertEquals("Estoque adequado", aPos.getSituacao());
    }

    @Test
    @DisplayName("Cenário 2: Histórico insuficiente")
    void historicoInsuficiente() {
        assertFalse(abNeg.isHistoricoSuficiente());
        assertFalse(abNeg.isRiscoRuptura());
        assertEquals("Histórico insuficiente para projeção (mínimo de 3 meses)", abNeg.getSituacao());
        assertEquals(1, abNeg.getMesesComConsumo());
    }

    @Test
    @DisplayName("Regra: risco quando o estoque é menor que μ + σ (limite exato não é risco)")
    void limiteDeRisco() {
        // mu = 10, sigma = 0 -> estoque 10 nao e menor que 10
        assertEquals("Estoque adequado", new ProjecaoDemanda(TipoSanguineo.B_POS, List.of(10, 10, 10), 10).getSituacao());
        assertEquals("Risco de ruptura", new ProjecaoDemanda(TipoSanguineo.B_POS, List.of(10, 10, 10), 9).getSituacao());
    }

    @Test
    @DisplayName("Indicadores: nível de segurança, bolsas a captar, cobertura, tendência e prioridade")
    void indicadoresComplementares() {
        assertEquals("13,6", oNeg.getNivelSegurancaFormatado());
        assertEquals(6, oNeg.getBolsasACaptar());          // 13,63 - 8 = 5,63 -> 6
        assertEquals(20L, oNeg.getCoberturaDias());         // 8 / 12 * 30
        assertEquals("Alta", oNeg.getTendencia());          // 10 -> 12 (+20%)
        assertEquals("Crítica", oNeg.getPrioridade());      // estoque 8 nao cobre nem a media 12
        assertEquals("Moderada", oNeg.getVariabilidade());  // 1,63 / 12 = 14%

        assertEquals(0, aPos.getBolsasACaptar());
        assertEquals(41L, aPos.getCoberturaDias());         // 30 / 22 * 30
        assertEquals("", aPos.getPrioridade());
        assertEquals("Baixa", aPos.getVariabilidade());     // 1,63 / 22 = 7%

        assertNull(abNeg.getCoberturaDias());
        assertEquals("-", abNeg.getTendencia());

        assertEquals("Estável", new ProjecaoDemanda(TipoSanguineo.B_POS, List.of(10, 12, 10), 30).getTendencia());
        assertEquals("Queda", new ProjecaoDemanda(TipoSanguineo.B_POS, List.of(20, 15, 10), 30).getTendencia());
        assertEquals("", new ProjecaoDemanda(TipoSanguineo.B_POS, List.of(10, 10, 10), 10).getPrioridade());
        assertEquals("Alta", new ProjecaoDemanda(TipoSanguineo.B_POS, List.of(8, 12, 10), 11).getPrioridade());
    }

    @Test
    @DisplayName("Campanhas: tipos em risco ordenados por bolsas a captar")
    void prioridadesDeCampanha() {
        DemandaService service = new DemandaService(mock(BolsaRepository.class));
        ProjecaoDemanda bNeg = new ProjecaoDemanda(TipoSanguineo.B_NEG, List.of(5, 6, 7), 4);   // mu + sigma = 6,8 -> capta 3
        List<ProjecaoDemanda> todas = List.of(bNeg, aPos, oNeg, abNeg);

        List<ProjecaoDemanda> prioridades = service.prioridadesDeCampanha(todas);

        assertEquals(List.of(oNeg, bNeg), prioridades);
        DemandaService.ResumoDemanda resumo = service.resumir(todas);
        assertEquals(2, resumo.getTiposEmRisco());
        assertEquals(1, resumo.getTiposAdequados());
        assertEquals(1, resumo.getTiposSemHistorico());
        assertEquals(40, resumo.getDemandaProjetada());     // 12 + 22 + 6
        assertEquals(9, resumo.getBolsasACaptar());         // 6 + 0 + 3
    }
}
