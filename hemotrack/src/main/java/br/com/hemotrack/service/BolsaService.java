package br.com.hemotrack.service;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusBolsa;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US03 - Cadastro e entrada de bolsas no estoque.
 *   - O codigo da bolsa e unico.
 *   - A data de validade deve ser posterior a data atual.
 *   - Toda bolsa entra com status "Disponivel".
 * US10 - Descarte de bolsas vencidas.
 *   - A verificacao pode ser acionada pelo operador e tambem ocorre periodicamente (RotinaValidade).
 *   - Bolsa vencida passa para "Descartada" com motivo "Validade vencida" e data do descarte.
 *   - Se a bolsa vencida estava reservada, a requisicao volta para "Pendente de Alocacao".
 */
@Service
public class BolsaService {

    private final BolsaRepository bolsaRepository;
    private final RequisicaoRepository requisicaoRepository;

    public BolsaService(BolsaRepository bolsaRepository, RequisicaoRepository requisicaoRepository) {
        this.bolsaRepository = bolsaRepository;
        this.requisicaoRepository = requisicaoRepository;
    }

    public List<Bolsa> listar() {
        return bolsaRepository.findAllByOrderByDataValidadeAsc();
    }

    /**
     * Saldo de bolsas disponiveis por tipo sanguineo (painel - US03 cenario 1).
     * Uma bolsa ja vencida, mesmo antes da verificacao da US10, nao conta como disponivel para uso.
     */
    public Map<TipoSanguineo, Long> saldoPorTipo() {
        LocalDate hoje = LocalDate.now();
        Map<TipoSanguineo, Long> saldo = new LinkedHashMap<>();
        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            saldo.put(tipo, bolsaRepository.countByStatusAndTipoSanguineoAndDataValidadeGreaterThanEqual(
                    StatusBolsa.DISPONIVEL, tipo, hoje));
        }
        return saldo;
    }

    /** Bolsas disponiveis que vencem de hoje ate os proximos dias (alerta do painel). */
    public long vencendoNosProximosDias(int dias) {
        LocalDate hoje = LocalDate.now();
        return bolsaRepository.countByStatusAndDataValidadeBetween(StatusBolsa.DISPONIVEL, hoje, hoje.plusDays(dias));
    }

    @Transactional
    public Bolsa cadastrar(String codigo, Hemocomponente hemocomponente, TipoSanguineo tipoSanguineo,
                           LocalDate dataColeta, LocalDate dataValidade) {
        if (codigo == null || codigo.isBlank()) {
            throw new RegraNegocioException("O código da bolsa é obrigatório");
        }
        if (hemocomponente == null) {
            throw new RegraNegocioException("O hemocomponente é obrigatório");
        }
        if (tipoSanguineo == null) {
            throw new RegraNegocioException("O tipo ABO/Rh é obrigatório");
        }
        if (dataValidade == null) {
            throw new RegraNegocioException("A data de validade é obrigatória");
        }
        LocalDate hoje = LocalDate.now();
        // Validade igual a hoje tambem e bloqueada: precisa ser posterior a data atual.
        if (!dataValidade.isAfter(hoje)) {
            throw new RegraNegocioException("Data de validade inválida. Não é permitido cadastrar bolsas vencidas");
        }
        if (dataColeta != null && dataColeta.isAfter(hoje)) {
            throw new RegraNegocioException("A data de coleta não pode ser futura");
        }
        String codigoLimpo = codigo.trim().toUpperCase();
        if (bolsaRepository.existsByCodigo(codigoLimpo)) {
            throw new RegraNegocioException("Já existe uma bolsa cadastrada com o código " + codigoLimpo);
        }
        return bolsaRepository.save(new Bolsa(codigoLimpo, hemocomponente, tipoSanguineo, dataColeta, dataValidade));
    }

    /**
     * US10: descarta as bolsas vencidas que ainda estao no estoque.
     * Se a bolsa estava reservada, o pedido volta para "Pendente de Alocacao"
     * e as demais bolsas reservadas para ele sao liberadas.
     *
     * @return quantidade de bolsas descartadas
     */
    @Transactional
    public int verificarValidade() {
        LocalDate hoje = LocalDate.now();
        List<Bolsa> vencidas = bolsaRepository.findByStatusInAndDataValidadeBefore(
                List.of(StatusBolsa.DISPONIVEL, StatusBolsa.RESERVADA), hoje);
        for (Bolsa bolsa : vencidas) {
            Requisicao requisicao = bolsa.getRequisicao();
            bolsa.descartar(Bolsa.MOTIVO_VALIDADE, hoje);
            bolsaRepository.save(bolsa);
            if (requisicao != null) {
                for (Bolsa outra : bolsaRepository.findByRequisicao(requisicao)) {
                    if (outra.getStatus() == StatusBolsa.RESERVADA && !outra.estaVencida(hoje)) {
                        outra.liberar();
                        bolsaRepository.save(outra);
                    }
                }
                requisicao.voltarParaPendente();
                requisicaoRepository.save(requisicao);
            }
        }
        return vencidas.size();
    }

    public String mensagemDescarte(int quantidade) {
        if (quantidade == 0) {
            return "Nenhuma bolsa vencida encontrada";
        }
        if (quantidade == 1) {
            return "1 bolsa descartada por validade";
        }
        return quantidade + " bolsas descartadas por validade";
    }
}
