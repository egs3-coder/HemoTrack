package br.com.hemotrack.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "bolsas")
public class BolsaSangue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoHemocomponente hemocomponente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineo;

    @Column(nullable = false)
    private LocalDate dataColeta;

    @Column(nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoBolsa estado;

    @Transient
    private Integer quantidadeCadastro = 1;

    public BolsaSangue() {
    }

    public BolsaSangue(String codigo,
                       TipoHemocomponente hemocomponente,
                       TipoSanguineo tipoSanguineo,
                       LocalDate dataColeta,
                       LocalDate dataValidade) {
        this.codigo = codigo;
        this.hemocomponente = hemocomponente;
        this.tipoSanguineo = tipoSanguineo;
        this.dataColeta = dataColeta;
        this.dataValidade = dataValidade;
        this.estado = EstadoBolsa.DISPONIVEL;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public TipoHemocomponente getHemocomponente() {
        return hemocomponente;
    }

    public void setHemocomponente(TipoHemocomponente hemocomponente) {
        this.hemocomponente = hemocomponente;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public void setTipoSanguineo(TipoSanguineo tipoSanguineo) {
        this.tipoSanguineo = tipoSanguineo;
    }

    public LocalDate getDataColeta() {
        return dataColeta;
    }

    public void setDataColeta(LocalDate dataColeta) {
        this.dataColeta = dataColeta;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public EstadoBolsa getEstado() {
        return estado;
    }

    public void setEstado(EstadoBolsa estado) {
        this.estado = estado;
    }

    public Integer getQuantidadeCadastro() {
        return quantidadeCadastro;
    }

    public void setQuantidadeCadastro(Integer quantidadeCadastro) {
        this.quantidadeCadastro = quantidadeCadastro;
    }
}
