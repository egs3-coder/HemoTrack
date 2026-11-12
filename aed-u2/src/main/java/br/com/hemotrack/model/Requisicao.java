package br.com.hemotrack.model;

import br.com.hemotrack.excecao.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
public class Requisicao extends EntidadeBase {

    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(optional = false)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Hemocomponente hemocomponente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineo;

    private int quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgencia urgencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRequisicao status;

    @Enumerated(EnumType.STRING)
    private StatusConservacao statusConservacao;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataDespacho;
    private LocalDateTime dataEntrega;

    private String itinerario;
    private Integer tempoPrevistoMin;
    private Double distanciaKm;

    protected Requisicao() {
        // exigido pelo JPA
    }

    public Requisicao(Hospital hospital, Hemocomponente hemocomponente, TipoSanguineo tipoSanguineo,
                      int quantidade, Urgencia urgencia) {
        this.hospital = hospital;
        this.hemocomponente = hemocomponente;
        this.tipoSanguineo = tipoSanguineo;
        this.quantidade = quantidade;
        this.urgencia = urgencia;
        this.status = StatusRequisicao.PENDENTE;
        this.dataCriacao = LocalDateTime.now();
    }

    public static final String PREFIXO_CODIGO = "REQ-";

    /** US01: identificador de rastreamento no formato REQ-0001. */
    public static String formatarCodigo(long numero) {
        return String.format("%s%04d", PREFIXO_CODIGO, numero);
    }

    /** Numero sequencial contido no identificador ("REQ-0012" -> 12); 0 se o formato nao for reconhecido. */
    public static long numeroDoCodigo(String codigo) {
        if (codigo == null || !codigo.startsWith(PREFIXO_CODIGO)) {
            return 0;
        }
        try {
            return Long.parseLong(codigo.substring(PREFIXO_CODIGO.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** US01: atribui o identificador sequencial, em ordem de cadastro (REQ-0001, REQ-0002, ...). */
    public void atribuirCodigo(long numero) {
        this.codigo = formatarCodigo(numero);
    }

    public void marcarAlocada() {
        if (status != StatusRequisicao.PENDENTE) {
            throw new RegraNegocioException("Só é possível alocar requisições pendentes de alocação");
        }
        this.status = StatusRequisicao.ALOCADA;
    }

    /** Usado quando uma bolsa reservada vence: o pedido precisa ser alocado de novo. */
    public void voltarParaPendente() {
        this.status = StatusRequisicao.PENDENTE;
        this.itinerario = null;
        this.tempoPrevistoMin = null;
        this.distanciaKm = null;
    }

    public void registrarRota(Rota rota) {
        if (status != StatusRequisicao.ALOCADA) {
            throw new RegraNegocioException("Só é possível gerar rota para pedidos com status Alocada");
        }
        this.itinerario = rota.getItinerario();
        this.tempoPrevistoMin = rota.getTempoMin();
        this.distanciaKm = rota.getDistanciaKm();
    }

    public void despachar(LocalDateTime agora) {
        if (status != StatusRequisicao.ALOCADA) {
            throw new RegraNegocioException("Só é possível despachar pedidos com status Alocada");
        }
        if (itinerario == null) {
            throw new RegraNegocioException("Gere a rota de entrega antes de despachar o pedido");
        }
        this.status = StatusRequisicao.EM_TRANSPORTE;
        this.statusConservacao = StatusConservacao.NORMAL;
        this.dataDespacho = agora;
    }

    public void confirmarRecebimento(LocalDateTime agora) {
        if (status != StatusRequisicao.EM_TRANSPORTE) {
            throw new RegraNegocioException("Só é possível confirmar o recebimento de pedidos em transporte");
        }
        this.status = StatusRequisicao.ENTREGUE;
        this.dataEntrega = agora;
    }

    public void atualizarConservacao(StatusConservacao novoStatus) {
        this.statusConservacao = novoStatus;
    }

    /** Minutos entre o despacho e a confirmacao de recebimento (null se ainda nao entregue). */
    public Long getDuracaoEntregaMin() {
        if (dataDespacho == null || dataEntrega == null) {
            return null;
        }
        return Duration.between(dataDespacho, dataEntrega).toMinutes();
    }

    public boolean isEntregueNoPrazo() {
        Long duracao = getDuracaoEntregaMin();
        return duracao != null && tempoPrevistoMin != null && duracao <= tempoPrevistoMin;
    }

    public String getCodigo() {
        return codigo;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public Hemocomponente getHemocomponente() {
        return hemocomponente;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public Urgencia getUrgencia() {
        return urgencia;
    }

    public StatusRequisicao getStatus() {
        return status;
    }

    public StatusConservacao getStatusConservacao() {
        return statusConservacao;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataDespacho() {
        return dataDespacho;
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }

    public String getItinerario() {
        return itinerario;
    }

    public Integer getTempoPrevistoMin() {
        return tempoPrevistoMin;
    }

    public Double getDistanciaKm() {
        return distanciaKm;
    }

    /** US05: distancia em km sem casas decimais desnecessarias ("9", "9,5"). */
    public String getDistanciaFormatada() {
        if (distanciaKm == null) {
            return null;
        }
        if (distanciaKm == Math.rint(distanciaKm)) {
            return String.valueOf(distanciaKm.longValue());
        }
        return String.format(new java.util.Locale("pt", "BR"), "%.1f", distanciaKm);
    }

    /** US11: posicao do pedido na sequencia de status (0 = Pendente ... 3 = Entregue). */
    public int getEtapa() {
        return status.ordinal();
    }
}
