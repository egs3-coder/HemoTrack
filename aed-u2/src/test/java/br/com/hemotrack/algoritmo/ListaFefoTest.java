package br.com.hemotrack.algoritmo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.TipoSanguineo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("U2 - FEFO por inserção ordenada (ListaFefo)")
class ListaFefoTest {

    private final LocalDate hoje = LocalDate.of(2026, 11, 12);

    private Bolsa bolsa(String codigo, int venceEmDias) {
        return new Bolsa(codigo, Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_POS,
                hoje.minusDays(10), hoje.plusDays(venceEmDias));
    }

    private static List<String> codigos(ListaFefo lista) {
        List<String> saida = new ArrayList<>();
        for (Bolsa b : lista.emOrdem()) {
            saida.add(b.getCodigo());
        }
        return saida;
    }

    @Test
    @DisplayName("Insere fora de ordem e a lista fica ordenada por validade (início, meio e fim)")
    void insercaoOrdenada() {
        ListaFefo lista = new ListaFefo();
        lista.inserir(bolsa("B-10", 10));   // lista vazia
        lista.inserir(bolsa("B-02", 2));    // vira cabeca
        lista.inserir(bolsa("B-20", 20));   // vai para o fim
        lista.inserir(bolsa("B-05", 5));    // entra no meio

        assertEquals(List.of("B-02", "B-05", "B-10", "B-20"), codigos(lista));
        assertEquals(4, lista.tamanho());
    }

    @Test
    @DisplayName("Mesma validade: desempate pelo código")
    void desempatePorCodigo() {
        ListaFefo lista = new ListaFefo();
        lista.inserir(bolsa("BOL-C", 3));
        lista.inserir(bolsa("BOL-A", 3));
        lista.inserir(bolsa("BOL-B", 3));

        assertEquals(List.of("BOL-A", "BOL-B", "BOL-C"), codigos(lista));
    }

    @Test
    @DisplayName("removerPrimeira devolve sempre a que vence antes")
    void retiradaFefo() {
        ListaFefo lista = new ListaFefo();
        lista.inserir(bolsa("B-7", 7));
        lista.inserir(bolsa("B-1", 1));
        lista.inserir(bolsa("B-4", 4));

        assertEquals("B-1", lista.removerPrimeira().getCodigo());
        assertEquals("B-4", lista.removerPrimeira().getCodigo());
        assertEquals("B-7", lista.removerPrimeira().getCodigo());
        assertTrue(lista.estaVazia());
        assertNull(lista.removerPrimeira());
    }

    @Test
    @DisplayName("Vencidas formam um prefixo e são removidas do início")
    void removeVencidas() {
        ListaFefo lista = new ListaFefo();
        lista.inserir(bolsa("OK-3", 3));
        lista.inserir(bolsa("VENC-2", -2));
        lista.inserir(bolsa("HOJE", 0));      // vence hoje: ainda pode ser usada
        lista.inserir(bolsa("VENC-1", -1));

        assertEquals(2, lista.removerVencidas(hoje));
        assertEquals(List.of("HOJE", "OK-3"), codigos(lista));
    }
}
