package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.Rota;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.model.Trecho;
import br.com.hemotrack.model.Urgencia;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.HospitalRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.repository.TrechoRepository;
import br.com.hemotrack.service.HospitalService;
import br.com.hemotrack.service.RequisicaoService;
import br.com.hemotrack.service.RotaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US05 - Rota de entrega mais rápida até o hospital")
class US05RotaTest {

    private final List<Trecho> malha = List.of(
            new Trecho("Hemocentro Central", "Hospital Santa Clara", 25, 12),
            new Trecho("Hemocentro Central", "Hospital Boa Vista", 10, 5),
            new Trecho("Hospital Boa Vista", "Hospital Santa Clara", 8, 4));

    private RequisicaoRepository requisicaoRepository;
    private RequisicaoService service;

    @BeforeEach
    void preparar() {
        TrechoRepository trechoRepository = mock(TrechoRepository.class);
        when(trechoRepository.findAll()).thenReturn(malha);
        requisicaoRepository = mock(RequisicaoRepository.class);
        when(requisicaoRepository.save(any(Requisicao.class))).thenAnswer(chamada -> chamada.getArgument(0));
        service = new RequisicaoService(requisicaoRepository, mock(BolsaRepository.class),
                new HospitalService(mock(HospitalRepository.class)), new RotaService(trechoRepository));
    }

    @Test
    @DisplayName("Cenário 1: Rota mais rápida passa por um ponto intermediário")
    void rotaPassaPorPontoIntermediario() {
        Requisicao req = Fixtures.comId(Fixtures.requisicao(TipoSanguineo.O_POS, 1), 1L);
        req.gerarCodigo();
        req.marcarAlocada();
        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(req));

        service.gerarRota(1L);

        assertEquals("Hemocentro Central → Hospital Boa Vista → Hospital Santa Clara", req.getItinerario());
        assertEquals("9", req.getDistanciaFormatada());
        assertEquals(18, req.getTempoPrevistoMin().intValue());
    }

    @Test
    @DisplayName("Cenário 2: Destino sem ligação na malha")
    void destinoSemLigacao() {
        Hospital ilhaVerde = Fixtures.comId(new Hospital("Hospital Ilha Verde", "11111111000111", "Leste"), 3L);
        Requisicao req = Fixtures.comId(new Requisicao(ilhaVerde, Hemocomponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.O_POS, 1, Urgencia.URGENTE), 3L);
        req.gerarCodigo();
        req.marcarAlocada();
        when(requisicaoRepository.findById(3L)).thenReturn(Optional.of(req));

        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> service.gerarRota(3L));

        assertEquals("Não existe rota cadastrada entre Hemocentro Central e Hospital Ilha Verde", erro.getMessage());
        assertNull(req.getItinerario());
    }

    @Test
    @DisplayName("Regra: a rota escolhida é a de menor tempo total (algoritmo isolado)")
    void menorTempoTotal() {
        Rota rota = new RotaService(null).calcular(malha, "Hemocentro Central", "Hospital Santa Clara");

        assertEquals(18, rota.getTempoMin());
        assertEquals(9.0, rota.getDistanciaKm(), 0.001);
    }
}
