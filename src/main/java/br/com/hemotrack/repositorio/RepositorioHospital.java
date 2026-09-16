package br.com.hemotrack.repositorio;

import br.com.hemotrack.modelo.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioHospital extends JpaRepository<Hospital, Long> {
}
