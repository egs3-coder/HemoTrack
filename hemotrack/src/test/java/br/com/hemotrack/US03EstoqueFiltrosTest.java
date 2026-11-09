package br.com.hemotrack;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Perfil;
import br.com.hemotrack.model.StatusBolsa;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.model.Usuario;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.UsuarioRepository;
import br.com.hemotrack.service.BolsaService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:estoque_filtros;DB_CLOSE_DELAY=-1",
        "hemotrack.dados-demo=false"})
@AutoConfigureMockMvc
@DisplayName("Filtros no Estoque de Bolsas e Preservação da Regra FEFO")
class US03EstoqueFiltrosTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BolsaRepository bolsaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BolsaService bolsaService;

    private final LocalDate hoje = LocalDate.now();
    private MockHttpSession sessaoAutenticada;

    @BeforeEach
    void preparar() {
        bolsaRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario operador = new Usuario("Operador Teste", "operador_teste", "senha123", Perfil.OPERADOR_HEMOCENTRO);
        usuarioRepository.save(operador);

        sessaoAutenticada = new MockHttpSession();
        sessaoAutenticada.setAttribute("usuarioId", operador.getId());
        sessaoAutenticada.setAttribute("usuarioNome", operador.getNome());
        sessaoAutenticada.setAttribute("usuarioPerfil", operador.getPerfil().getRotulo());
    }

    private Bolsa criarBolsa(String codigo, Hemocomponente hemocomponente, TipoSanguineo tipo,
                             StatusBolsa status, int validadeEmDias) {
        Bolsa b = new Bolsa(codigo, hemocomponente, tipo, hoje.minusDays(5), hoje.plusDays(validadeEmDias));
        if (status == StatusBolsa.DESCARTADA) {
            b.descartar(Bolsa.MOTIVO_VALIDADE, hoje);
        } else if (status == StatusBolsa.EM_TRANSPORTE) {
            b.despachar();
        } else if (status == StatusBolsa.ENTREGUE) {
            b.entregar(hoje);
        }
        return bolsaRepository.save(b);
    }

    @Test
    @DisplayName("Filtro individual por Tipo Sanguíneo com respeito à regra FEFO")
    void filtrarPorTipoSanguineoComFEFO() {
        // Cria bolsas de tipos variados e validades distintas
        Bolsa b1 = criarBolsa("BOL-A1", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.A_POS, StatusBolsa.DISPONIVEL, 10);
        Bolsa b2 = criarBolsa("BOL-A2", Hemocomponente.PLAQUETAS, TipoSanguineo.A_POS, StatusBolsa.DISPONIVEL, 3);
        Bolsa b3 = criarBolsa("BOL-O1", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_NEG, StatusBolsa.DISPONIVEL, 5);

        List<Bolsa> resultado = bolsaService.filtrar(TipoSanguineo.A_POS, null, null);

        assertEquals(2, resultado.size());
        // Regra FEFO: BOL-A2 (vence em 3 dias) deve vir antes de BOL-A1 (vence em 10 dias)
        assertEquals("BOL-A2", resultado.get(0).getCodigo());
        assertEquals("BOL-A1", resultado.get(1).getCodigo());
    }

    @Test
    @DisplayName("Filtro individual por Status com respeito à regra FEFO")
    void filtrarPorStatusComFEFO() {
        Bolsa b1 = criarBolsa("BOL-D1", Hemocomponente.PLAQUETAS, TipoSanguineo.O_POS, StatusBolsa.DISPONIVEL, 15);
        Bolsa b2 = criarBolsa("BOL-D2", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.A_NEG, StatusBolsa.DISPONIVEL, 2);
        Bolsa b3 = criarBolsa("BOL-DESC", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS, StatusBolsa.DESCARTADA, -1);

        List<Bolsa> disponiveis = bolsaService.filtrar(null, StatusBolsa.DISPONIVEL, null);

        assertEquals(2, disponiveis.size());
        assertEquals("BOL-D2", disponiveis.get(0).getCodigo());
        assertEquals("BOL-D1", disponiveis.get(1).getCodigo());

        List<Bolsa> descartadas = bolsaService.filtrar(null, StatusBolsa.DESCARTADA, null);
        assertEquals(1, descartadas.size());
        assertEquals("BOL-DESC", descartadas.get(0).getCodigo());
    }

    @Test
    @DisplayName("Filtro individual por Hemocomponente com respeito à regra FEFO")
    void filtrarPorHemocomponenteComFEFO() {
        Bolsa b1 = criarBolsa("BOL-P1", Hemocomponente.PLAQUETAS, TipoSanguineo.B_POS, StatusBolsa.DISPONIVEL, 8);
        Bolsa b2 = criarBolsa("BOL-P2", Hemocomponente.PLAQUETAS, TipoSanguineo.O_NEG, StatusBolsa.DISPONIVEL, 4);
        Bolsa b3 = criarBolsa("BOL-H1", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.B_POS, StatusBolsa.DISPONIVEL, 2);

        List<Bolsa> plaquetas = bolsaService.filtrar(null, null, Hemocomponente.PLAQUETAS);

        assertEquals(2, plaquetas.size());
        assertEquals("BOL-P2", plaquetas.get(0).getCodigo());
        assertEquals("BOL-P1", plaquetas.get(1).getCodigo());
    }

    @Test
    @DisplayName("Combinação simultânea dos três filtros (Tipo + Status + Hemocomponente)")
    void filtrarCombinandoTresFiltros() {
        Bolsa alvo1 = criarBolsa("BOL-ALVO-1", Hemocomponente.PLAQUETAS, TipoSanguineo.AB_POS, StatusBolsa.DISPONIVEL, 7);
        Bolsa alvo2 = criarBolsa("BOL-ALVO-2", Hemocomponente.PLAQUETAS, TipoSanguineo.AB_POS, StatusBolsa.DISPONIVEL, 2);
        // Não bate status:
        Bolsa outroStatus = criarBolsa("BOL-OUTRO-ST", Hemocomponente.PLAQUETAS, TipoSanguineo.AB_POS, StatusBolsa.EM_TRANSPORTE, 1);
        // Não bate hemocomponente:
        Bolsa outroComp = criarBolsa("BOL-OUTRO-COMP", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.AB_POS, StatusBolsa.DISPONIVEL, 1);
        // Não bate tipo:
        Bolsa outroTipo = criarBolsa("BOL-OUTRO-TIPO", Hemocomponente.PLAQUETAS, TipoSanguineo.O_NEG, StatusBolsa.DISPONIVEL, 1);

        List<Bolsa> resultado = bolsaService.filtrar(TipoSanguineo.AB_POS, StatusBolsa.DISPONIVEL, Hemocomponente.PLAQUETAS);

        assertEquals(2, resultado.size());
        // FEFO preservado:
        assertEquals("BOL-ALVO-2", resultado.get(0).getCodigo());
        assertEquals("BOL-ALVO-1", resultado.get(1).getCodigo());
    }

    @Test
    @DisplayName("Remover todos os filtros restaura listagem original completa em ordem FEFO")
    void semFiltrosRetornaListagemOriginalComFEFO() {
        Bolsa b1 = criarBolsa("BOL-1", Hemocomponente.PLASMA, TipoSanguineo.O_POS, StatusBolsa.DISPONIVEL, 20);
        Bolsa b2 = criarBolsa("BOL-2", Hemocomponente.PLAQUETAS, TipoSanguineo.A_NEG, StatusBolsa.DISPONIVEL, 5);
        Bolsa b3 = criarBolsa("BOL-3", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.B_POS, StatusBolsa.DESCARTADA, -2);

        List<Bolsa> resultado = bolsaService.filtrar(null, null, null);

        assertEquals(3, resultado.size());
        assertEquals("BOL-3", resultado.get(0).getCodigo()); // vencida há 2 dias
        assertEquals("BOL-2", resultado.get(1).getCodigo()); // vence em 5 dias
        assertEquals("BOL-1", resultado.get(2).getCodigo()); // vence em 20 dias
    }

    @Test
    @DisplayName("Validação na tela (Thymeleaf/MockMvc): filtros combinados, chips de remoção individual e botão limpar")
    void integracaoTelaEstoqueFiltros() throws Exception {
        criarBolsa("BOL-TELA-1", Hemocomponente.PLAQUETAS, TipoSanguineo.O_NEG, StatusBolsa.DISPONIVEL, 4);
        criarBolsa("BOL-TELA-2", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_NEG, StatusBolsa.DISPONIVEL, 2);
        criarBolsa("BOL-TELA-3", Hemocomponente.PLAQUETAS, TipoSanguineo.A_POS, StatusBolsa.DISPONIVEL, 1);

        // 1. Acesso sem filtros: exibe todas as 3 bolsas, sem chips de filtros ativos
        mvc.perform(get("/bolsas").session(sessaoAutenticada))
                .andExpect(status().isOk())
                .andExpect(model().attribute("temFiltroAtivo", false))
                .andExpect(content().string(containsString("BOL-TELA-1")))
                .andExpect(content().string(containsString("BOL-TELA-2")))
                .andExpect(content().string(containsString("BOL-TELA-3")))
                .andExpect(content().string(not(containsString("Filtros ativos:"))));

        // 2. Filtrar por Tipo Sanguíneo O_NEG e Hemocomponente PLAQUETAS
        mvc.perform(get("/bolsas")
                        .session(sessaoAutenticada)
                        .param("tipoSanguineo", "O_NEG")
                        .param("hemocomponente", "PLAQUETAS"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("temFiltroAtivo", true))
                .andExpect(content().string(containsString("BOL-TELA-1")))
                .andExpect(content().string(not(containsString("BOL-TELA-2"))))
                .andExpect(content().string(not(containsString("BOL-TELA-3"))))
                .andExpect(content().string(containsString("Filtros ativos:")))
                // Chip do Tipo O- e link de remoção individual mantendo hemocomponente
                .andExpect(content().string(containsString("Tipo: <strong>O-</strong>")))
                .andExpect(content().string(containsString("hemocomponente=PLAQUETAS")))
                // Chip do Hemocomponente Plaquetas e link de remoção mantendo tipoSanguineo
                .andExpect(content().string(containsString("Hemocomponente: <strong>Plaquetas</strong>")))
                .andExpect(content().string(containsString("tipoSanguineo=O_NEG")))
                // Link e botão de remover todos os filtros
                .andExpect(content().string(containsString("Remover todos os filtros")))
                .andExpect(content().string(containsString("Limpar filtros")));

        // 3. Simula remoção individual do filtro de Hemocomponente (mantendo apenas tipoSanguineo=O_NEG)
        mvc.perform(get("/bolsas")
                        .session(sessaoAutenticada)
                        .param("tipoSanguineo", "O_NEG"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("temFiltroAtivo", true))
                // Agora ambas as bolsas O- aparecem
                .andExpect(content().string(containsString("BOL-TELA-1")))
                .andExpect(content().string(containsString("BOL-TELA-2")))
                .andExpect(content().string(not(containsString("BOL-TELA-3"))))
                .andExpect(content().string(containsString("Tipo: <strong>O-</strong>")))
                .andExpect(content().string(not(containsString("Hemocomponente: <strong>Plaquetas</strong>"))));

        // 4. Simula remoção de todos os filtros (GET /bolsas)
        mvc.perform(get("/bolsas").session(sessaoAutenticada))
                .andExpect(status().isOk())
                .andExpect(model().attribute("temFiltroAtivo", false))
                .andExpect(content().string(containsString("BOL-TELA-1")))
                .andExpect(content().string(containsString("BOL-TELA-2")))
                .andExpect(content().string(containsString("BOL-TELA-3")))
                .andExpect(content().string(not(containsString("Filtros ativos:"))));
    }
}
