package br.com.hemotrack.model;

import java.util.List;

/** Resultado do calculo de rota (objeto de valor, nao e persistido diretamente). */
public class Rota {

    private final List<String> pontos;
    private final int tempoMin;
    private final double distanciaKm;

    public Rota(List<String> pontos, int tempoMin, double distanciaKm) {
        this.pontos = List.copyOf(pontos);
        this.tempoMin = tempoMin;
        this.distanciaKm = distanciaKm;
    }

    public List<String> getPontos() {
        return pontos;
    }

    public String getItinerario() {
        return String.join(" → ", pontos);
    }

    public int getTempoMin() {
        return tempoMin;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }
}
