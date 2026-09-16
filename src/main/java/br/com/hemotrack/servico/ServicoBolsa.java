package br.com.hemotrack.servico;

import br.com.hemotrack.modelo.BolsaSangue;
import br.com.hemotrack.modelo.EstadoBolsa;
import br.com.hemotrack.modelo.EstadoRequisicao;
import br.com.hemotrack.repositorio.RepositorioAlocacao;
import br.com.hemotrack.repositorio.RepositorioBolsa;
import br.com.hemotrack.repositorio.RepositorioRequisicao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ServicoBolsa {

    private final RepositorioBolsa repositorioBolsa;
    private final RepositorioAlocacao repositorioAlocacao;
    private final RepositorioRequisicao repositorioRequisicao;

    public ServicoBolsa(RepositorioBolsa repositorioBolsa,
                        RepositorioAlocacao repositorioAlocacao,
                        RepositorioRequisicao repositorioRequisicao) {
        this.repositorioBolsa = repositorioBolsa;
        this.repositorioAlocacao = repositorioAlocacao;
        this.repositorioRequisicao = repositorioRequisicao;
    }

    public BolsaSangue cadastrar(BolsaSangue bolsa) {
        bolsa.setQuantidadeCadastro(1);
        return cadastrarLote(bolsa).get(0);
    }

    @Transactional
    public List<BolsaSangue> cadastrarLote(BolsaSangue bolsa) {
        validarQuantidadeCadastro(bolsa);
        validarDatas(bolsa);
        validarCamposObrigatorios(bolsa);

        String codigoBase = normalizarCodigo(bolsa.getCodigo());
        List<String> codigos = gerarCodigos(
                codigoBase,
                bolsa.getQuantidadeCadastro());

        validarCodigosDisponiveis(codigos);

        List<BolsaSangue> bolsasCadastradas = new ArrayList<>();

        for (String codigo : codigos) {
            BolsaSangue novaBolsa = new BolsaSangue(
                    codigo,
                    bolsa.getHemocomponente(),
                    bolsa.getTipoSanguineo(),
                    bolsa.getDataColeta(),
                    bolsa.getDataValidade());

            bolsasCadastradas.add(repositorioBolsa.save(novaBolsa));
        }

        return bolsasCadastradas;
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O código da bolsa é obrigatório.");
        }

        return codigo.trim().toUpperCase();
    }

    private List<String> gerarCodigos(String codigoBase, int quantidade) {
        if (quantidade == 1) {
            return List.of(codigoBase);
        }

        List<String> codigos = new ArrayList<>();

        for (int numero = 1; numero <= quantidade; numero++) {
            codigos.add(String.format("%s-%03d", codigoBase, numero));
        }

        return codigos;
    }

    private void validarCodigosDisponiveis(List<String> codigos) {
        for (String codigo : codigos) {
            if (repositorioBolsa.existsByCodigo(codigo)) {
                throw new IllegalArgumentException(
                        "Já existe uma bolsa cadastrada com o código " + codigo + ".");
            }
        }
    }

    private void validarQuantidadeCadastro(BolsaSangue bolsa) {
        if (bolsa.getQuantidadeCadastro() == null
                || bolsa.getQuantidadeCadastro() <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade de bolsas deve ser maior que zero.");
        }
    }

    private void validarDatas(BolsaSangue bolsa) {
        if (bolsa.getDataColeta() == null || bolsa.getDataValidade() == null) {
            throw new IllegalArgumentException("Data de coleta e validade são obrigatórias.");
        }

        if (!bolsa.getDataValidade().isAfter(bolsa.getDataColeta())) {
            throw new IllegalArgumentException(
                    "Data de validade inválida. A data de validade deve ser posterior à data de coleta.");
        }
    }

    private void validarCamposObrigatorios(BolsaSangue bolsa) {
        if (bolsa.getHemocomponente() == null) {
            throw new IllegalArgumentException("Hemocomponente é obrigatório.");
        }

        if (bolsa.getTipoSanguineo() == null) {
            throw new IllegalArgumentException("Tipo sanguíneo é obrigatório.");
        }
    }

    public List<BolsaSangue> listarTodas() {
        List<BolsaSangue> bolsas = repositorioBolsa.findAll();
        bolsas.sort(Comparator.comparing(BolsaSangue::getDataValidade));
        return bolsas;
    }

    @Transactional
    public void remover(Long id) {
        BolsaSangue bolsa = repositorioBolsa.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Bolsa não encontrada."));

        var alocacoes = repositorioAlocacao.findByBolsa(bolsa);
        var requisicoesAfetadas = alocacoes.stream()
                .map(alocacao -> alocacao.getRequisicao())
                .distinct()
                .toList();

        repositorioAlocacao.deleteAll(alocacoes);
        repositorioAlocacao.flush();

        requisicoesAfetadas.forEach(requisicao -> {
            int quantidadeAlocada =
                    repositorioAlocacao.findByRequisicao(requisicao).size();

            if (quantidadeAlocada == 0) {
                requisicao.setEstado(EstadoRequisicao.PENDENTE_ALOCACAO);
            } else if (quantidadeAlocada < requisicao.getQuantidade()) {
                requisicao.setEstado(EstadoRequisicao.PARCIALMENTE_ALOCADA);
            } else {
                requisicao.setEstado(EstadoRequisicao.ALOCADA);
            }

            repositorioRequisicao.save(requisicao);
        });

        repositorioBolsa.delete(bolsa);
    }
}
