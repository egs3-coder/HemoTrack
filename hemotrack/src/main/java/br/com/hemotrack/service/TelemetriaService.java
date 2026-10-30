package br.com.hemotrack.service;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.LeituraTemperatura;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.StatusConservacao;
import br.com.hemotrack.model.StatusRequisicao;
import br.com.hemotrack.repository.LeituraTemperaturaRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US06 - Monitoramento de temperatura em transito.
 *   - Faixa segura do Concentrado de Hemacias: 2,0 C a 6,0 C (limites inclusos).
 *   - Uma leitura fora da faixa muda o status de conservacao para "Alerta - Fora da Faixa".
 *   - Duas leituras consecutivas fora da faixa mudam o status para "Comprometido".
 *   - Toda leitura e guardada no historico do pedido.
 */
@Service
public class TelemetriaService {

    private final LeituraTemperaturaRepository leituraRepository;
    private final RequisicaoRepository requisicaoRepository;

    public TelemetriaService(LeituraTemperaturaRepository leituraRepository,
                             RequisicaoRepository requisicaoRepository) {
        this.leituraRepository = leituraRepository;
        this.requisicaoRepository = requisicaoRepository;
    }

    public List<LeituraTemperatura> historico(Requisicao requisicao) {
        return leituraRepository.findByRequisicaoOrderByIdDesc(requisicao);
    }

    @Transactional
    public LeituraTemperatura registrar(Requisicao requisicao, Double temperatura) {
        if (temperatura == null) {
            throw new RegraNegocioException("A temperatura é obrigatória");
        }
        if (requisicao.getStatus() != StatusRequisicao.EM_TRANSPORTE) {
            throw new RegraNegocioException("Só é possível registrar leituras de pedidos em transporte");
        }
        boolean fora = !requisicao.getHemocomponente().temperaturaSegura(temperatura);
        Optional<LeituraTemperatura> anterior = leituraRepository.findFirstByRequisicaoOrderByIdDesc(requisicao);

        // Uma vez comprometido, o lote nao volta a ser considerado seguro.
        if (requisicao.getStatusConservacao() != StatusConservacao.COMPROMETIDO) {
            if (!fora) {
                requisicao.atualizarConservacao(StatusConservacao.NORMAL);
            } else if (anterior.isPresent() && anterior.get().isForaDaFaixa()) {
                requisicao.atualizarConservacao(StatusConservacao.COMPROMETIDO);
            } else {
                requisicao.atualizarConservacao(StatusConservacao.ALERTA);
            }
            requisicaoRepository.save(requisicao);
        }
        return leituraRepository.save(new LeituraTemperatura(requisicao, temperatura, fora));
    }

    public String mensagemAlerta(Requisicao requisicao, double temperatura) {
        Hemocomponente componente = requisicao.getHemocomponente();
        return "Temperatura de " + LeituraTemperatura.formatar(temperatura) + " fora da faixa segura ("
                + componente.getFaixaFormatada() + ")";
    }
}
