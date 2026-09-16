package br.com.hemotrack.servico;

import br.com.hemotrack.modelo.*;
import br.com.hemotrack.repositorio.RepositorioAlocacao;
import br.com.hemotrack.repositorio.RepositorioBolsa;
import br.com.hemotrack.repositorio.RepositorioRequisicao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ServicoAlocacao {

    private final RepositorioBolsa repositorioBolsa;
    private final RepositorioRequisicao repositorioRequisicao;
    private final RepositorioAlocacao repositorioAlocacao;
    private final ServicoCompatibilidade servicoCompatibilidade;

    public ServicoAlocacao(RepositorioBolsa repositorioBolsa,
                           RepositorioRequisicao repositorioRequisicao,
                           RepositorioAlocacao repositorioAlocacao,
                           ServicoCompatibilidade servicoCompatibilidade) {
        this.repositorioBolsa = repositorioBolsa;
        this.repositorioRequisicao = repositorioRequisicao;
        this.repositorioAlocacao = repositorioAlocacao;
        this.servicoCompatibilidade = servicoCompatibilidade;
    }

    @Transactional
    public Alocacao alocar(Long idRequisicao) {
        return alocar(idRequisicao, 1).get(0);
    }

    @Transactional
    public List<Alocacao> alocar(Long idRequisicao, Integer quantidadeParaAlocar) {
        if (quantidadeParaAlocar == null || quantidadeParaAlocar <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade para alocar deve ser maior que zero.");
        }

        Requisicao requisicao = repositorioRequisicao
                .findById(idRequisicao)
                .orElseThrow(() ->
                        new IllegalArgumentException("Requisição não encontrada."));

        if (requisicao.getEstado() != EstadoRequisicao.PENDENTE_ALOCACAO
                && requisicao.getEstado() != EstadoRequisicao.PARCIALMENTE_ALOCADA) {
            throw new IllegalStateException(
                    "A requisição não possui bolsas pendentes de alocação.");
        }

        int quantidadeJaAlocada =
                repositorioAlocacao.findByRequisicao(requisicao).size();
        int quantidadePendente =
                Math.max(requisicao.getQuantidade() - quantidadeJaAlocada, 0);

        if (quantidadeParaAlocar > quantidadePendente) {
            throw new IllegalArgumentException(
                    "A requisição possui apenas " + quantidadePendente
                            + " bolsa(s) pendente(s).");
        }

        List<BolsaSangue> bolsasCompativeis =
                buscarBolsasCompativeisDisponiveis(requisicao);

        if (quantidadeParaAlocar > bolsasCompativeis.size()) {
            throw new IllegalStateException(
                    "Estoque insuficiente. Existem " + bolsasCompativeis.size()
                            + " bolsa(s) compatível(is) disponível(is), mas foram solicitadas "
                            + quantidadeParaAlocar + ".");
        }

        List<Alocacao> novasAlocacoes = new ArrayList<>();

        for (int indice = 0; indice < quantidadeParaAlocar; indice++) {
            BolsaSangue bolsaSelecionada = bolsasCompativeis.get(indice);
            bolsaSelecionada.setEstado(EstadoBolsa.RESERVADA_PARA_DESPACHO);
            repositorioBolsa.save(bolsaSelecionada);

            Alocacao alocacao = new Alocacao();
            alocacao.setBolsa(bolsaSelecionada);
            alocacao.setRequisicao(requisicao);
            alocacao.setDataAlocacao(LocalDateTime.now());
            alocacao.setCriterio(
                    "Compatibilidade ABO/Rh e prioridade por validade FEFO");

            novasAlocacoes.add(repositorioAlocacao.save(alocacao));
        }

        int totalAlocado = quantidadeJaAlocada + quantidadeParaAlocar;
        requisicao.setEstado(totalAlocado >= requisicao.getQuantidade()
                ? EstadoRequisicao.ALOCADA
                : EstadoRequisicao.PARCIALMENTE_ALOCADA);
        repositorioRequisicao.save(requisicao);

        return novasAlocacoes;
    }

    @Transactional
    public List<ResumoRequisicao> criarResumos(List<Requisicao> requisicoes) {
        return requisicoes.stream()
                .map(requisicao -> new ResumoRequisicao(
                        requisicao,
                        repositorioAlocacao.findByRequisicao(requisicao).size(),
                        buscarBolsasCompativeisDisponiveis(requisicao).size()))
                .toList();
    }

    private List<BolsaSangue> buscarBolsasCompativeisDisponiveis(
            Requisicao requisicao) {
        return repositorioBolsa
                .findByHemocomponenteAndEstado(
                        requisicao.getHemocomponente(),
                        EstadoBolsa.DISPONIVEL)
                .stream()
                .filter(bolsa -> servicoCompatibilidade.compativel(
                        bolsa.getTipoSanguineo(),
                        requisicao.getTipoSanguineoPaciente()))
                .sorted(Comparator.comparing(BolsaSangue::getDataValidade))
                .toList();
    }
}
