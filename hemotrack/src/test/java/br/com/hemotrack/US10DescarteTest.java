package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusBolsa;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.service.BolsaService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US10 - Descarte de bolsas vencidas")
class US10DescarteTest {

    private final LocalDate hoje = LocalDate.now();
    private final List<Bolsa> estoque = new ArrayList<>();
    private BolsaService service;

    @BeforeEach
    void preparar() {
        BolsaRepository bolsaRepository = mock(BolsaRepository.class);
        service = new BolsaService(bolsaRepository, mock(RequisicaoRepository.class));
        when(bolsaRepository.save(any(Bolsa.class))).thenAnswer(chamada -> chamada.getArgument(0));
        // Simula a consulta do banco: status informados e validade anterior a hoje
        when(bolsaRepository.findByStatusInAndDataValidadeBefore(any(), any())).thenAnswer(chamada -> {
            Collection<StatusBolsa> status = chamada.getArgument(0);
            LocalDate data = chamada.getArgument(1);
            List<Bolsa> vencidas = new ArrayList<>();
            for (Bolsa b : estoque) {
                if (status.contains(b.getStatus()) && b.getDataValidade().isBefore(data)) {
                    vencidas.add(b);
                }
            }
            return vencidas;
        });
        when(bolsaRepository.findByRequisicao(any())).thenReturn(List.of());
    }

    private Bolsa bolsa(String codigo, int venceEmDias) {
        Bolsa bolsa = new Bolsa(codigo, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.A_POS,
                hoje.minusDays(30), hoje.plusDays(venceEmDias));
        estoque.add(bolsa);
        return bolsa;
    }

    @Test
    @DisplayName("Cenário 1: Bolsa disponível vencida")
    void bolsaDisponivelVencida() {
        Bolsa bol1001 = bolsa("BOL-1001", -1);
        Bolsa bol1002 = bolsa("BOL-1002", 4);

        int descartadas = service.verificarValidade();

        assertEquals("Descartada", bol1001.getStatus().getRotulo());
        assertEquals("Validade vencida", bol1001.getMotivoDescarte());
        assertEquals(hoje, bol1001.getDataDescarte());
        assertEquals(StatusBolsa.DISPONIVEL, bol1002.getStatus());
        assertEquals("1 bolsa descartada por validade", service.mensagemDescarte(descartadas));
    }

    @Test
    @DisplayName("Cenário 2: Bolsa reservada vencida")
    void bolsaReservadaVencida() {
        Requisicao req0004 = Fixtures.comId(Fixtures.requisicao(TipoSanguineo.A_POS, 1), 4L);
        req0004.atribuirCodigo(req0004.getId());
        req0004.marcarAlocada();
        Bolsa bol1003 = bolsa("BOL-1003", -1);
        bol1003.reservarPara(req0004);
        assertEquals("Reservada para Despacho", bol1003.getStatus().getRotulo());

        service.verificarValidade();

        assertEquals(StatusBolsa.DESCARTADA, bol1003.getStatus());
        assertNull(bol1003.getRequisicao());
        assertEquals("REQ-0004", req0004.getCodigo());
        assertEquals("Pendente de Alocação", req0004.getStatus().getRotulo());
        assertEquals(StatusRequisicao.PENDENTE, req0004.getStatus());
    }

    @Test
    @DisplayName("Cenário 3: Nenhuma bolsa vencida")
    void nenhumaBolsaVencida() {
        Bolsa futura = bolsa("BOL-2001", 10);

        int descartadas = service.verificarValidade();

        assertEquals(0, descartadas);
        assertEquals(StatusBolsa.DISPONIVEL, futura.getStatus());
        assertEquals("Nenhuma bolsa vencida encontrada", service.mensagemDescarte(descartadas));
    }
}
