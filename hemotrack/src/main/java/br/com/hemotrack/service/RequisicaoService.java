package br.com.hemotrack.service;

import br.com.hemotrack.algoritmo.ListaFefo;
import br.com.hemotrack.algoritmo.MatrizCompatibilidade;
import br.com.hemotrack.algoritmo.TabelaHashEstoque;
import br.com.hemotrack.excecao.RecursoNaoEncontradoException;
import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.Rota;
import br.com.hemotrack.model.StatusBolsa;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.model.Urgencia;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US01 - Requisicao emergencial de hemocomponentes.
 *   - Hospital de destino, hemocomponente, tipo sanguineo, quantidade e urgencia sao obrigatorios.
 *   - A quantidade deve ser de 1 a 10 bolsas por requisicao.
 *   - Toda requisicao nasce com status "Pendente de Alocacao" e recebe um identificador REQ-0001.
 * US04 - Alocacao de bolsas compativeis com prioridade por vencimento.
 *   - So sao consideradas bolsas "Disponivel", do hemocomponente solicitado e compativeis em ABO/Rh.
 *   - Entre as compativeis, e escolhida a de vencimento mais proximo.
 * US05 - Rota de entrega (vizinho mais proximo, U2): a rota calculada fica registrada no pedido.
 * US11 - Acompanhamento do pedido e confirmacao de recebimento.
 *   - Sequencia: "Pendente de Alocacao" -> "Alocada" -> "Em Transporte" -> "Entregue".
 *   - So e possivel confirmar o recebimento de pedido "Em Transporte".
 *   - Na confirmacao, grava-se a data/hora da entrega e as bolsas passam para "Entregue".
 */
@Service
public class RequisicaoService {

    private final RequisicaoRepository requisicaoRepository;
    private final BolsaRepository bolsaRepository;
    private final HospitalService hospitalService;
    private final RotaService rotaService;

    public RequisicaoService(RequisicaoRepository requisicaoRepository, BolsaRepository bolsaRepository,
                             HospitalService hospitalService, RotaService rotaService) {
        this.requisicaoRepository = requisicaoRepository;
        this.bolsaRepository = bolsaRepository;
        this.hospitalService = hospitalService;
        this.rotaService = rotaService;
    }

    public List<Requisicao> listar() {
        return requisicaoRepository.findAllByOrderByPrioridadeUrgencia();
    }

    /**
     * Lista requisições aplicando filtros combinados por código, urgência, status, tipo sanguíneo e hospital.
     * Ordena por prioridade de urgência (Emergência > Urgente > Rotina) e data de criação decrescente.
     * Se nenhum critério for fornecido, retorna a listagem original completa.
     */
    public List<Requisicao> filtrar(String codigo, Urgencia urgencia, StatusRequisicao status,
                                    TipoSanguineo tipoSanguineo, Long hospitalId) {
        String codigoLimpo = (codigo != null && !codigo.isBlank()) ? codigo.trim() : null;
        if (codigoLimpo == null && urgencia == null && status == null && tipoSanguineo == null && hospitalId == null) {
            return listar();
        }
        return requisicaoRepository.filtrar(codigoLimpo, urgencia, status, tipoSanguineo, hospitalId);
    }

    public List<Requisicao> filtrar(String codigo, Urgencia urgencia, StatusRequisicao status) {
        return filtrar(codigo, urgencia, status, null, null);
    }

