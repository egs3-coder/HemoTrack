package br.com.hemotrack.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "requisicoes")
public class Requisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigoRastreamento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoHemocomponente hemocomponente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineoPaciente;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private String urgencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRequisicao estado;

    @Column(nullable = false)
    private LocalDateTime dataSolicitacao;

    public Requisicao() {
    }

    public Long getId() {
        return id;
    }

    public String getCodigoRastreamento() {
        return codigoRastreamento;
    }

    public void setCodigoRastreamento(String codigoRastreamento) {
        this.codigoRastreamento = codigoRastreamento;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public TipoHemocomponente getHemocomponente() {
        return hemocomponente;
    }

    public void setHemocomponente(TipoHemocomponente hemocomponente) {
        this.hemocomponente = hemocomponente;
    }

    public TipoSanguineo getTipoSanguineoPaciente() {
        return tipoSanguineoPaciente;
    }

    public void setTipoSanguineoPaciente(TipoSanguineo tipoSanguineoPaciente) {
        this.tipoSanguineoPaciente = tipoSanguineoPaciente;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(String urgencia) {
        this.urgencia = urgencia;
    }

    public EstadoRequisicao getEstado() {
        return estado;
    }

    public void setEstado(EstadoRequisicao estado) {
        this.estado = estado;
    }

    public LocalDateTime getDataSolicitacao() {
        return dataSolicitacao;
    }

    public void setDataSolicitacao(LocalDateTime dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }
}
