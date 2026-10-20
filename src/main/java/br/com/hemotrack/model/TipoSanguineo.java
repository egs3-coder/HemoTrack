package br.com.hemotrack.model;

import java.util.ArrayList;
import java.util.List;

public enum TipoSanguineo {
    O_NEG("O-", false, false, false),
    O_POS("O+", false, false, true),
    A_NEG("A-", true, false, false),
    A_POS("A+", true, false, true),
    B_NEG("B-", false, true, false),
    B_POS("B+", false, true, true),
    AB_NEG("AB-", true, true, false),
    AB_POS("AB+", true, true, true);

    private final String rotulo;
    private final boolean antigenoA;
    private final boolean antigenoB;
    private final boolean rhPositivo;

    TipoSanguineo(String rotulo, boolean antigenoA, boolean antigenoB, boolean rhPositivo) {
        this.rotulo = rotulo;
        this.antigenoA = antigenoA;
        this.antigenoB = antigenoB;
        this.rhPositivo = rhPositivo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Um paciente so pode receber sangue que nao tenha antigenos (A, B, Rh) que ele nao possui. */
    public boolean podeReceberDe(TipoSanguineo doador) {
        return (!doador.antigenoA || this.antigenoA)
                && (!doador.antigenoB || this.antigenoB)
                && (!doador.rhPositivo || this.rhPositivo);
    }

    public List<TipoSanguineo> doadoresCompativeis() {
        List<TipoSanguineo> compativeis = new ArrayList<>();
        for (TipoSanguineo doador : values()) {
            if (podeReceberDe(doador)) {
                compativeis.add(doador);
            }
        }
        return compativeis;
    }
}
