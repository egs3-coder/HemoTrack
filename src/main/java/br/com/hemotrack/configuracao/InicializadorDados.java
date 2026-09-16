package br.com.hemotrack.configuracao;

import br.com.hemotrack.modelo.Hospital;
import br.com.hemotrack.repositorio.RepositorioHospital;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InicializadorDados implements CommandLineRunner {

    private final RepositorioHospital repositorioHospital;

    public InicializadorDados(RepositorioHospital repositorioHospital) {
        this.repositorioHospital = repositorioHospital;
    }

    @Override
    public void run(String... argumentos) {
        if (repositorioHospital.count() == 0) {
            repositorioHospital.save(
                    new Hospital("Hospital Santa Clara", "Recife"));

            repositorioHospital.save(
                    new Hospital("Hospital Regional Central", "Recife"));
        }
    }
}
