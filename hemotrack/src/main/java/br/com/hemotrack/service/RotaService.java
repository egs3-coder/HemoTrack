package br.com.hemotrack.service;

import br.com.hemotrack.algoritmo.RoteirizadorVizinhoMaisProximo;
import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Rota;
import br.com.hemotrack.model.Trecho;
import br.com.hemotrack.repository.TrechoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US05 - Rota de entrega mais rapida ate o hospital.
 *   - A malha e formada por trechos entre pontos (hemocentro e hospitais), com tempo (min) e distancia (km).
 *   - A rota e calculada pelo vizinho mais proximo (U2) e fica registrada no pedido.
 */
@Service
public class RotaService {

    public static final String HEMOCENTRO = "Hemocentro Central";

    private final TrechoRepository trechoRepository;

    public RotaService(TrechoRepository trechoRepository) {
        this.trechoRepository = trechoRepository;
    }

    public List<Trecho> listarTrechos() {
        return trechoRepository.findAll();
    }

    @Transactional
    public Trecho cadastrarTrecho(String origem, String destino, Integer tempoMin, Double distanciaKm) {
        if (origem == null || origem.isBlank() || destino == null || destino.isBlank()) {
            throw new RegraNegocioException("Origem e destino do trecho são obrigatórios");
        }
        if (origem.trim().equalsIgnoreCase(destino.trim())) {
            throw new RegraNegocioException("Origem e destino devem ser pontos diferentes");
        }
        if (tempoMin == null || tempoMin <= 0 || distanciaKm == null || distanciaKm <= 0) {
            throw new RegraNegocioException("Tempo e distância devem ser maiores que zero");
        }
        return trechoRepository.save(new Trecho(origem.trim(), destino.trim(), tempoMin, distanciaKm));
    }

    @Transactional
    public void removerTrecho(Long id) {
        trechoRepository.deleteById(id);
    }

    public Rota calcularAteHospital(String nomeHospital) {
        return calcular(trechoRepository.findAll(), HEMOCENTRO, nomeHospital);
    }

    /**
     * U2: rota calculada pela heuristica do VIZINHO MAIS PROXIMO
     * (br.com.hemotrack.algoritmo.RoteirizadorVizinhoMaisProximo), usando o tempo como custo.
     * Nao acessa o banco, por isso pode ser testado isoladamente.
     */
    public Rota calcular(List<Trecho> trechos, String origem, String destino) {
        Rota rota = RoteirizadorVizinhoMaisProximo.calcular(trechos, origem, destino);
        if (rota == null) {
            throw new RegraNegocioException("Não existe rota cadastrada entre " + origem + " e " + destino);
        }
        return rota;
    }
}
