package br.com.hemotrack.service;

import java.util.Map;

/** Indicadores consolidados de um periodo (US07). */
public class Dashboard {

    private final int dias;
    private final long bolsasEntregues;
    private final long bolsasDescartadas;
    private final long pedidosEntregues;
    private final long pedidosNoPrazo;
    private final Map<String, Long> tempoMedioPorRegiao;

    public Dashboard(int dias, long bolsasEntregues, long bolsasDescartadas, long pedidosEntregues,
                     long pedidosNoPrazo, Map<String, Long> tempoMedioPorRegiao) {
        this.dias = dias;
        this.bolsasEntregues = bolsasEntregues;
        this.bolsasDescartadas = bolsasDescartadas;
        this.pedidosEntregues = pedidosEntregues;
        this.pedidosNoPrazo = pedidosNoPrazo;
        this.tempoMedioPorRegiao = tempoMedioPorRegiao;
    }

    public boolean isVazio() {
        return bolsasEntregues == 0 && bolsasDescartadas == 0 && pedidosEntregues == 0;
    }

    /** Descartadas por validade / bolsas que sairam do estoque, em %. */
    public long getTaxaDescarte() {
        long saidas = bolsasEntregues + bolsasDescartadas;
        return saidas == 0 ? 0 : Math.round(bolsasDescartadas * 100.0 / saidas);
    }

    public boolean isTemPedidos() {
        return pedidosEntregues > 0;
    }

    /** Pedidos entregues dentro do tempo previsto / pedidos entregues, em %. */
    public long getSla() {
        return pedidosEntregues == 0 ? 0 : Math.round(pedidosNoPrazo * 100.0 / pedidosEntregues);
    }

    public int getDias() {
        return dias;
    }

    public long getBolsasEntregues() {
        return bolsasEntregues;
    }

    public long getBolsasDescartadas() {
        return bolsasDescartadas;
    }

    public long getPedidosEntregues() {
        return pedidosEntregues;
    }

    public long getPedidosNoPrazo() {
        return pedidosNoPrazo;
    }

    public Map<String, Long> getTempoMedioPorRegiao() {
        return tempoMedioPorRegiao;
    }
}
