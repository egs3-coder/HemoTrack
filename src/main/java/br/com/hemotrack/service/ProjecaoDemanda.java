package br.com.hemotrack.service;

import br.com.hemotrack.model.TipoSanguineo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * US08 - Projecao de demanda de um tipo sanguineo.
 *
 * Regras da historia:
 *   - A projecao do proximo mes e a media (mu) do consumo dos ultimos 3 meses;
 *     a dispersao e o desvio padrao (sigma) desses 3 valores.
 *   - Um tipo esta em "Risco de ruptura" quando o estoque disponivel e menor que mu + sigma.
 *   - Sem consumo registrado nos 3 meses: "Historico insuficiente para projecao (minimo de 3 meses)".
 *
 * Indicadores complementares (apoiam o objetivo da historia: direcionar campanhas de doacao),
 * todos derivados de mu, sigma e do estoque:
 *   - Nivel de seguranca = mu + sigma (o estoque minimo para nao estar em risco).
 *   - Bolsas a captar = quanto falta para o estoque alcancar mu + sigma.
 *   - Cobertura = por quantos dias o estoque atende o consumo medio (mes de 30 dias).
 *   - Tendencia = comparacao entre o ultimo e o primeiro mes analisado.
 *   - Variabilidade = coeficiente de variacao (sigma / mu).
 *   - Prioridade = "Critica" quando o estoque nao cobre nem a media; "Alta" quando cobre a media
 *     mas nao mu + sigma.
 */
public class ProjecaoDemanda {

    public static final int MESES_NECESSARIOS = 3;
    public static final int DIAS_DO_MES = 30;
    /** Variacao percentual entre o primeiro e o ultimo mes considerada estavel. */
    static final double FAIXA_ESTAVEL = 0.10;

    public static final String RISCO_RUPTURA = "Risco de ruptura";
    public static final String ESTOQUE_ADEQUADO = "Estoque adequado";
    public static final String HISTORICO_INSUFICIENTE = "Histórico insuficiente para projeção (mínimo de 3 meses)";

    private static final Locale PT_BR = new Locale("pt", "BR");

    private final TipoSanguineo tipo;
    private final List<Integer> consumoMensal;
    private final long estoqueDisponivel;
    private final double media;
    private final double desvio;

    public ProjecaoDemanda(TipoSanguineo tipo, List<Integer> consumoMensal, long estoqueDisponivel) {
        this.tipo = tipo;
        this.consumoMensal = List.copyOf(consumoMensal);
        this.estoqueDisponivel = estoqueDisponivel;
        this.media = Estatistica.media(consumoMensal);
        this.desvio = Estatistica.desvioPadrao(consumoMensal);
    }

    // ---------------- Regras da historia ----------------

    public int getMesesComConsumo() {
        int meses = 0;
        for (int consumo : consumoMensal) {
            if (consumo > 0) {
                meses++;
            }
        }
        return meses;
    }

    /** Exige consumo registrado em todos os 3 meses analisados (cenario 2). */
    public boolean isHistoricoSuficiente() {
        return getMesesComConsumo() >= MESES_NECESSARIOS;
    }

    /** Cenario 1: estoque disponivel menor que mu + sigma. */
    public boolean isRiscoRuptura() {
        return isHistoricoSuficiente() && estoqueDisponivel < getNivelSeguranca();
    }

    public String getSituacao() {
        if (!isHistoricoSuficiente()) {
            return HISTORICO_INSUFICIENTE;
        }
        return isRiscoRuptura() ? RISCO_RUPTURA : ESTOQUE_ADEQUADO;
    }

    /** Classe CSS da situacao: risco, adequado ou insuficiente. */
    public String getCategoria() {
        if (!isHistoricoSuficiente()) {
            return "insuficiente";
        }
        return isRiscoRuptura() ? "risco" : "adequado";
    }

    // ---------------- Indicadores complementares ----------------

    /** mu + sigma. */
    public double getNivelSeguranca() {
        return media + desvio;
    }

    /** Bolsas que faltam para o estoque alcancar mu + sigma (0 quando adequado). */
    public long getBolsasACaptar() {
        if (!isHistoricoSuficiente()) {
            return 0;
        }
        return Math.max(0, (long) Math.ceil(getNivelSeguranca() - estoqueDisponivel));
    }

