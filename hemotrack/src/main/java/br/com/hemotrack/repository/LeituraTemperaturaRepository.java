package br.com.hemotrack.repository;

import br.com.hemotrack.model.LeituraTemperatura;
import br.com.hemotrack.model.Requisicao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeituraTemperaturaRepository extends JpaRepository<LeituraTemperatura, Long> {

    List<LeituraTemperatura> findByRequisicaoOrderByIdDesc(Requisicao requisicao);

    Optional<LeituraTemperatura> findFirstByRequisicaoOrderByIdDesc(Requisicao requisicao);
}
