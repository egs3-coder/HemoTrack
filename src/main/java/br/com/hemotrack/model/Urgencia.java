
package br.com.hemotrack.model;

public enum Urgencia {
    ROTINA("Rotina"),
    URGENTE("Urgente"),
    EMERGENCIA("Emergência");

    private final String rotulo;

    Urgencia(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
