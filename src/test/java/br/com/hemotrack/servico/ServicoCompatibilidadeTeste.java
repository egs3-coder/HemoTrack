package br.com.hemotrack.servico;

import br.com.hemotrack.modelo.TipoSanguineo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServicoCompatibilidadeTeste {

    private final ServicoCompatibilidade servico =
            new ServicoCompatibilidade();

    @Test
    void deveAceitarBPositivoParaPacienteBPositivo() {
        assertTrue(
                servico.compativel(
                        TipoSanguineo.B_POSITIVO,
                        TipoSanguineo.B_POSITIVO));
    }

    @Test
    void deveAceitarONegativoParaPacienteBPositivo() {
        assertTrue(
                servico.compativel(
                        TipoSanguineo.O_NEGATIVO,
                        TipoSanguineo.B_POSITIVO));
    }

    @Test
    void deveRejeitarAPositivoParaPacienteBPositivo() {
        assertFalse(
                servico.compativel(
                        TipoSanguineo.A_POSITIVO,
                        TipoSanguineo.B_POSITIVO));
    }
}
