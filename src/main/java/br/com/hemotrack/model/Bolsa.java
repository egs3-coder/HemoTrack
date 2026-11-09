package br.com.hemotrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class Bolsa extends EntidadeBase {

    public static final String MOTIVO_VALIDADE = "Validade vencida";

    @Column(nullable = false, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Hemocomponente hemocomponente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineo;

    private LocalDate dataColeta;

    @Column(nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusBolsa status;

    private String motivoDescarte;
    private LocalDate dataDescarte;
    private LocalDate dataEntrega;

    @ManyToOne
    private Requisicao requisicao;

    protected Bolsa() {
        // exigido pelo JPA
    }

    public Bolsa(String codigo, Hemocomponente hemocomponente, TipoSanguineo tipoSanguineo,
                 LocalDate dataColeta, LocalDate dataValidade) {
        this.codigo = codigo;
        this.hemocomponente = hemocomponente;
        this.tipoSanguineo = tipoSanguineo;
        this.dataColeta = dataColeta;
        this.dataValidade = dataValidade;
        this.status = StatusBolsa.DISPONIVEL;
    }

    public boolean estaVencida(LocalDate hoje) {
        return dataValidade.isBefore(hoje);
    }

    public void reservarPara(Requisicao requisicao) {
        this.requisicao = requisicao;
        this.status = StatusBolsa.RESERVADA;
    }

    public void liberar() {
        this.requisicao = null;
        this.status = StatusBolsa.DISPONIVEL;
    }

    public void despachar() {
        this.status = StatusBolsa.EM_TRANSPORTE;
    }

    public void entregar(LocalDate data) {
        this.status = StatusBolsa.ENTREGUE;
        this.dataEntrega = data;
    }

    public void descartar(String motivo, LocalDate data) {
        this.status = StatusBolsa.DESCARTADA;
        this.motivoDescarte = motivo;
        this.dataDescarte = data;
        this.requisicao = null;
    }

    public String getCodigo() {
        return codigo;
    }

    public Hemocomponente getHemocomponente() {
        return hemocomponente;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public LocalDate getDataColeta() {
        return dataColeta;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public StatusBolsa getStatus() {
        return status;
    }

    public String getMotivoDescarte() {
        return motivoDescarte;
    }

    public LocalDate getDataDescarte() {
        return dataDescarte;
    }

    public LocalDate getDataEntrega() {
        return dataEntrega;
    }

    public Requisicao getRequisicao() {
        return requisicao;
    }
}
