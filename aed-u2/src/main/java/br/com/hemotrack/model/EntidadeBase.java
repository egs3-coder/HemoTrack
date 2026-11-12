package br.com.hemotrack.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;

/** Superclasse de todas as entidades persistidas: concentra o identificador e a igualdade. */
@MappedSuperclass
public abstract class EntidadeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof EntidadeBase)) {
            return false;
        }
        EntidadeBase que = (EntidadeBase) outro;
        return id != null && id.equals(que.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
