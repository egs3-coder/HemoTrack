package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusBolsa;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US04 - Alocação de bolsas compatíveis com prioridade por vencimento")
class US04AlocacaoTest {

    private final LocalDate hoje = LocalDate.now();
    private RequisicaoRepository requisicaoRepository;
    private BolsaRepository bolsaRepository;
    private RequisicaoService service;
    private final List<Bolsa> estoque = new ArrayList<>();

    @BeforeEach
    void preparar() {
        requisicaoRepository = mock(RequisicaoRepository.class);
        bolsaRepository = mock(BolsaRepository.class);
        service = new RequisicaoService(requisicaoRepository, bolsaRepository,
                new HospitalService(mock(HospitalRepository.class)), new RotaService(mock(TrechoRepository.class)));
        when(requisicaoRepository.save(any(Requisicao.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(bolsaRepository.save(any(Bolsa.class))).thenAnswer(chamada -> chamada.getArgument(0));
        // Simula a consulta do banco: Disponivel + hemocomponente + tipos compativeis + nao vencida, por validade
        when(bolsaRepository.findByStatusAndHemocomponenteAndTipoSanguineoInAndDataValidadeGreaterThanEqualOrderByDataValidadeAsc(
                eq(StatusBolsa.DISPONIVEL), any(), any(), any())).thenAnswer(chamada -> {
                    Hemocomponente componente = chamada.getArgument(1);
                    Collection<TipoSanguineo> tipos = chamada.getArgument(2);
                    List<Bolsa> resultado = new ArrayList<>();
                    for (Bolsa b : estoque) {
                        if (b.getStatus() == StatusBolsa.DISPONIVEL && b.getHemocomponente() == componente
                                && tipos.contains(b.getTipoSanguineo()) && !b.estaVencida(hoje)) {
                            resultado.add(b);
                        }
                    }
                    resultado.sort(Comparator.comparing(Bolsa::getDataValidade));
                    return resultado;
                });
    }

    private Requisicao pedido(long id, TipoSanguineo tipo) {
        Requisicao requisicao = Fixtures.comId(Fixtures.requisicao(tipo, 1), id);
        requisicao.atribuirCodigo(requisicao.getId());
        when(requisicaoRepository.findById(id)).thenReturn(Optional.of(requisicao));
        return requisicao;
    }

    private Bolsa bolsa(String codigo, TipoSanguineo tipo, int venceEmDias) {
        Bolsa bolsa = new Bolsa(codigo, Hemocomponente.CONCENTRADO_HEMACIAS, tipo, hoje.minusDays(5),
                hoje.plusDays(venceEmDias));
        estoque.add(bolsa);
        return bolsa;
    }

    @Test
    @DisplayName("Cenário 1: Seleção da bolsa compatível mais próxima do vencimento")
    void selecionaBolsaMaisProximaDoVencimento() {
        Requisicao req = pedido(1L, TipoSanguineo.B_POS);
        Bolsa bol0001 = bolsa("BOL-0001", TipoSanguineo.B_POS, 3);
        Bolsa bol0002 = bolsa("BOL-0002", TipoSanguineo.O_NEG, 10);

        service.alocar(1L);

        assertEquals("REQ-0001", req.getCodigo());
        assertEquals(req, bol0001.getRequisicao());
        assertEquals("Reservada para Despacho", bol0001.getStatus().getRotulo());
        assertEquals(StatusBolsa.DISPONIVEL, bol0002.getStatus());
        assertEquals("Alocada", req.getStatus().getRotulo());
    }

    @Test
    @DisplayName("Cenário 2: Nenhuma bolsa compatível")
    void nenhumaBolsaCompativel() {
        Requisicao req = pedido(2L, TipoSanguineo.O_NEG);
        Bolsa bol0003 = bolsa("BOL-0003", TipoSanguineo.A_POS, 5);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> service.alocar(2L));

        assertEquals("Não há bolsas compatíveis disponíveis para o tipo O-", erro.getMessage());
        assertEquals(StatusRequisicao.PENDENTE, req.getStatus());
        assertEquals(StatusBolsa.DISPONIVEL, bol0003.getStatus());
        assertNull(bol0003.getRequisicao());
    }

    @Test
    @DisplayName("Regra: compatibilidade ABO/Rh (B+ recebe de B+, B-, O+, O-)")
    void compatibilidadeAboRh() {
        assertEquals(List.of(TipoSanguineo.O_NEG, TipoSanguineo.O_POS, TipoSanguineo.B_NEG, TipoSanguineo.B_POS),
                TipoSanguineo.B_POS.doadoresCompativeis());
        assertEquals(List.of(TipoSanguineo.O_NEG), TipoSanguineo.O_NEG.doadoresCompativeis());
        assertEquals(8, TipoSanguineo.AB_POS.doadoresCompativeis().size());
    }

    @Test
    @DisplayName("Regra: só bolsas do hemocomponente solicitado são consideradas")
    void somenteHemocomponenteSolicitado() {
        Requisicao req = pedido(3L, TipoSanguineo.B_POS);
        Bolsa plaquetas = new Bolsa("BOL-0004", Hemocomponente.PLAQUETAS, TipoSanguineo.B_POS, hoje, hoje.plusDays(2));
        estoque.add(plaquetas);

        assertThrows(RegraNegocioException.class, () -> service.alocar(3L));
        assertTrue(req.getStatus() == StatusRequisicao.PENDENTE && plaquetas.getStatus() == StatusBolsa.DISPONIVEL);
    }
}
