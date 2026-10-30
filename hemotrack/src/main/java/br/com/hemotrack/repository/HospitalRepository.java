package br.com.hemotrack.repository;

import br.com.hemotrack.model.Hospital;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    boolean existsByCnpj(String cnpj);

    List<Hospital> findAllByOrderByNomeAsc();

    List<Hospital> findByAtivoTrueOrderByNomeAsc();
}
