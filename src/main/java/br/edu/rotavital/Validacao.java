package br.edu.rotavital;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
final class Validacao {
    private Validacao() { }
    static void texto(String valor, int limite) {
        if (valor == null || valor.isBlank() || valor.indexOf('\0') >= 0
                || valor.getBytes(StandardCharsets.UTF_8).length > limite)
            throw new IllegalArgumentException("Texto vazio ou maior que " + limite + " bytes.");
    }
    static LocalDate data(String valor) {
        if (valor == null || !valor.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
            throw new IllegalArgumentException("Data deve usar AAAA-MM-DD.");
        LocalDate data = LocalDate.parse(valor);
        if (data.getYear() < 1) throw new IllegalArgumentException("Ano inválido.");
        return data;
    }
}
