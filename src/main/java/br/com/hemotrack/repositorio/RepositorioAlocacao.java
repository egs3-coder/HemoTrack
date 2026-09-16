package br.com.hemotrack.repositorio;

import br.com.hemotrack.modelo.Alocacao;
import br.com.hemotrack.modelo.BolsaSangue;
import br.com.hemotrack.modelo.Requisicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepositorioAlocacao extends JpaRepository<Alocacao, Long> {

    List<Alocacao> findByRequisicao(Requisicao requisicao);

    List<Alocacao> findByBolsa(BolsaSangue bolsa);
}
