package br.com.hemotrack.service;

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
 * US05 - Rota de entrega mais rapida: a rota de menor tempo fica registrada no pedido.
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
        return requisicaoRepository.findAllByOrderByDataCriacaoDesc();
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
        List<Bolsa> candidatas = bolsaRepository
                .findByStatusAndHemocomponenteAndTipoSanguineoInAndDataValidadeGreaterThanEqualOrderByDataValidadeAsc(
                        StatusBolsa.DISPONIVEL, requisicao.getHemocomponente(),
                        requisicao.getTipoSanguineo().doadoresCompativeis(), LocalDate.now());
        if (candidatas.isEmpty()) {
            throw new RegraNegocioException("Não há bolsas compatíveis disponíveis para o tipo "
                    + requisicao.getTipoSanguineo().getRotulo());
        }
        if (candidatas.size() < requisicao.getQuantidade()) {
            throw new RegraNegocioException("Há apenas " + candidatas.size()
                    + " bolsa(s) compatível(is) disponível(is) para o tipo "
                    + requisicao.getTipoSanguineo().getRotulo() + "; o pedido precisa de "
                    + requisicao.getQuantidade());
        }
        requisicao.marcarAlocada();
        for (Bolsa bolsa : candidatas.subList(0, requisicao.getQuantidade())) {
            bolsa.reservarPara(requisicao);
            bolsaRepository.save(bolsa);
        }
        return requisicaoRepository.save(requisicao);
    }

    /** US05 cenarios 1 e 2: calcula a rota de menor tempo e registra no pedido. */
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
