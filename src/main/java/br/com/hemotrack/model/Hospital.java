package br.com.hemotrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Hospital extends EntidadeBase {

    @Column(nullable = false)
    private String nome;

    /** Guardado apenas com os 14 digitos. */
    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(nullable = false)
    private String regiao;

    private boolean ativo;

    protected Hospital() {
        // exigido pelo JPA
    }

    public Hospital(String nome, String cnpj, String regiao) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.regiao = regiao;
        this.ativo = true;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void inativar() {
        this.ativo = false;
    }

    public String getNome() {
        return nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getCnpjFormatado() {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8)
                + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }

    public String getRegiao() {
        return regiao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public String getSituacao() {
        return ativo ? "Ativo" : "Inativo";
    }
}