    /** Dias de consumo medio que o estoque atual atende (null sem historico). */
    public Long getCoberturaDias() {
        if (!isHistoricoSuficiente() || media <= 0) {
            return null;
        }
        return Math.round(estoqueDisponivel / media * DIAS_DO_MES);
    }

    /** "Alta", "Queda" ou "Estável" comparando o ultimo com o primeiro mes analisado. */
    public String getTendencia() {
        if (!isHistoricoSuficiente()) {
            return "-";
        }
        int primeiro = consumoMensal.get(0);
        int ultimo = consumoMensal.get(consumoMensal.size() - 1);
        double variacao = (ultimo - primeiro) / (double) primeiro;
        if (variacao > FAIXA_ESTAVEL) {
            return "Alta";
        }
        if (variacao < -FAIXA_ESTAVEL) {
            return "Queda";
        }
        return "Estável";
    }

    public String getSimboloTendencia() {
        switch (getTendencia()) {
            case "Alta":
                return "▲";
            case "Queda":
                return "▼";
            case "Estável":
                return "▬";
            default:
                return "";
        }
    }

    /** Coeficiente de variacao sigma / mu, em %. */
    public long getCoeficienteVariacao() {
        return media <= 0 ? 0 : Math.round(desvio / media * 100);
    }

    /** "Baixa" (ate 10%), "Moderada" (ate 25%) ou "Alta". */
    public String getVariabilidade() {
        if (!isHistoricoSuficiente()) {
            return "-";
        }
        long cv = getCoeficienteVariacao();
        if (cv <= 10) {
            return "Baixa";
        }
        return cv <= 25 ? "Moderada" : "Alta";
    }

    /** "Crítica", "Alta" ou "" (sem prioridade de campanha). */
    public String getPrioridade() {
        if (!isRiscoRuptura()) {
            return "";
        }
        return estoqueDisponivel < media ? "Crítica" : "Alta";
    }

    // ---------------- Apoio ao grafico ----------------

    /** Altura (0 a 100) de cada mes no mini grafico, relativa ao maior consumo da linha. */
    public List<Integer> getAlturasConsumo() {
        int maior = 0;
        for (int consumo : consumoMensal) {
            maior = Math.max(maior, consumo);
        }
        List<Integer> alturas = new ArrayList<>();
        for (int consumo : consumoMensal) {
            alturas.add(maior == 0 ? 0 : (int) Math.round(consumo * 100.0 / maior));
        }
        return alturas;
    }

    /** Escala comum da barra "estoque x nivel de seguranca". */
    private double escala() {
        return Math.max(estoqueDisponivel, getNivelSeguranca()) * 1.15;
    }

    /** Largura (0 a 100) do estoque na barra. */
    public int getLarguraEstoque() {
        double escala = escala();
        return escala <= 0 ? 0 : (int) Math.round(estoqueDisponivel / escala * 100);
    }

    /** Posicao (0 a 100) do marcador mu + sigma na barra. */
    public int getPosicaoNivelSeguranca() {
        double escala = escala();
        return escala <= 0 ? 0 : (int) Math.round(getNivelSeguranca() / escala * 100);
    }

    /** Largura (0 a 100) da barra da media, relativa ao maior valor exibido na tabela. */
    public int larguraBarra(double maiorValor) {
        if (maiorValor <= 0) {
            return 0;
        }
        return (int) Math.round(media / maiorValor * 100);
    }

    // ---------------- Getters e formatacao ----------------

    public TipoSanguineo getTipo() {
        return tipo;
    }

    public List<Integer> getConsumoMensal() {
        return consumoMensal;
    }

    public long getEstoqueDisponivel() {
        return estoqueDisponivel;
    }

    public double getMedia() {
        return media;
    }

    public double getDesvio() {
        return desvio;
    }

    public String getMediaFormatada() {
        return String.format(PT_BR, "%.1f", media);
    }

    public String getDesvioFormatado() {
        return String.format(PT_BR, "%.2f", desvio);
    }

    public String getNivelSegurancaFormatado() {
        return String.format(PT_BR, "%.1f", getNivelSeguranca());
    }
}
