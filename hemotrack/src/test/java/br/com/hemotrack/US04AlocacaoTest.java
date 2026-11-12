package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.hemotrack.algoritmo.MatrizCompatibilidade;
import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusBolsa;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        // Simula o banco: so filtra pelo status e devolve na ORDEM DE CADASTRO (sem ORDER BY).
        // A ordem por validade tem que vir da ListaFefo (insercao ordenada).
        when(bolsaRepository.findByStatus(StatusBolsa.DISPONIVEL)).thenAnswer(chamada -> {
            List<Bolsa> resultado = new ArrayList<>();
            for (Bolsa b : estoque) {
                if (b.getStatus() == StatusBolsa.DISPONIVEL) {
                    resultado.add(b);
                }
            }
            return resultado;
        });
    }

    private Requisicao pedido(long id, TipoSanguineo tipo) {
        return pedido(id, Hemocomponente.CONCENTRADO_HEMACIAS, tipo, 1);
    }

    private Requisicao pedido(long id, Hemocomponente componente, TipoSanguineo tipo, int quantidade) {
        Requisicao requisicao = Fixtures.comId(
                new Requisicao(Fixtures.santaClara(), componente, tipo, quantidade, Urgencia.EMERGENCIA), id);
        requisicao.atribuirCodigo(requisicao.getId());
        when(requisicaoRepository.findById(id)).thenReturn(Optional.of(requisicao));
        return requisicao;
    }

    private Bolsa bolsa(String codigo, TipoSanguineo tipo, int venceEmDias) {
        return bolsa(codigo, Hemocomponente.CONCENTRADO_HEMACIAS, tipo, venceEmDias);
    }

    private Bolsa bolsa(String codigo, Hemocomponente componente, TipoSanguineo tipo, int venceEmDias) {
        Bolsa bolsa = new Bolsa(codigo, componente, tipo, hoje.minusDays(5), hoje.plusDays(venceEmDias));
        estoque.add(bolsa);
        return bolsa;
    }

    // ------------------------------------------------------------------ cenarios da historia

    @Test
    @DisplayName("Cenário 1: Seleção da bolsa compatível mais próxima do vencimento")
    void selecionaBolsaMaisProximaDoVencimento() {
        Requisicao req = pedido(1L, TipoSanguineo.B_POS);
        Bolsa bol0002 = bolsa("BOL-0002", TipoSanguineo.O_NEG, 10);   // cadastrada antes, vence depois
        Bolsa bol0001 = bolsa("BOL-0001", TipoSanguineo.B_POS, 3);

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
    @DisplayName("Regra: só bolsas do hemocomponente solicitado são consideradas")
    void somenteHemocomponenteSolicitado() {
        Requisicao req = pedido(3L, TipoSanguineo.B_POS);
        Bolsa plaquetas = bolsa("BOL-0004", Hemocomponente.PLAQUETAS, TipoSanguineo.B_POS, 2);

        assertThrows(RegraNegocioException.class, () -> service.alocar(3L));
        assertTrue(req.getStatus() == StatusRequisicao.PENDENTE && plaquetas.getStatus() == StatusBolsa.DISPONIVEL);
    }

    // ------------------------------------------------------------------ matriz ABO/Rh 8x8

    @ParameterizedTest(name = "hemácias {0} -> {1}: compatível")
    @DisplayName("Matriz ABO/Rh (hemácias): pares COMPATÍVEIS")
    @CsvSource({
        "O_NEG, O_NEG", "O_NEG, AB_POS", "O_NEG, A_NEG", "O_POS, A_POS", "O_POS, O_POS",
        "A_NEG, A_POS", "A_NEG, AB_NEG", "B_NEG, B_POS", "B_POS, AB_POS", "AB_NEG, AB_POS", "AB_POS, AB_POS"
    })
    void matrizHemaciasCompativel(TipoSanguineo doador, TipoSanguineo receptor) {
        assertTrue(MatrizCompatibilidade.compativel(Hemocomponente.CONCENTRADO_HEMACIAS, doador, receptor));
    }

    @ParameterizedTest(name = "hemácias {0} -> {1}: incompatível")
    @DisplayName("Matriz ABO/Rh (hemácias): pares INCOMPATÍVEIS")
    @CsvSource({
        "O_POS, O_NEG", "A_POS, O_POS", "A_POS, A_NEG", "A_NEG, B_NEG", "B_NEG, A_POS",
        "B_POS, B_NEG", "AB_POS, A_POS", "AB_NEG, O_NEG", "AB_POS, AB_NEG", "A_NEG, O_NEG"
    })
    void matrizHemaciasIncompativel(TipoSanguineo doador, TipoSanguineo receptor) {
        assertFalse(MatrizCompatibilidade.compativel(Hemocomponente.CONCENTRADO_HEMACIAS, doador, receptor));
    }

    @ParameterizedTest(name = "plasma {0} -> {1}: compatível")
    @DisplayName("Matriz ABO/Rh (plasma, regra invertida): pares COMPATÍVEIS")
    @CsvSource({ "AB_NEG, O_POS", "AB_POS, B_NEG", "A_POS, O_NEG", "B_NEG, B_POS", "O_POS, O_NEG" })
    void matrizPlasmaCompativel(TipoSanguineo doador, TipoSanguineo receptor) {
        assertTrue(MatrizCompatibilidade.compativel(Hemocomponente.PLASMA, doador, receptor));
    }

    @ParameterizedTest(name = "plasma {0} -> {1}: incompatível")
    @DisplayName("Matriz ABO/Rh (plasma, regra invertida): pares INCOMPATÍVEIS")
    @CsvSource({ "O_NEG, A_POS", "O_NEG, AB_POS", "A_NEG, B_POS", "B_POS, AB_NEG", "A_POS, AB_POS" })
    void matrizPlasmaIncompativel(TipoSanguineo doador, TipoSanguineo receptor) {
        assertFalse(MatrizCompatibilidade.compativel(Hemocomponente.PLASMA, doador, receptor));
    }

    @Test
    @DisplayName("Matriz de hemácias confere com a regra de antígenos nas 64 combinações")
    void matrizConfereComRegraDeAntigenos() {
        int compativeis = 0;
        for (TipoSanguineo doador : TipoSanguineo.values()) {
            for (TipoSanguineo receptor : TipoSanguineo.values()) {
                // Regra antiga (antigenos), escrita aqui so como "gabarito" independente da matriz
                boolean esperado = (!temAntigeno(doador, 'A') || temAntigeno(receptor, 'A'))
                        && (!temAntigeno(doador, 'B') || temAntigeno(receptor, 'B'))
                        && (!doador.getRotulo().endsWith("+") || receptor.getRotulo().endsWith("+"));
                assertEquals(esperado, MatrizCompatibilidade.compativel(doador, receptor),
                        doador.getRotulo() + " -> " + receptor.getRotulo());
                if (esperado) {
                    compativeis++;
                }
            }
        }
        assertEquals(27, compativeis);
    }

    private static boolean temAntigeno(TipoSanguineo tipo, char antigeno) {
        return tipo.getRotulo().indexOf(antigeno) >= 0;
    }

    @Test
    @DisplayName("Regra: doadores compatíveis vêm da coluna da matriz (B+ recebe de O-, O+, B-, B+)")
    void compatibilidadeAboRh() {
        assertEquals(List.of(TipoSanguineo.O_NEG, TipoSanguineo.O_POS, TipoSanguineo.B_NEG, TipoSanguineo.B_POS),
                TipoSanguineo.B_POS.doadoresCompativeis());
        assertEquals(List.of(TipoSanguineo.O_NEG), TipoSanguineo.O_NEG.doadoresCompativeis());
        assertEquals(8, TipoSanguineo.AB_POS.doadoresCompativeis().size());
        assertEquals(2, MatrizCompatibilidade.doadoresCompativeis(Hemocomponente.PLASMA, TipoSanguineo.AB_POS).length);
        assertEquals(8, MatrizCompatibilidade.doadoresCompativeis(Hemocomponente.PLASMA, TipoSanguineo.O_NEG).length);
    }

    // ------------------------------------------------------------------ alocacao integrada

    @Test
    @DisplayName("Integração: nunca reserva bolsa incompatível, mesmo vencendo antes")
    void nuncaReservaIncompativel() {
        Requisicao req = pedido(4L, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.A_NEG, 3);
        int dias = 1;
        for (TipoSanguineo tipo : TipoSanguineo.values()) {      // uma bolsa de cada tipo, todas vencendo cedo
            bolsa("BOL-" + tipo.name(), tipo, dias++);
        }
        bolsa("BOL-A-NEG-2", TipoSanguineo.A_NEG, 30);

        service.alocar(4L);

        List<String> reservadas = new ArrayList<>();
        for (Bolsa b : estoque) {
            if (b.getStatus() == StatusBolsa.RESERVADA) {
                assertTrue(MatrizCompatibilidade.compativel(b.getHemocomponente(), b.getTipoSanguineo(),
                        req.getTipoSanguineo()), b.getCodigo() + " é incompatível com A-");
                reservadas.add(b.getCodigo());
            }
        }
        // A- recebe so de O- e A-: as tres bolsas compativeis, em ordem de validade
        assertEquals(List.of("BOL-O_NEG", "BOL-A_NEG", "BOL-A-NEG-2"), reservadas);
    }

    @Test
    @DisplayName("Integração: FEFO entre tipos compatíveis, independente da ordem do banco, ignorando vencidas")
    void fefoEntreTiposCompativeis() {
        pedido(5L, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.B_POS, 2);
        Bolsa bPos = bolsa("BOL-B1", TipoSanguineo.B_POS, 9);
        Bolsa vencida = bolsa("BOL-B0", TipoSanguineo.B_POS, -1);
        Bolsa oPos = bolsa("BOL-O1", TipoSanguineo.O_POS, 5);
        Bolsa oNeg = bolsa("BOL-O2", TipoSanguineo.O_NEG, 2);

        service.alocar(5L);

        assertEquals(StatusBolsa.RESERVADA, oNeg.getStatus());
        assertEquals(StatusBolsa.RESERVADA, oPos.getStatus());
        assertEquals(StatusBolsa.DISPONIVEL, bPos.getStatus());
        assertEquals(StatusBolsa.DISPONIVEL, vencida.getStatus());
    }

    @Test
    @DisplayName("Integração: plasma usa a regra invertida (AB+ só recebe plasma AB)")
    void plasmaUsaRegraInvertida() {
        pedido(6L, Hemocomponente.PLASMA, TipoSanguineo.AB_POS, 1);
        Bolsa plasmaA = bolsa("PL-A", Hemocomponente.PLASMA, TipoSanguineo.A_POS, 2);
        Bolsa plasmaAB = bolsa("PL-AB", Hemocomponente.PLASMA, TipoSanguineo.AB_NEG, 7);

        service.alocar(6L);

        assertEquals(StatusBolsa.RESERVADA, plasmaAB.getStatus());
        assertEquals(StatusBolsa.DISPONIVEL, plasmaA.getStatus());
    }

    @Test
    @DisplayName("Integração: estoque insuficiente não reserva nada")
    void estoqueInsuficiente() {
        Requisicao req = pedido(7L, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS, 3);
        Bolsa b1 = bolsa("BOL-1", TipoSanguineo.O_POS, 4);
        Bolsa b2 = bolsa("BOL-2", TipoSanguineo.A_POS, 4);   // incompativel com O+

        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> service.alocar(7L));

        assertEquals("Há apenas 1 bolsa(s) compatível(is) disponível(is) para o tipo O+; o pedido precisa de 3",
                erro.getMessage());
        assertEquals(StatusRequisicao.PENDENTE, req.getStatus());
        assertEquals(StatusBolsa.DISPONIVEL, b1.getStatus());
        assertEquals(StatusBolsa.DISPONIVEL, b2.getStatus());
    }
}
