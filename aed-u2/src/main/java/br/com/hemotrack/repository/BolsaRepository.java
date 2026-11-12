package br.com.hemotrack.repository;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusBolsa;
import br.com.hemotrack.model.TipoSanguineo;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BolsaRepository extends JpaRepository<Bolsa, Long> {

    boolean existsByCodigo(String codigo);

    List<Bolsa> findAllByOrderByDataValidadeAsc();

    long countByStatusAndTipoSanguineo(StatusBolsa status, TipoSanguineo tipoSanguineo);

    /** Saldo utilizavel: bolsas "Disponivel" que ainda nao venceram (validade hoje ou depois). */
    
    long countByStatusAndTipoSanguineoAndDataValidadeGreaterThanEqual(
            StatusBolsa status, TipoSanguineo tipoSanguineo, LocalDate hoje);

    /** Painel: bolsas disponiveis que vencem dentro de uma janela de dias (risco de descarte). */
    
    long countByStatusAndDataValidadeBetween(StatusBolsa status, LocalDate inicio, LocalDate fim);

    /**
     * US04: bolsas com o status pedido, SEM ordenacao do banco. A compatibilidade (matriz 8x8),
     * o indice (tabela hash) e a ordem FEFO (insercao ordenada) sao feitos no Java, pelo pacote
     * br.com.hemotrack.algoritmo.
     */
    List<Bolsa> findByStatus(StatusBolsa status);

    /** US10: bolsas ainda no estoque cuja validade ja passou. */
    
    List<Bolsa> findByStatusInAndDataValidadeBefore(Collection<StatusBolsa> status, LocalDate hoje);

    List<Bolsa> findByRequisicao(Requisicao requisicao);

    List<Bolsa> findByDataEntregaBetween(LocalDate inicio, LocalDate fim);

    long countByDataEntregaBetween(LocalDate inicio, LocalDate fim);

    long countByDataDescarteBetween(LocalDate inicio, LocalDate fim);

    /** US07: apenas descartes por validade entram na taxa de descarte. */
    
    long countByMotivoDescarteAndDataDescarteBetween(String motivoDescarte, LocalDate inicio, LocalDate fim);

    /** Filtro combinado de bolsas para a tela de estoque, respeitando a regra FEFO (validade mais próxima primeiro). */
    @org.springframework.data.jpa.repository.Query("SELECT b FROM Bolsa b WHERE "
            + "(:tipoSanguineo IS NULL OR b.tipoSanguineo = :tipoSanguineo) AND "
            + "(:status IS NULL OR b.status = :status) AND "
            + "(:hemocomponente IS NULL OR b.hemocomponente = :hemocomponente) "
            + "ORDER BY b.dataValidade ASC")
    List<Bolsa> filtrar(@org.springframework.data.repository.query.Param("tipoSanguineo") TipoSanguineo tipoSanguineo,
                        @org.springframework.data.repository.query.Param("status") StatusBolsa status,
                        @org.springframework.data.repository.query.Param("hemocomponente") Hemocomponente hemocomponente);
}
