package br.com.hemotrack.repositorio;

import br.com.hemotrack.modelo.BolsaSangue;
import br.com.hemotrack.modelo.EstadoBolsa;
import br.com.hemotrack.modelo.TipoHemocomponente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RepositorioBolsa extends JpaRepository<BolsaSangue, Long> {

    boolean existsByCodigo(String codigo);

    List<BolsaSangue> findByHemocomponenteAndEstado(
            TipoHemocomponente hemocomponente,
            EstadoBolsa estado
    );
}
