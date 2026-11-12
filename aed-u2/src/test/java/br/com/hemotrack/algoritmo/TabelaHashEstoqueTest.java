package br.com.hemotrack.algoritmo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.TipoSanguineo;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("U2 - Índice de estoque por hash (TabelaHashEstoque)")
class TabelaHashEstoqueTest {

    private final LocalDate hoje = LocalDate.of(2026, 11, 12);

    private Bolsa bolsa(String codigo, Hemocomponente componente, TipoSanguineo tipo, int venceEmDias) {
        return new Bolsa(codigo, componente, tipo, hoje.minusDays(5), hoje.plusDays(venceEmDias));
    }

    @Test
    @DisplayName("Chave e função hash: chave = componente*8 + tipo, h = chave % 11")
    void chaveEHash() {
        assertEquals(0, TabelaHashEstoque.chave(Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_NEG));
        assertEquals(11, TabelaHashEstoque.chave(Hemocomponente.PLAQUETAS, TipoSanguineo.A_POS));
        assertEquals(31, TabelaHashEstoque.chave(Hemocomponente.CRIOPRECIPITADO, TipoSanguineo.AB_POS));
        assertEquals(0, TabelaHashEstoque.hash(0));
        assertEquals(0, TabelaHashEstoque.hash(11));
        assertEquals(9, TabelaHashEstoque.hash(31));
    }

    @Test
    @DisplayName("Busca devolve a lista FEFO da chave; chave inexistente devolve null")
    void buscaPorChave() {
        TabelaHashEstoque tabela = TabelaHashEstoque.construir(List.of(
                bolsa("CH-1", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.B_POS, 8),
                bolsa("CH-2", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.B_POS, 2),
                bolsa("PQ-1", Hemocomponente.PLAQUETAS, TipoSanguineo.B_POS, 1)));

        ListaFefo chBPos = tabela.buscar(Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.B_POS);
        assertNotNull(chBPos);
        assertEquals(2, chBPos.tamanho());
        assertEquals("CH-2", chBPos.primeira().getCodigo());
        assertEquals(1, tabela.buscar(Hemocomponente.PLAQUETAS, TipoSanguineo.B_POS).tamanho());
        assertNull(tabela.buscar(Hemocomponente.PLASMA, TipoSanguineo.B_POS));
        assertEquals(3, tabela.getTotalBolsas());
        assertEquals(2, tabela.getChavesDistintas());
    }

    @Test
    @DisplayName("Colisão: chaves 0 e 11 caem no slot 0 e as duas continuam sendo encontradas")
    void colisaoPorEncadeamento() {
        TabelaHashEstoque tabela = TabelaHashEstoque.construir(List.of(
                bolsa("CH-ONEG", Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_NEG, 4),
                bolsa("PQ-APOS", Hemocomponente.PLAQUETAS, TipoSanguineo.A_POS, 6)));

        assertEquals(1, tabela.getColisoes());
        assertEquals("CH-ONEG",
                tabela.buscar(Hemocomponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_NEG).primeira().getCodigo());
        assertEquals("PQ-APOS",
                tabela.buscar(Hemocomponente.PLAQUETAS, TipoSanguineo.A_POS).primeira().getCodigo());
    }

    @Test
    @DisplayName("Todas as 32 combinações: cada uma acha a sua lista e o fator de carga fica limitado")
    void todasAsCombinacoes() {
        TabelaHashEstoque tabela = new TabelaHashEstoque();
        for (Hemocomponente c : Hemocomponente.values()) {
            for (TipoSanguineo t : TipoSanguineo.values()) {
                tabela.inserir(bolsa(c.name() + "-" + t.name(), c, t, 5));
            }
        }
        for (Hemocomponente c : Hemocomponente.values()) {
            for (TipoSanguineo t : TipoSanguineo.values()) {
                assertEquals(c.name() + "-" + t.name(), tabela.buscar(c, t).primeira().getCodigo());
            }
        }
        assertEquals(32, tabela.getChavesDistintas());
        assertTrue(tabela.fatorDeCarga() < 3.0);
    }
}
