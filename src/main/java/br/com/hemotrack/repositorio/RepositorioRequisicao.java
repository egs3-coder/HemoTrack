package br.com.hemotrack.repositorio;

import br.com.hemotrack.modelo.Requisicao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioRequisicao extends JpaRepository<Requisicao, Long> {
}
