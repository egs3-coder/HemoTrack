package br.com.hemotrack.repository;

import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusRequisicao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequisicaoRepository extends JpaRepository<Requisicao, Long> {

    Optional<Requisicao> findByCodigo(String codigo);

    long countByStatus(StatusRequisicao status);

    List<Requisicao> findAllByOrderByDataCriacaoDesc();

    List<Requisicao> findByStatusAndDataEntregaBetween(StatusRequisicao status, LocalDateTime inicio, LocalDateTime fim);
}
