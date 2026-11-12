package br.com.hemotrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/** Ligacao (de mao dupla) entre dois pontos da malha: hemocentro ou hospitais. */
@Entity
public class Trecho extends EntidadeBase {

    @Column(nullable = false)
    private String origem;

    @Column(nullable = false)
    private String destino;

    private int tempoMin;
    private double distanciaKm;

    protected Trecho() {
        // exigido pelo JPA
    }

    public Trecho(String origem, String destino, int tempoMin, double distanciaKm) {
        this.origem = origem;
        this.destino = destino;
        this.tempoMin = tempoMin;
        this.distanciaKm = distanciaKm;
    }

    public String getOrigem() {
        return origem;
    }

    public String getDestino() {
        return destino;
    }

    public int getTempoMin() {
        return tempoMin;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }
}
