package br.com.hemotrack.service;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.StatusBolsa;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.repository.BolsaRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * US08 - Projecao de demanda por tipo sanguineo.
 * O consumo de um mes e o numero de bolsas entregues naquele mes; sao analisados os 3 meses
 * completos anteriores ao mes atual. O estoque disponivel considera apenas bolsas "Disponivel"
 * ainda dentro da validade.
 */
@Service
public class DemandaService {

    private final BolsaRepository bolsaRepository;

    public DemandaService(BolsaRepository bolsaRepository) {
        this.bolsaRepository = bolsaRepository;
    }

    public List<YearMonth> mesesAnalisados() {
        List<YearMonth> meses = new ArrayList<>();
        YearMonth atual = YearMonth.now();
        for (int i = ProjecaoDemanda.MESES_NECESSARIOS; i >= 1; i--) {
            meses.add(atual.minusMonths(i));
        }
        return meses;
    }

    public List<ProjecaoDemanda> projetar() {
        List<YearMonth> meses = mesesAnalisados();
        List<Bolsa> entregues = bolsaRepository.findByDataEntregaBetween(
                meses.get(0).atDay(1), meses.get(meses.size() - 1).atEndOfMonth());
        LocalDate hoje = LocalDate.now();

        List<ProjecaoDemanda> projecoes = new ArrayList<>();
        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            List<Integer> consumo = new ArrayList<>();
            for (YearMonth mes : meses) {
                int total = 0;
                for (Bolsa bolsa : entregues) {
                    if (bolsa.getTipoSanguineo() == tipo && YearMonth.from(bolsa.getDataEntrega()).equals(mes)) {
                        total++;
                    }
                }
                consumo.add(total);
            }
            long estoque = bolsaRepository.countByStatusAndTipoSanguineoAndDataValidadeGreaterThanEqual(
                    StatusBolsa.DISPONIVEL, tipo, hoje);
            projecoes.add(new ProjecaoDemanda(tipo, consumo, estoque));
        }
        return projecoes;
    }

    /**
     * Tipos em risco de ruptura, do mais urgente para o menos urgente:
     * mais bolsas a captar primeiro e, no empate, menor cobertura em dias.
     */
    public List<ProjecaoDemanda> prioridadesDeCampanha(List<ProjecaoDemanda> projecoes) {
        List<ProjecaoDemanda> emRisco = new ArrayList<>();
        for (ProjecaoDemanda projecao : projecoes) {
            if (projecao.isRiscoRuptura()) {
                emRisco.add(projecao);
            }
        }
        emRisco.sort(Comparator.comparingLong(ProjecaoDemanda::getBolsasACaptar).reversed()
                .thenComparing(p -> p.getCoberturaDias() == null ? Long.MAX_VALUE : p.getCoberturaDias()));
        return emRisco;
    }

    public ResumoDemanda resumir(List<ProjecaoDemanda> projecoes) {
        int risco = 0;
        int adequado = 0;
        int insuficiente = 0;
        double demandaProjetada = 0;
        long aCaptar = 0;
        for (ProjecaoDemanda projecao : projecoes) {
            if (!projecao.isHistoricoSuficiente()) {
                insuficiente++;
                continue;
            }
            if (projecao.isRiscoRuptura()) {
                risco++;
            } else {
                adequado++;
            }
            demandaProjetada += projecao.getMedia();
            aCaptar += projecao.getBolsasACaptar();
        }
        return new ResumoDemanda(risco, adequado, insuficiente, Math.round(demandaProjetada), aCaptar);
    }

    public double maiorMedia(List<ProjecaoDemanda> projecoes) {
        double maior = 0;
        for (ProjecaoDemanda projecao : projecoes) {
            maior = Math.max(maior, projecao.getMedia() + projecao.getDesvio());
        }
        return maior;
    }

    /** Totais exibidos no topo da tela de demanda preditiva. */
    public static final class ResumoDemanda {

        private final int tiposEmRisco;
        private final int tiposAdequados;
        private final int tiposSemHistorico;
        private final long demandaProjetada;
        private final long bolsasACaptar;

        public ResumoDemanda(int tiposEmRisco, int tiposAdequados, int tiposSemHistorico,
                             long demandaProjetada, long bolsasACaptar) {
            this.tiposEmRisco = tiposEmRisco;
            this.tiposAdequados = tiposAdequados;
            this.tiposSemHistorico = tiposSemHistorico;
            this.demandaProjetada = demandaProjetada;
            this.bolsasACaptar = bolsasACaptar;
        }

        public int getTiposEmRisco() {
            return tiposEmRisco;
        }

        public int getTiposAdequados() {
            return tiposAdequados;
        }

        public int getTiposSemHistorico() {
            return tiposSemHistorico;
        }

        public long getDemandaProjetada() {
            return demandaProjetada;
        }

        public long getBolsasACaptar() {
            return bolsasACaptar;
        }
    }
}
