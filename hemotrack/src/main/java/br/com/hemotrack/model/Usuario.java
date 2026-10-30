package br.com.hemotrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** US02 - Pessoa que acessa a plataforma. A senha nunca e guardada em texto puro, apenas o seu hash. */
@Entity
public class Usuario extends EntidadeBase {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;

    protected Usuario() {
        // exigido pelo JPA
    }

    public Usuario(String nome, String login, String senha, Perfil perfil) {
        this.nome = nome;
        this.login = login;
        this.senhaHash = gerarHash(senha);
        this.perfil = perfil;
    }

    public boolean senhaConfere(String senha) {
        return senha != null && senhaHash.equals(gerarHash(senha));
    }

    /** US02 (recuperacao de acesso): confere o nome completo informado, ignorando maiusculas e espacos extras. */
    public boolean nomeConfere(String nomeInformado) {
        return nomeInformado != null && normalizar(nome).equals(normalizar(nomeInformado));
    }

    /** US02 (recuperacao de acesso): troca a senha guardando apenas o novo hash. */
    public void alterarSenha(String novaSenha) {
        this.senhaHash = gerarHash(novaSenha);
    }

    private static String normalizar(String texto) {
        return texto.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static String gerarHash(String senha) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(senha.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }

    public String getNome() {
        return nome;
    }

    public String getLogin() {
        return login;
    }

    public Perfil getPerfil() {
        return perfil;
    }
}
