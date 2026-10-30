package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.LeituraTemperatura;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.repository.LeituraTemperaturaRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.service.TelemetriaService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US06 - Monitoramento de temperatura em trânsito")
class US06TemperaturaTest {

    private TelemetriaService service;
    private Requisicao req0001;
    /** Historico simulado do banco (mais recente por ultimo). */
    private final List<LeituraTemperatura> historico = new ArrayList<>();

    @BeforeEach
    void preparar() {
        LeituraTemperaturaRepository repositorio = mock(LeituraTemperaturaRepository.class);
        service = new TelemetriaService(repositorio, mock(RequisicaoRepository.class));
        when(repositorio.save(any(LeituraTemperatura.class))).thenAnswer(chamada -> {
            LeituraTemperatura leitura = chamada.getArgument(0);
            historico.add(leitura);
            return leitura;
        });
        when(repositorio.findFirstByRequisicaoOrderByIdDesc(any())).thenAnswer(chamada ->
                historico.isEmpty() ? Optional.empty() : Optional.of(historico.get(historico.size() - 1)));
        when(repositorio.findByRequisicaoOrderByIdDesc(any())).thenAnswer(chamada -> List.copyOf(historico));

        req0001 = Fixtures.emTransporte(TipoSanguineo.O_POS);
    }

    @Test
    @DisplayName("Cenário 1: Temperatura dentro da faixa segura")
    void dentroDaFaixa() {
        service.registrar(req0001, 4.2);
        service.registrar(req0001, 4.2);

        assertEquals("Normal / Seguro", req0001.getStatusConservacao().getRotulo());
        assertEquals(2, service.historico(req0001).size());
    }

    @Test
    @DisplayName("Cenário 2: Temperatura fora da faixa (alerta)")
    void foraDaFaixaAlerta() {
        assertEquals("Normal / Seguro", req0001.getStatusConservacao().getRotulo());

        LeituraTemperatura leitura = service.registrar(req0001, 8.5);

        assertEquals("Alerta - Fora da Faixa", req0001.getStatusConservacao().getRotulo());
        assertEquals("Temperatura de 8,5 °C fora da faixa segura (2,0 °C a 6,0 °C)",
                service.mensagemAlerta(req0001, 8.5));
        assertTrue(leitura.isForaDaFaixa());
        assertTrue(service.historico(req0001).contains(leitura));
    }

    @Test
    @DisplayName("Cenário 3: Duas leituras consecutivas fora da faixa")
    void duasConsecutivasForaDaFaixa() {
        service.registrar(req0001, 8.5);
        assertEquals("Alerta - Fora da Faixa", req0001.getStatusConservacao().getRotulo());

        service.registrar(req0001, 9.0);

        assertEquals("Comprometido", req0001.getStatusConservacao().getRotulo());
    }

    @Test
    @DisplayName("Cenário 4: Leitura no limite da faixa")
    void leituraNoLimite() {
        LeituraTemperatura leitura = service.registrar(req0001, 6.0);

        assertFalse(leitura.isForaDaFaixa());
        assertEquals("Normal / Seguro", req0001.getStatusConservacao().getRotulo());
        assertTrue(Hemocomponente.CONCENTRADO_HEMACIAS.temperaturaSegura(2.0));
    }

    @Test
    @DisplayName("Regra: só pedidos em transporte recebem leituras")
    void somenteEmTransporte() {
        assertThrows(RegraNegocioException.class,
                () -> service.registrar(Fixtures.requisicao(TipoSanguineo.O_POS, 1), 4.2));
    }
}
