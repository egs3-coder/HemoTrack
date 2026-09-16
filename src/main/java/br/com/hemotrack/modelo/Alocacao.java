package br.com.hemotrack.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alocacoes")
public class Alocacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requisicao_id", nullable = false)
    private Requisicao requisicao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bolsa_id", nullable = false)
    private BolsaSangue bolsa;

    @Column(nullable = false)
    private LocalDateTime dataAlocacao;

    @Column(nullable = false)
    private String criterio;

    public Alocacao() {
    }

    public Long getId() {
        return id;
    }

    public Requisicao getRequisicao() {
        return requisicao;
    }

    public void setRequisicao(Requisicao requisicao) {
        this.requisicao = requisicao;
    }

    public BolsaSangue getBolsa() {
        return bolsa;
    }

    public void setBolsa(BolsaSangue bolsa) {
        this.bolsa = bolsa;
    }

    public LocalDateTime getDataAlocacao() {
        return dataAlocacao;
    }

    public void setDataAlocacao(LocalDateTime dataAlocacao) {
        this.dataAlocacao = dataAlocacao;
    }

    public String getCriterio() {
        return criterio;
    }

    public void setCriterio(String criterio) {
        this.criterio = criterio;
    }
}
