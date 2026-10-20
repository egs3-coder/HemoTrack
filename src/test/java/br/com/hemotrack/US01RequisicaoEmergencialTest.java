package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.model.Urgencia;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.HospitalRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.repository.TrechoRepository;
import br.com.hemotrack.service.HospitalService;
import br.com.hemotrack.service.RequisicaoService;
import br.com.hemotrack.service.RotaService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US01 - Requisição emergencial de hemocomponentes")
class US01RequisicaoEmergencialTest {

    private RequisicaoRepository requisicaoRepository;
    private RequisicaoService service;
    private Hospital santaClara;

    @BeforeEach
    void preparar() {
        requisicaoRepository = mock(RequisicaoRepository.class);
        HospitalRepository hospitalRepository = mock(HospitalRepository.class);
        service = new RequisicaoService(requisicaoRepository, mock(BolsaRepository.class),
                new HospitalService(hospitalRepository), new RotaService(mock(TrechoRepository.class)));
        santaClara = Fixtures.santaClara();
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(santaClara));
        // O banco atribui o id 1 na primeira gravacao
        when(requisicaoRepository.save(any(Requisicao.class))).thenAnswer(chamada -> {
            Requisicao r = chamada.getArgument(0);
            return r.getId() == null ? Fixtures.comId(r, 1L) : r;
        });
    }

    @Test
    @DisplayName("Cenário 1: Solicitação enviada com sucesso")
    void solicitacaoEnviadaComSucesso() {
        Requisicao requisicao = service.criar(1L, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS, 2,
                Urgencia.EMERGENCIA);

        assertEquals("Pendente de Alocação", requisicao.getStatus().getRotulo());
        assertEquals("REQ-0001", requisicao.getCodigo());
        assertEquals("Hospital Santa Clara", requisicao.getHospital().getNome());
        assertEquals("Emergência", requisicao.getUrgencia().getRotulo());
    }

    @Test
    @DisplayName("Cenário 2: Requisição sem tipo sanguíneo")
    void requisicaoSemTipoSanguineo() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.criar(1L, Hemocomponente.CONCENTRADO_HEMACIAS, null, 2, Urgencia.EMERGENCIA));

        assertEquals("O tipo sanguíneo do paciente é obrigatório para validação de compatibilidade", erro.getMessage());
        verify(requisicaoRepository, never()).save(any(Requisicao.class));
    }

    @Test
    @DisplayName("Cenário 3: Quantidade fora do limite (0 e 11)")
    void quantidadeForaDoLimite() {
        RegraNegocioException zero = assertThrows(RegraNegocioException.class,
                () -> service.criar(1L, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS, 0, Urgencia.EMERGENCIA));
        RegraNegocioException onze = assertThrows(RegraNegocioException.class,
                () -> service.criar(1L, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS, 11, Urgencia.EMERGENCIA));

        assertEquals("A quantidade deve ser de 1 a 10 bolsas", zero.getMessage());
        assertEquals("A quantidade deve ser de 1 a 10 bolsas", onze.getMessage());
        verify(requisicaoRepository, never()).save(any(Requisicao.class));
    }

    @Test
    @DisplayName("Regra: hospital, hemocomponente e urgência são obrigatórios")
    void camposObrigatorios() {
        assertEquals("O hospital de destino é obrigatório", assertThrows(RegraNegocioException.class,
                () -> service.criar(null, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS, 2, Urgencia.EMERGENCIA)).getMessage());
        assertEquals("O tipo de hemocomponente é obrigatório", assertThrows(RegraNegocioException.class,
                () -> service.criar(1L, null, TipoSanguineo.O_POS, 2, Urgencia.EMERGENCIA)).getMessage());
        assertEquals("O nível de urgência é obrigatório", assertThrows(RegraNegocioException.class,
                () -> service.criar(1L, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS, 2, null)).getMessage());
    }

    @Test
    @DisplayName("Regra: hospital inativo não recebe requisições (US09)")
    void hospitalInativo() {
        santaClara.inativar();

        assertThrows(RegraNegocioException.class,
                () -> service.criar(1L, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS, 2, Urgencia.EMERGENCIA));
    }

    @Test
    @DisplayName("Regra: toda requisição nasce Pendente de Alocação")
    void nasceComoPendente() {
        assertEquals(StatusRequisicao.PENDENTE, Fixtures.requisicao(TipoSanguineo.O_POS, 2).getStatus());
    }
}
