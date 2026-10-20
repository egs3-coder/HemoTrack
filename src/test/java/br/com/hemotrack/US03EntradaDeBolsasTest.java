package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.StatusBolsa;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.service.BolsaService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US03 - Cadastro e entrada de bolsas no estoque")
class US03EntradaDeBolsasTest {

    private BolsaRepository bolsaRepository;
    private BolsaService service;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void preparar() {
        bolsaRepository = mock(BolsaRepository.class);
        service = new BolsaService(bolsaRepository, mock(RequisicaoRepository.class));
        when(bolsaRepository.save(any(Bolsa.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    @DisplayName("Cenário 1: Cadastro de bolsa válida (saldo A+ passa de 3 para 4)")
    void cadastroDeBolsaValida() {
        when(bolsaRepository.countByStatusAndTipoSanguineoAndDataValidadeGreaterThanEqual(
                eq(StatusBolsa.DISPONIVEL), eq(TipoSanguineo.A_POS), any())).thenReturn(3L);
        assertEquals(3L, service.saldoPorTipo().get(TipoSanguineo.A_POS));

        Bolsa bolsa = service.cadastrar("BOL-9821", Hemocomponente.PLAQUETAS, TipoSanguineo.A_POS,
                hoje, hoje.plusDays(5));
        when(bolsaRepository.countByStatusAndTipoSanguineoAndDataValidadeGreaterThanEqual(
                eq(StatusBolsa.DISPONIVEL), eq(TipoSanguineo.A_POS), any())).thenReturn(4L);

        assertEquals("BOL-9821", bolsa.getCodigo());
        assertEquals("Disponível", bolsa.getStatus().getRotulo());
        assertEquals("Plaquetas", bolsa.getHemocomponente().getRotulo());
        assertEquals(4L, service.saldoPorTipo().get(TipoSanguineo.A_POS));
    }

    @Test
    @DisplayName("Cenário 2: Bolsa com validade vencida")
    void bolsaComValidadeVencida() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("BOL-9822", Hemocomponente.PLAQUETAS, TipoSanguineo.A_POS,
                        hoje.minusDays(10), hoje.minusDays(1)));

        assertEquals("Data de validade inválida. Não é permitido cadastrar bolsas vencidas", erro.getMessage());
        verify(bolsaRepository, never()).save(any(Bolsa.class));
    }

    @Test
    @DisplayName("Regra: validade igual a hoje também é bloqueada (deve ser posterior à data atual)")
    void validadeHojeBloqueada() {
        assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("BOL-9823", Hemocomponente.PLAQUETAS, TipoSanguineo.A_POS, hoje, hoje));
    }

    @Test
    @DisplayName("Cenário 3: Código duplicado")
    void codigoDuplicado() {
        when(bolsaRepository.existsByCodigo("BOL-9821")).thenReturn(true);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("BOL-9821", Hemocomponente.PLAQUETAS, TipoSanguineo.A_POS,
                        hoje, hoje.plusDays(5)));

        assertEquals("Já existe uma bolsa cadastrada com o código BOL-9821", erro.getMessage());
        verify(bolsaRepository, never()).save(any(Bolsa.class));
    }
}
