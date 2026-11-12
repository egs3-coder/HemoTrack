package br.com.hemotrack.service;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Rota;
import br.com.hemotrack.model.Trecho;
import br.com.hemotrack.repository.TrechoRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US05 - Rota de entrega mais rapida ate o hospital.
 *   - A malha e formada por trechos entre pontos (hemocentro e hospitais), com tempo (min) e distancia (km).
 *   - A rota escolhida e a de menor tempo total; a rota fica registrada no pedido.
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
     * Algoritmo de caminho minimo (Dijkstra) usando o tempo como custo.
     * Nao acessa o banco, por isso pode ser testado isoladamente.
     */
    public Rota calcular(List<Trecho> trechos, String origem, String destino) {
        Map<String, List<Aresta>> grafo = new HashMap<>();
        for (Trecho trecho : trechos) {
            grafo.computeIfAbsent(trecho.getOrigem(), chave -> new ArrayList<>())
                    .add(new Aresta(trecho.getDestino(), trecho.getTempoMin(), trecho.getDistanciaKm()));
            grafo.computeIfAbsent(trecho.getDestino(), chave -> new ArrayList<>())
                    .add(new Aresta(trecho.getOrigem(), trecho.getTempoMin(), trecho.getDistanciaKm()));
        }

        Map<String, Integer> menorTempo = new HashMap<>();
        Map<String, Double> distancia = new HashMap<>();
        Map<String, String> anterior = new HashMap<>();
        Set<String> visitados = new HashSet<>();
        PriorityQueue<Estado> fila = new PriorityQueue<>(Comparator.comparingInt(Estado::tempo));

        menorTempo.put(origem, 0);
        distancia.put(origem, 0.0);
        fila.add(new Estado(origem, 0));

        while (!fila.isEmpty()) {
            Estado atual = fila.poll();
            if (!visitados.add(atual.ponto())) {
                continue;
            }
            if (atual.ponto().equals(destino)) {
                break;
            }
            for (Aresta aresta : grafo.getOrDefault(atual.ponto(), List.of())) {
                int novoTempo = atual.tempo() + aresta.tempoMin();
                if (novoTempo < menorTempo.getOrDefault(aresta.para(), Integer.MAX_VALUE)) {
                    menorTempo.put(aresta.para(), novoTempo);
                    distancia.put(aresta.para(), distancia.get(atual.ponto()) + aresta.distanciaKm());
                    anterior.put(aresta.para(), atual.ponto());
                    fila.add(new Estado(aresta.para(), novoTempo));
                }
            }
        }

        if (!menorTempo.containsKey(destino)) {
            throw new RegraNegocioException("Não existe rota cadastrada entre " + origem + " e " + destino);
        }

        List<String> pontos = new ArrayList<>();
        for (String ponto = destino; ponto != null; ponto = anterior.get(ponto)) {
            pontos.add(ponto);
        }
        Collections.reverse(pontos);
        return new Rota(pontos, menorTempo.get(destino), distancia.get(destino));
    }

    /** Ligacao de um ponto ate outro, com o custo de percorre-la. */
    private static final class Aresta {

        private final String para;
        private final int tempoMin;
        private final double distanciaKm;

        private Aresta(String para, int tempoMin, double distanciaKm) {
            this.para = para;
            this.tempoMin = tempoMin;
            this.distanciaKm = distanciaKm;
        }

        private String para() {
            return para;
        }

        private int tempoMin() {
            return tempoMin;
        }

        private double distanciaKm() {
            return distanciaKm;
        }
    }

    /** Ponto alcancado e o tempo acumulado ate ele (item da fila de prioridade). */
    private static final class Estado {

        private final String ponto;
        private final int tempo;

        private Estado(String ponto, int tempo) {
            this.ponto = ponto;
            this.tempo = tempo;
        }

        private String ponto() {
            return ponto;
        }

        private int tempo() {
            return tempo;
        }
    }
}
