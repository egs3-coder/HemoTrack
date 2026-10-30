package br.com.hemotrack.model;

public enum Perfil {
    PROFISSIONAL_HOSPITALAR("Profissional hospitalar"),
    OPERADOR_HEMOCENTRO("Operador do hemocentro"),
    COORDENADOR_LOGISTICA("Coordenador de logística"),
    GESTOR("Gestor do hemocentro");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
