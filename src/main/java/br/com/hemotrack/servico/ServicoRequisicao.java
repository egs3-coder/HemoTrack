package br.com.hemotrack.servico;

import br.com.hemotrack.modelo.EstadoRequisicao;
import br.com.hemotrack.modelo.EstadoBolsa;
import br.com.hemotrack.modelo.Requisicao;
import br.com.hemotrack.repositorio.RepositorioAlocacao;
import br.com.hemotrack.repositorio.RepositorioBolsa;
import br.com.hemotrack.repositorio.RepositorioRequisicao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ServicoRequisicao {

    private final RepositorioRequisicao repositorioRequisicao;
    private final RepositorioAlocacao repositorioAlocacao;
    private final RepositorioBolsa repositorioBolsa;

    public ServicoRequisicao(RepositorioRequisicao repositorioRequisicao,
                             RepositorioAlocacao repositorioAlocacao,
                             RepositorioBolsa repositorioBolsa) {
        this.repositorioRequisicao = repositorioRequisicao;
        this.repositorioAlocacao = repositorioAlocacao;
        this.repositorioBolsa = repositorioBolsa;
    }

    public Requisicao cadastrar(Requisicao requisicao) {
        if (requisicao.getHospital() == null) {
            throw new IllegalArgumentException("Hospital é obrigatório.");
        }

        if (requisicao.getHemocomponente() == null) {
            throw new IllegalArgumentException("Hemocomponente é obrigatório.");
        }

        if (requisicao.getTipoSanguineoPaciente() == null) {
            throw new IllegalArgumentException("Tipo sanguíneo do paciente é obrigatório.");
        }

        if (requisicao.getQuantidade() == null || requisicao.getQuantidade() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        if (requisicao.getUrgencia() == null || requisicao.getUrgencia().isBlank()) {
            throw new IllegalArgumentException("Urgência é obrigatória.");
        }

        requisicao.setCodigoRastreamento(
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        requisicao.setEstado(EstadoRequisicao.PENDENTE_ALOCACAO);
        requisicao.setDataSolicitacao(LocalDateTime.now());

        return repositorioRequisicao.save(requisicao);
    }

    public List<Requisicao> listarTodas() {
        return repositorioRequisicao.findAll();
    }

    @Transactional
    public void remover(Long id) {
        Requisicao requisicao = repositorioRequisicao.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Requisição não encontrada."));

        var alocacoes = repositorioAlocacao.findByRequisicao(requisicao);

        alocacoes.forEach(alocacao -> {
            alocacao.getBolsa().setEstado(EstadoBolsa.DISPONIVEL);
            repositorioBolsa.save(alocacao.getBolsa());
        });

        repositorioAlocacao.deleteAll(alocacoes);
        repositorioAlocacao.flush();
        repositorioRequisicao.delete(requisicao);
    }
}
