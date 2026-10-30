package br.com.hemotrack.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
public class LeituraTemperatura extends EntidadeBase {

    @ManyToOne(optional = false)
    private Requisicao requisicao;

    private double temperatura;
    private LocalDateTime dataHora;
    private boolean foraDaFaixa;

    protected LeituraTemperatura() {
        // exigido pelo JPA
    }

    public LeituraTemperatura(Requisicao requisicao, double temperatura, boolean foraDaFaixa) {
        this.requisicao = requisicao;
        this.temperatura = temperatura;
        this.foraDaFaixa = foraDaFaixa;
        this.dataHora = LocalDateTime.now();
    }

    public static String formatar(double temperatura) {
        return String.format(new Locale("pt", "BR"), "%.1f °C", temperatura);
    }

    public Requisicao getRequisicao() {
        return requisicao;
    }

    public double getTemperatura() {
        return temperatura;
    }

    public String getTemperaturaFormatada() {
        return formatar(temperatura);
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public boolean isForaDaFaixa() {
        return foraDaFaixa;
    }
}
