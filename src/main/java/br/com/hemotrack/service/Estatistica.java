package br.com.hemotrack.service;

import java.util.List;

/** Calculos estatisticos usados na projecao de demanda (US08). */
public final class Estatistica {

    private Estatistica() {
    }

    public static double media(List<Integer> valores) {
        if (valores.isEmpty()) {
            return 0;
        }
        double soma = 0;
        for (int valor : valores) {
            soma += valor;
        }
        return soma / valores.size();
    }

    /** Desvio padrao populacional. */
    public static double desvioPadrao(List<Integer> valores) {
        if (valores.isEmpty()) {
            return 0;
        }
        double media = media(valores);
        double somaQuadrados = 0;
        for (int valor : valores) {
            somaQuadrados += (valor - media) * (valor - media);
        }
        return Math.sqrt(somaQuadrados / valores.size());
    }
}
