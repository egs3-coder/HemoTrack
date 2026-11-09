package br.com.hemotrack;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.model.Perfil;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.model.Urgencia;
import br.com.hemotrack.model.Usuario;
import br.com.hemotrack.repository.HospitalRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.repository.UsuarioRepository;
import br.com.hemotrack.service.RequisicaoService;
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
        "spring.datasource.url=jdbc:h2:mem:requisicoes_filtros;DB_CLOSE_DELAY=-1",
        "hemotrack.dados-demo=false"})
@AutoConfigureMockMvc
@DisplayName("Filtros, Pesquisa e Ordenação por Urgência na Tela de Requisições")
class US01RequisicoesFiltrosTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RequisicaoRepository requisicaoRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RequisicaoService requisicaoService;

    private Hospital hospital;
    private MockHttpSession sessaoAutenticada;

    @BeforeEach
    void preparar() {
        requisicaoRepository.deleteAll();
        hospitalRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario usuario = new Usuario("Profissional Teste", "profissional_teste", "senha123", Perfil.PROFISSIONAL_HOSPITALAR);
        usuarioRepository.save(usuario);

        sessaoAutenticada = new MockHttpSession();
        sessaoAutenticada.setAttribute("usuarioId", usuario.getId());
        sessaoAutenticada.setAttribute("usuarioNome", usuario.getNome());
        sessaoAutenticada.setAttribute("usuarioPerfil", usuario.getPerfil().getRotulo());

        hospital = hospitalRepository.save(new Hospital("Hospital Santa Clara", "12345678000190", "Norte"));
    }

    private Requisicao criarRequisicao(String codigo, Urgencia urgencia, StatusRequisicao status) {
        Requisicao req = new Requisicao(hospital, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS, 2, urgencia);
        req.atribuirCodigo(Long.parseLong(codigo.replace("REQ-", "")));
        if (status == StatusRequisicao.ALOCADA) {
            req.marcarAlocada();
        }
        return requisicaoRepository.save(req);
    }

    @Test
    @DisplayName("Ordenação por prioridade de urgência (Emergência > Urgente > Rotina)")
    void ordenacaoPorPrioridadeUrgencia() {
        criarRequisicao("REQ-0001", Urgencia.ROTINA, StatusRequisicao.PENDENTE);
        criarRequisicao("REQ-0002", Urgencia.EMERGENCIA, StatusRequisicao.PENDENTE);
        criarRequisicao("REQ-0003", Urgencia.URGENTE, StatusRequisicao.PENDENTE);

        List<Requisicao> lista = requisicaoService.listar();

        assertEquals(3, lista.size());
        assertEquals("REQ-0002", lista.get(0).getCodigo()); // Emergência primeiro
        assertEquals(Urgencia.EMERGENCIA, lista.get(0).getUrgencia());
        assertEquals("REQ-0003", lista.get(1).getCodigo()); // Urgente segundo
        assertEquals(Urgencia.URGENTE, lista.get(1).getUrgencia());
        assertEquals("REQ-0001", lista.get(2).getCodigo()); // Rotina por último
        assertEquals(Urgencia.ROTINA, lista.get(2).getUrgencia());
    }

    @Test
    @DisplayName("Filtro individual por Urgência")
    void filtrarPorUrgencia() {
        criarRequisicao("REQ-0010", Urgencia.EMERGENCIA, StatusRequisicao.PENDENTE);
        criarRequisicao("REQ-0011", Urgencia.ROTINA, StatusRequisicao.PENDENTE);

        List<Requisicao> emergencia = requisicaoService.filtrar(null, Urgencia.EMERGENCIA, null);
        assertEquals(1, emergencia.size());
        assertEquals("REQ-0010", emergencia.get(0).getCodigo());

        List<Requisicao> rotina = requisicaoService.filtrar(null, Urgencia.ROTINA, null);
        assertEquals(1, rotina.size());
        assertEquals("REQ-0011", rotina.get(0).getCodigo());
    }

    @Test
    @DisplayName("Filtro individual por Status")
    void filtrarPorStatus() {
        criarRequisicao("REQ-0020", Urgencia.URGENTE, StatusRequisicao.PENDENTE);
        criarRequisicao("REQ-0021", Urgencia.URGENTE, StatusRequisicao.ALOCADA);

        List<Requisicao> pendentes = requisicaoService.filtrar(null, null, StatusRequisicao.PENDENTE);
        assertEquals(1, pendentes.size());
        assertEquals("REQ-0020", pendentes.get(0).getCodigo());

        List<Requisicao> alocadas = requisicaoService.filtrar(null, null, StatusRequisicao.ALOCADA);
        assertEquals(1, alocadas.size());
        assertEquals("REQ-0021", alocadas.get(0).getCodigo());
    }

    @Test
    @DisplayName("Filtro combinado: Urgência + Status + Código")
    void filtroCombinado() {
        criarRequisicao("REQ-0030", Urgencia.EMERGENCIA, StatusRequisicao.PENDENTE);
        criarRequisicao("REQ-0031", Urgencia.EMERGENCIA, StatusRequisicao.ALOCADA);
        criarRequisicao("REQ-0032", Urgencia.ROTINA, StatusRequisicao.PENDENTE);

        List<Requisicao> resultado = requisicaoService.filtrar("0030", Urgencia.EMERGENCIA, StatusRequisicao.PENDENTE);
        assertEquals(1, resultado.size());
        assertEquals("REQ-0030", resultado.get(0).getCodigo());
    }

    @Test
    @DisplayName("Pesquisa de requisição existente não abre detalhes automaticamente e a exibe na listagem")
    void pesquisaNaoAbreAutomaticamenteEDetalhaApenasAoClicar() throws Exception {
        Requisicao req = criarRequisicao("REQ-0040", Urgencia.EMERGENCIA, StatusRequisicao.PENDENTE);
        criarRequisicao("REQ-0041", Urgencia.ROTINA, StatusRequisicao.PENDENTE);

        // 1. Pesquisa via parâmetro ?codigo=REQ-0040 na listagem
        mvc.perform(get("/requisicoes")
                        .session(sessaoAutenticada)
                        .param("codigo", "REQ-0040"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("temFiltroAtivo", true))
                // A requisição pesquisada deve estar presente na tabela
                .andExpect(content().string(containsString("REQ-0040")))
                // As outras não devem aparecer
                .andExpect(content().string(not(containsString("REQ-0041"))))
                // O link para abrir os detalhes deve existir na linha
                .andExpect(content().string(containsString("/requisicoes/" + req.getId())));

        // 2. Pesquisa via endpoint antigo /consulta redireciona para a listagem (não para /requisicoes/{id})
        mvc.perform(get("/requisicoes/consulta")
                        .session(sessaoAutenticada)
                        .param("codigo", "REQ-0040"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisicoes?codigo=REQ-0040"));
    }

    @Test
    @DisplayName("Pesquisa sem resultado mantém a tela com aviso amigável")
    void pesquisaSemResultado() throws Exception {
        criarRequisicao("REQ-0050", Urgencia.ROTINA, StatusRequisicao.PENDENTE);

        mvc.perform(get("/requisicoes")
                        .session(sessaoAutenticada)
                        .param("codigo", "REQ-INEXISTENTE"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("temFiltroAtivo", true))
                .andExpect(content().string(containsString("Nenhuma requisição encontrada para os critérios selecionados.")));
    }

    @Test
    @DisplayName("Remoção individual e geral dos filtros na interface")
    void remocaoIndividualEGeral() throws Exception {
        criarRequisicao("REQ-0060", Urgencia.EMERGENCIA, StatusRequisicao.PENDENTE);
        criarRequisicao("REQ-0061", Urgencia.ROTINA, StatusRequisicao.ALOCADA);

        // Com filtros ativos: codigo + urgencia
        mvc.perform(get("/requisicoes")
                        .session(sessaoAutenticada)
                        .param("codigo", "REQ-0060")
                        .param("urgencia", "EMERGENCIA"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("temFiltroAtivo", true))
                .andExpect(content().string(containsString("Filtros ativos:")))
                .andExpect(content().string(containsString("Código: <strong>REQ-0060</strong>")))
                .andExpect(content().string(containsString("Urgência: <strong>Emergência</strong>")))
                // Remoção individual da urgência mantém o código na URL
                .andExpect(content().string(containsString("codigo=REQ-0060")))
                // Link e botão de remover todos
                .andExpect(content().string(containsString("Limpar filtros")))
                .andExpect(content().string(containsString("Remover todos os filtros")));

        // Ao remover todos os filtros (GET /requisicoes)
        mvc.perform(get("/requisicoes").session(sessaoAutenticada))
                .andExpect(status().isOk())
                .andExpect(model().attribute("temFiltroAtivo", false))
                .andExpect(content().string(not(containsString("Filtros ativos:"))))
                // Ambas voltam a ser exibidas
                .andExpect(content().string(containsString("REQ-0060")))
                .andExpect(content().string(containsString("REQ-0061")));
    }
}