    public Requisicao buscar(Long id) {
        return requisicaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    /** US11 cenarios 1 e 4: consulta pelo identificador de rastreamento. */
    public Requisicao buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new RegraNegocioException("Informe o identificador do pedido (ex.: REQ-0001)");
        }
        // Nao diferencia maiusculas e ignora espacos: "req-0001 " encontra "REQ-0001".
        String codigoLimpo = codigo.replaceAll("\\s+", "").toUpperCase();
        return requisicaoRepository.findByCodigo(codigoLimpo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + codigoLimpo + " não encontrado"));
    }

    public List<Bolsa> bolsasDa(Requisicao requisicao) {
        return bolsaRepository.findByRequisicao(requisicao);
    }

    public long contarPorStatus(StatusRequisicao status) {
        return requisicaoRepository.countByStatus(status);
    }

    /** US01 cenarios 1, 2 e 3. */
    @Transactional
    public Requisicao criar(Long hospitalId, Hemocomponente hemocomponente, TipoSanguineo tipoSanguineo,
                            Integer quantidade, Urgencia urgencia) {
        if (hospitalId == null) {
            throw new RegraNegocioException("O hospital de destino é obrigatório");
        }
        Hospital hospital = hospitalService.buscar(hospitalId);
        if (!hospital.isAtivo()) {
            throw new RegraNegocioException("O hospital selecionado está inativo");
        }
        if (hemocomponente == null) {
            throw new RegraNegocioException("O tipo de hemocomponente é obrigatório");
        }
        if (tipoSanguineo == null) {
            throw new RegraNegocioException(
                    "O tipo sanguíneo do paciente é obrigatório para validação de compatibilidade");
        }
        if (quantidade == null || quantidade < 1 || quantidade > 10) {
            throw new RegraNegocioException("A quantidade deve ser de 1 a 10 bolsas");
        }
        if (urgencia == null) {
            throw new RegraNegocioException("O nível de urgência é obrigatório");
        }
        Requisicao requisicao = new Requisicao(hospital, hemocomponente, tipoSanguineo, quantidade, urgencia);
        requisicao.atribuirCodigo(proximoNumero());
        return requisicaoRepository.save(requisicao);
    }

    /**
     * US01: identificador em ordem de cadastro. E calculado a partir da ultima requisicao gravada
     * (e nao do id do banco), para que a sequencia REQ-0001, REQ-0002, ... nao tenha saltos.
     */
    private long proximoNumero() {
        return requisicaoRepository.findTopByOrderByIdDesc()
                .map(ultima -> Requisicao.numeroDoCodigo(ultima.getCodigo()) + 1)
                .orElse(1L);
    }

    /**
     * US04 cenarios 1 e 2: reserva as bolsas compativeis de vencimento mais proximo (FEFO).
     * Sem bolsas suficientes nada e reservado e o pedido permanece "Pendente de Alocacao".
     */
    @Transactional
    public Requisicao alocar(Long id) {
        Requisicao requisicao = buscar(id);
        if (requisicao.getStatus() != StatusRequisicao.PENDENTE) {
            throw new RegraNegocioException("Só é possível alocar requisições pendentes de alocação");
        }
        List<Bolsa> escolhidas = selecionarBolsas(requisicao, bolsaRepository.findByStatus(StatusBolsa.DISPONIVEL),
                LocalDate.now());
        requisicao.marcarAlocada();
        for (Bolsa bolsa : escolhidas) {
            bolsa.reservarPara(requisicao);
            bolsaRepository.save(bolsa);
        }
        return requisicaoRepository.save(requisicao);
    }

    /**
     * Integracao dos algoritmos da U2 na alocacao (n = bolsas disponiveis, q = bolsas pedidas):
     *  (1) TabelaHashEstoque.construir: cada bolsa vai para a ListaFefo da chave (componente, tipo)
     *      por INSERCAO ORDENADA -> a ordem de validade sai daqui, e nao de ORDER BY.
     *  (2) MatrizCompatibilidade: a coluna do receptor da os doadores aceitos (8 consultas O(1)).
     *  (3) Para cada doador aceito, uma busca O(1) no hash devolve a lista FEFO daquele tipo;
     *      as vencidas (prefixo da lista) sao puladas.
     *  (4) q vezes: compara so as CABECAS das ate 8 listas e retira a que vence primeiro
     *      (intercalacao de listas ordenadas) -> FEFO entre todos os tipos compativeis.
     * Nada e reservado se nao houver bolsas suficientes.
     */
    List<Bolsa> selecionarBolsas(Requisicao requisicao, List<Bolsa> disponiveis, LocalDate hoje) {
        TabelaHashEstoque indice = TabelaHashEstoque.construir(disponiveis);

        TipoSanguineo[] doadores = MatrizCompatibilidade.doadoresCompativeis(
                requisicao.getHemocomponente(), requisicao.getTipoSanguineo());
        ListaFefo[] listas = new ListaFefo[doadores.length];
        int nListas = 0;
        int totalCompativeis = 0;
        for (TipoSanguineo doador : doadores) {
            ListaFefo lista = indice.buscar(requisicao.getHemocomponente(), doador);
            if (lista == null) {
                continue;
            }
            lista.removerVencidas(hoje);
            if (!lista.estaVazia()) {
                listas[nListas++] = lista;
                totalCompativeis += lista.tamanho();
            }
        }

        if (totalCompativeis == 0) {
            throw new RegraNegocioException("Não há bolsas compatíveis disponíveis para o tipo "
                    + requisicao.getTipoSanguineo().getRotulo());
        }
        if (totalCompativeis < requisicao.getQuantidade()) {
            throw new RegraNegocioException("Há apenas " + totalCompativeis
                    + " bolsa(s) compatível(is) disponível(is) para o tipo "
                    + requisicao.getTipoSanguineo().getRotulo() + "; o pedido precisa de "
                    + requisicao.getQuantidade());
        }

        List<Bolsa> escolhidas = new ArrayList<>(requisicao.getQuantidade());
        while (escolhidas.size() < requisicao.getQuantidade()) {
            ListaFefo melhor = null;
            for (int i = 0; i < nListas; i++) {
                if (listas[i].estaVazia()) {
                    continue;
                }
                if (melhor == null || ListaFefo.venceAntes(listas[i].primeira(), melhor.primeira())) {
                    melhor = listas[i];
                }
            }
            escolhidas.add(melhor.removerPrimeira());
        }
        return escolhidas;
    }

    /**
     * Fluxo integrado da U2: dado um pedido, aloca as bolsas compativeis (matriz + hash + FEFO)
     * e ja calcula a rota de entrega (vizinho mais proximo).
     */
    @Transactional
    public Requisicao atender(Long id) {
        alocar(id);
        return gerarRota(id);
    }

    /** US05 cenarios 1 e 2: calcula a rota pelo vizinho mais proximo e registra no pedido. */
    @Transactional
    public Requisicao gerarRota(Long id) {
        Requisicao requisicao = buscar(id);
        // A rota so pode ser gerada para pedidos "Alocada" (verificado antes de calcular o caminho).
        if (requisicao.getStatus() != StatusRequisicao.ALOCADA) {
            throw new RegraNegocioException("Só é possível gerar rota para pedidos com status Alocada");
        }
        Rota rota = rotaService.calcularAteHospital(requisicao.getHospital().getNome());
        requisicao.registrarRota(rota);
        return requisicaoRepository.save(requisicao);
    }

    @Transactional
    public Requisicao despachar(Long id) {
        Requisicao requisicao = buscar(id);
        requisicao.despachar(LocalDateTime.now());
        for (Bolsa bolsa : bolsaRepository.findByRequisicao(requisicao)) {
            bolsa.despachar();
            bolsaRepository.save(bolsa);
        }
        return requisicaoRepository.save(requisicao);
    }

    /** US11 cenarios 2 e 3. */
    @Transactional
    public Requisicao confirmarRecebimento(Long id) {
        Requisicao requisicao = buscar(id);
        requisicao.confirmarRecebimento(LocalDateTime.now());
        for (Bolsa bolsa : bolsaRepository.findByRequisicao(requisicao)) {
            bolsa.entregar(LocalDate.now());
            bolsaRepository.save(bolsa);
        }
        return requisicaoRepository.save(requisicao);
    }
}
