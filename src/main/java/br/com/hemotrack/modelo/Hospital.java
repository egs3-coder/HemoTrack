package br.com.hemotrack.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "hospitais")
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cidade;

    public Hospital() {
    }

    public Hospital(String nome, String cidade) {
        this.nome = nome;
        this.cidade = cidade;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }
}
