package br.com.hemotrack.servico;

import br.com.hemotrack.modelo.TipoSanguineo;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class ServicoCompatibilidade {

    private final Map<TipoSanguineo, Set<TipoSanguineo>> doadoresCompativeis;

    public ServicoCompatibilidade() {
        doadoresCompativeis = new EnumMap<>(TipoSanguineo.class);

        doadoresCompativeis.put(TipoSanguineo.O_NEGATIVO,
                EnumSet.of(TipoSanguineo.O_NEGATIVO));

        doadoresCompativeis.put(TipoSanguineo.O_POSITIVO,
                EnumSet.of(
                        TipoSanguineo.O_NEGATIVO,
                        TipoSanguineo.O_POSITIVO));

        doadoresCompativeis.put(TipoSanguineo.A_NEGATIVO,
                EnumSet.of(
                        TipoSanguineo.O_NEGATIVO,
                        TipoSanguineo.A_NEGATIVO));

        doadoresCompativeis.put(TipoSanguineo.A_POSITIVO,
                EnumSet.of(
                        TipoSanguineo.O_NEGATIVO,
                        TipoSanguineo.O_POSITIVO,
                        TipoSanguineo.A_NEGATIVO,
                        TipoSanguineo.A_POSITIVO));

        doadoresCompativeis.put(TipoSanguineo.B_NEGATIVO,
                EnumSet.of(
                        TipoSanguineo.O_NEGATIVO,
                        TipoSanguineo.B_NEGATIVO));

        doadoresCompativeis.put(TipoSanguineo.B_POSITIVO,
                EnumSet.of(
                        TipoSanguineo.O_NEGATIVO,
                        TipoSanguineo.O_POSITIVO,
                        TipoSanguineo.B_NEGATIVO,
                        TipoSanguineo.B_POSITIVO));

        doadoresCompativeis.put(TipoSanguineo.AB_NEGATIVO,
                EnumSet.of(
                        TipoSanguineo.O_NEGATIVO,
                        TipoSanguineo.A_NEGATIVO,
                        TipoSanguineo.B_NEGATIVO,
                        TipoSanguineo.AB_NEGATIVO));

        doadoresCompativeis.put(TipoSanguineo.AB_POSITIVO,
                EnumSet.allOf(TipoSanguineo.class));
    }

    public boolean compativel(TipoSanguineo tipoBolsa, TipoSanguineo tipoPaciente) {
        if (tipoBolsa == null || tipoPaciente == null) {
            return false;
        }

        return doadoresCompativeis.get(tipoPaciente).contains(tipoBolsa);
    }
}
