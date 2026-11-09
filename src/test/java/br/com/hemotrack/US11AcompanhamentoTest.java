package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.hemotrack.excecao.RecursoNaoEncontradoException;
import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.HospitalRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.repository.TrechoRepository;
import br.com.hemotrack.service.HospitalService;
import br.com.hemotrack.service.RequisicaoService;
import br.com.hemotrack.service.RotaService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US11 - Acompanhamento do pedido e confirmação de recebimento")
class US11AcompanhamentoTest {

    private RequisicaoRepository requisicaoRepository;
    private BolsaRepository bolsaRepository;
    private RequisicaoService service;
    private Requisicao req0001;
    private Bolsa bol0001;

    @BeforeEach
    void preparar() {
        requisicaoRepository = mock(RequisicaoRepository.class);
        bolsaRepository = mock(BolsaRepository.class);
        service = new RequisicaoService(requisicaoRepository, bolsaRepository,
                new HospitalService(mock(HospitalRepository.class)), new RotaService(mock(TrechoRepository.class)));
        when(requisicaoRepository.save(any(Requisicao.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(bolsaRepository.save(any(Bolsa.class))).thenAnswer(chamada -> chamada.getArgument(0));

        req0001 = Fixtures.comId(Fixtures.emTransporte(TipoSanguineo.O_POS), 1L);
        req0001.gerarCodigo();
        bol0001 = new Bolsa("BOL-0001", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS,
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(20));
        bol0001.reservarPara(req0001);
        bol0001.despachar();
        when(requisicaoRepository.findByCodigo("REQ-0001")).thenReturn(Optional.of(req0001));
        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(req0001));
        when(bolsaRepository.findByRequisicao(req0001)).thenReturn(List.of(bol0001));
    }

    @Test
    @DisplayName("Cenário 1: Consulta do pedido pelo identificador")
    void consultaPeloIdentificador() {
        Requisicao encontrada = service.buscarPorCodigo("req-0001 ");

        assertEquals("Em Transporte", encontrada.getStatus().getRotulo());
        assertEquals("Hospital Santa Clara", encontrada.getHospital().getNome());
        assertEquals("BOL-0001", service.bolsasDa(encontrada).get(0).getCodigo());
    }

    @Test
    @DisplayName("Cenário 2: Confirmação de recebimento")
    void confirmacaoDeRecebimento() {
        service.confirmarRecebimento(1L);

        assertEquals("Entregue", req0001.getStatus().getRotulo());
        assertNotNull(req0001.getDataEntrega());
        assertEquals("Entregue", bol0001.getStatus().getRotulo());
        assertEquals(LocalDate.now(), bol0001.getDataEntrega());
    }

    @Test
    @DisplayName("Cenário 3: Confirmação de pedido que ainda não saiu")
    void confirmacaoDePedidoQueNaoSaiu() {
        Requisicao req0002 = Fixtures.comId(Fixtures.requisicao(TipoSanguineo.O_POS, 1), 2L);
        req0002.gerarCodigo();
        when(requisicaoRepository.findById(2L)).thenReturn(Optional.of(req0002));

        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> service.confirmarRecebimento(2L));

        assertEquals("Só é possível confirmar o recebimento de pedidos em transporte", erro.getMessage());
        assertEquals(StatusRequisicao.PENDENTE, req0002.getStatus());
    }

    @Test
    @DisplayName("Cenário 4: Identificador inexistente")
    void identificadorInexistente() {
        when(requisicaoRepository.findByCodigo("REQ-9999")).thenReturn(Optional.empty());

        RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.buscarPorCodigo("REQ-9999"));

        assertEquals("Pedido REQ-9999 não encontrado", erro.getMessage());
    }

    @Test
    @DisplayName("Regra: sequência Pendente de Alocação → Alocada → Em Transporte → Entregue")
    void sequenciaDeStatus() {
        StatusRequisicao[] sequencia = StatusRequisicao.values();
        assertEquals("Pendente de Alocação", sequencia[0].getRotulo());
        assertEquals("Alocada", sequencia[1].getRotulo());
        assertEquals("Em Transporte", sequencia[2].getRotulo());
        assertEquals("Entregue", sequencia[3].getRotulo());
        assertEquals(2, req0001.getEtapa());
    }
}
