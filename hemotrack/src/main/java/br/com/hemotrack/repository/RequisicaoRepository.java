package br.com.hemotrack.repository;

import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.model.Urgencia;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequisicaoRepository extends JpaRepository<Requisicao, Long> {

    Optional<Requisicao> findByCodigo(String codigo);

    /** US01: ultima requisicao cadastrada, usada para gerar o proximo identificador REQ-xxxx. */
    Optional<Requisicao> findTopByOrderByIdDesc();

    long countByStatus(StatusRequisicao status);

    List<Requisicao> findAllByOrderByDataCriacaoDesc();

    /**
     * Lista todas as requisições ordenadas por prioridade de urgência
     * (Emergência > Urgente > Rotina) e mais recentes primeiro (dataCriacao DESC).
     */
    @Query("SELECT r FROM Requisicao r "
            + "ORDER BY CASE r.urgencia "
            + "  WHEN br.com.hemotrack.model.Urgencia.EMERGENCIA THEN 1 "
            + "  WHEN br.com.hemotrack.model.Urgencia.URGENTE THEN 2 "
            + "  WHEN br.com.hemotrack.model.Urgencia.ROTINA THEN 3 "
            + "  ELSE 4 END ASC, r.dataCriacao DESC")
    List<Requisicao> findAllByOrderByPrioridadeUrgencia();

    List<Requisicao> findByStatusAndDataEntregaBetween(StatusRequisicao status, LocalDateTime inicio, LocalDateTime fim);

    /**
     * Filtro combinado de requisições por identificador (código), urgência e status.
     * Ordena por prioridade de urgência (Emergência > Urgente > Rotina) e data de criação decrescente.
     */
    @Query("SELECT r FROM Requisicao r WHERE "
            + "(:codigo IS NULL OR UPPER(TRIM(r.codigo)) LIKE UPPER(CONCAT('%', TRIM(:codigo), '%'))) AND "
            + "(:urgencia IS NULL OR r.urgencia = :urgencia) AND "
            + "(:status IS NULL OR r.status = :status) "
            + "ORDER BY CASE r.urgencia "
            + "  WHEN br.com.hemotrack.model.Urgencia.EMERGENCIA THEN 1 "
            + "  WHEN br.com.hemotrack.model.Urgencia.URGENTE THEN 2 "
            + "  WHEN br.com.hemotrack.model.Urgencia.ROTINA THEN 3 "
            + "  ELSE 4 END ASC, r.dataCriacao DESC")
    List<Requisicao> filtrar(@Param("codigo") String codigo,
                             @Param("urgencia") Urgencia urgencia,
                             @Param("status") StatusRequisicao status);
}
