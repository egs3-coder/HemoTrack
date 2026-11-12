package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusBolsa;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.model.Trecho;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.HospitalRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.repository.TrechoRepository;
import br.com.hemotrack.service.HospitalService;
import br.com.hemotrack.service.RequisicaoService;
import br.com.hemotrack.service.RotaService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * U2 - Fluxo integrado: dado um pedido, o sistema
 *   (1) acha os doadores compativeis na MATRIZ 8x8,
 *   (2) busca no INDICE HASH a lista de cada tipo,
 *   (3) escolhe pela validade com a LISTA FEFO (insercao ordenada) e
 *   (4) calcula a rota pelo VIZINHO MAIS PROXIMO.
 */
@DisplayName("U2 - Fluxo integrado: compatibilidade + hash + FEFO + rota")
class U2FluxoIntegradoTest {

    private final LocalDate hoje = LocalDate.now();
    private final List<Bolsa> estoque = new ArrayList<>();
    private final List<Trecho> malha = List.of(
            new Trecho("Hemocentro Central", "Hospital Santa Clara", 25, 12),
            new Trecho("Hemocentro Central", "Hospital Boa Vista", 10, 5),
            new Trecho("Hospital Boa Vista", "Hospital Santa Clara", 8, 4));

    private RequisicaoRepository requisicaoRepository;
    private RequisicaoService service;

    @BeforeEach
    void preparar() {
        requisicaoRepository = mock(RequisicaoRepository.class);
        BolsaRepository bolsaRepository = mock(BolsaRepository.class);
        TrechoRepository trechoRepository = mock(TrechoRepository.class);
        when(trechoRepository.findAll()).thenReturn(malha);
        when(requisicaoRepository.save(any(Requisicao.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(bolsaRepository.save(any(Bolsa.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(bolsaRepository.findByStatus(StatusBolsa.DISPONIVEL)).thenAnswer(chamada -> {
            List<Bolsa> disponiveis = new ArrayList<>();
            for (Bolsa b : estoque) {
                if (b.getStatus() == StatusBolsa.DISPONIVEL) {
                    disponiveis.add(b);
                }
            }
            return disponiveis;
        });
        service = new RequisicaoService(requisicaoRepository, bolsaRepository,
                new HospitalService(mock(HospitalRepository.class)), new RotaService(trechoRepository));
    }

    private Bolsa bolsa(String codigo, TipoSanguineo tipo, int venceEmDias) {
        Bolsa bolsa = new Bolsa(codigo, Hemocomponente.CONCENTRADO_HEMACIAS, tipo, hoje.minusDays(3),
                hoje.plusDays(venceEmDias));
        estoque.add(bolsa);
        return bolsa;
    }

    private Requisicao pedido(TipoSanguineo tipo, int quantidade) {
        Requisicao req = Fixtures.comId(Fixtures.requisicao(tipo, quantidade), 1L);
        req.atribuirCodigo(req.getId());
        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(req));
        return req;
    }

    @Test
    @DisplayName("Pedido O+ (2 bolsas) para o Hospital Santa Clara: aloca por validade e gera a rota")
    void pedidoCompleto() {
        Requisicao req = pedido(TipoSanguineo.O_POS, 2);
        Bolsa aPos = bolsa("BOL-A", TipoSanguineo.A_POS, 1);     // vence primeiro, mas e incompativel
        Bolsa oPos = bolsa("BOL-OP", TipoSanguineo.O_POS, 12);
        Bolsa oNeg = bolsa("BOL-ON", TipoSanguineo.O_NEG, 3);
        Bolsa oPos2 = bolsa("BOL-OP2", TipoSanguineo.O_POS, 6);

        service.atender(1L);

        assertEquals(StatusRequisicao.ALOCADA, req.getStatus());
        assertEquals(StatusBolsa.RESERVADA, oNeg.getStatus());    // vence em 3 dias
        assertEquals(StatusBolsa.RESERVADA, oPos2.getStatus());   // vence em 6 dias
        assertEquals(StatusBolsa.DISPONIVEL, oPos.getStatus());
        assertEquals(StatusBolsa.DISPONIVEL, aPos.getStatus());
        assertEquals("Hemocentro Central → Hospital Boa Vista → Hospital Santa Clara", req.getItinerario());
        assertEquals(18, req.getTempoPrevistoMin().intValue());
    }

    @Test
    @DisplayName("Sem bolsa compatível o fluxo para na alocação e não gera rota")
    void semBolsaCompativel() {
        Requisicao req = pedido(TipoSanguineo.O_NEG, 1);
        bolsa("BOL-A", TipoSanguineo.A_NEG, 4);

        assertThrows(RegraNegocioException.class, () -> service.atender(1L));
        assertEquals(StatusRequisicao.PENDENTE, req.getStatus());
        assertNull(req.getItinerario());
    }
}
