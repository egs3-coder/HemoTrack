package br.com.hemotrack.tarefa;

import br.com.hemotrack.service.BolsaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * US10: verificacao periodica de validade.
 * O Spring executa este metodo em uma thread de agendamento, separada das threads
 * que atendem as requisicoes web.
 */
@Component
public class RotinaValidade {

    private static final Logger LOG = LoggerFactory.getLogger(RotinaValidade.class);

    private final BolsaService bolsaService;

    public RotinaValidade(BolsaService bolsaService) {
        this.bolsaService = bolsaService;
    }

    @Scheduled(cron = "${hemotrack.validade.cron}")
    public void executar() {
        int descartadas = bolsaService.verificarValidade();
        LOG.info("[{}] Verificação de validade: {}", Thread.currentThread().getName(),
                bolsaService.mensagemDescarte(descartadas));
    }
}
