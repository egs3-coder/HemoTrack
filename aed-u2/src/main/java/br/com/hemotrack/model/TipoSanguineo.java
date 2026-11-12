package br.com.hemotrack.model;

import br.com.hemotrack.algoritmo.MatrizCompatibilidade;
import java.util.List;

/**
 * Tipos ABO/Rh. A ORDEM das constantes e a ordem das linhas e colunas da
 * MatrizCompatibilidade (ordinal 0 = O-, ..., 7 = AB+): nao reordenar.
 */
public enum TipoSanguineo {
    O_NEG("O-"),
    O_POS("O+"),
    A_NEG("A-"),
    A_POS("A+"),
    B_NEG("B-"),
    B_POS("B+"),
    AB_NEG("AB-"),
    AB_POS("AB+");

    private final String rotulo;

    TipoSanguineo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Regra de hemacias, consultada na matriz 8x8 [doador][receptor]. O(1). */
    public boolean podeReceberDe(TipoSanguineo doador) {
        return MatrizCompatibilidade.compativel(doador, this);
    }

    /** Doadores de hemacias aceitos por este receptor (coluna da matriz). */
    public List<TipoSanguineo> doadoresCompativeis() {
        return List.of(MatrizCompatibilidade.doadoresCompativeis(Hemocomponente.CONCENTRADO_HEMACIAS, this));
    }
}
