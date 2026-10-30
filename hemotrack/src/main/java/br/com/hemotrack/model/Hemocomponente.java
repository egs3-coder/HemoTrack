package br.com.hemotrack.model;

/** Cada hemocomponente conhece a sua propria faixa segura de temperatura. */
public enum Hemocomponente {
    CONCENTRADO_HEMACIAS("Concentrado de Hemácias", 2.0, 6.0),
    PLAQUETAS("Plaquetas", 20.0, 24.0),
    PLASMA("Plasma Fresco Congelado", -40.0, -18.0),
    CRIOPRECIPITADO("Crioprecipitado", -40.0, -18.0);

    private final String rotulo;
    private final double temperaturaMinima;
    private final double temperaturaMaxima;

    Hemocomponente(String rotulo, double temperaturaMinima, double temperaturaMaxima) {
        this.rotulo = rotulo;
        this.temperaturaMinima = temperaturaMinima;
        this.temperaturaMaxima = temperaturaMaxima;
    }

    public String getRotulo() {
        return rotulo;
    }

    public double getTemperaturaMinima() {
        return temperaturaMinima;
    }

    public double getTemperaturaMaxima() {
        return temperaturaMaxima;
    }

    /** US06: "2,0 °C a 6,0 °C". */
    public String getFaixaFormatada() {
        return LeituraTemperatura.formatar(temperaturaMinima) + " a " + LeituraTemperatura.formatar(temperaturaMaxima);
    }

    /** US06: limites inclusos. */
    public boolean temperaturaSegura(double temperatura) {
        return temperatura >= temperaturaMinima && temperatura <= temperaturaMaxima;
    }
}
