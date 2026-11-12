package br.com.hemotrack.config;

import br.com.hemotrack.model.Bolsa;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.model.Perfil;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.Rota;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.model.Trecho;
import br.com.hemotrack.model.Urgencia;
import br.com.hemotrack.model.Usuario;
import br.com.hemotrack.repository.BolsaRepository;
import br.com.hemotrack.repository.HospitalRepository;
import br.com.hemotrack.repository.RequisicaoRepository;
import br.com.hemotrack.repository.TrechoRepository;
import br.com.hemotrack.repository.UsuarioRepository;
import br.com.hemotrack.service.RotaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Executa na subida da aplicacao.
 * Sempre garante os usuarios de acesso. Se hemotrack.dados-demo=true e o banco estiver vazio,
 * carrega dados de demonstracao que reproduzem os numeros dos cenarios de historias.md:
 *
 * - US05: malha Hemocentro Central / Hospital Boa Vista / Hospital Santa Clara (rota de 9 km e 18 min).
 * - US06 e US11: REQ-0001 "Em Transporte" para o Hospital Santa Clara com a bolsa BOL-0001.
 * - US07: nos ultimos 30 dias (e fora dos ultimos 7) 36 bolsas entregues, 4 descartadas por validade,
 *         20 pedidos entregues (18 no prazo), Norte 20/24 min e Sul 30/40 min -> 10%, 90%, 22 e 35 min.
 * - US08: consumo O- 10/14/12 e A+ 20/22/24 com estoque O- 8 e A+ 30; AB- com consumo em apenas 1 mes.
 * - US10: BOL-1001 disponivel vencida ontem, BOL-1002 vence em 4 dias e BOL-1003 reservada e vencida.
 *
 * Para recarregar tudo, pare a aplicacao e apague a pasta hemotrack-dados (na pasta do usuario).
 */
@Component
public class DadosIniciais implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DadosIniciais.class);

    private static final Hemocomponente HEMACIAS = Hemocomponente.CONCENTRADO_HEMACIAS;
    /** Dias atras em que ficam as entregas do painel: dentro de 30 dias e fora de 7 dias (US07). */
    private static final int DIAS_ATRAS_PAINEL = 10;

    private final UsuarioRepository usuarioRepository;
    private final HospitalRepository hospitalRepository;
    private final TrechoRepository trechoRepository;
    private final BolsaRepository bolsaRepository;
    private final RequisicaoRepository requisicaoRepository;
    private final boolean carregarDemo;
    private final String urlBanco;
    /** Proximo numero do identificador REQ-xxxx dos pedidos de demonstracao (em ordem de cadastro). */
    private long proximoPedido = 1;

    public DadosIniciais(UsuarioRepository usuarioRepository, HospitalRepository hospitalRepository,
                         TrechoRepository trechoRepository, BolsaRepository bolsaRepository,
                         RequisicaoRepository requisicaoRepository,
                         @Value("${hemotrack.dados-demo:true}") boolean carregarDemo,
                         @Value("${spring.datasource.url:}") String urlBanco) {
        this.usuarioRepository = usuarioRepository;
        this.hospitalRepository = hospitalRepository;
        this.trechoRepository = trechoRepository;
        this.bolsaRepository = bolsaRepository;
        this.requisicaoRepository = requisicaoRepository;
        this.carregarDemo = carregarDemo;
        this.urlBanco = urlBanco;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            criarUsuarios();
        }
        if (carregarDemo && hospitalRepository.count() == 0) {
            carregarDemonstracao();
        }
        // Mostra no console onde as contas e os demais dados estao gravados (US02: a conta criada
        // pela tela continua valendo nas proximas execucoes, pois o banco e sempre o mesmo arquivo).
        LOG.info("Banco de dados HemoTrack: {} ({} conta(s) cadastrada(s))", urlBanco, usuarioRepository.count());
    }

    /** US02: um usuario de cada perfil. A conta "marina" do cenario 1 e criada pela propria tela. */
    private void criarUsuarios() {
        String senha = "hemotrack123";
        usuarioRepository.save(new Usuario("Helena Prado", "hospital", senha, Perfil.PROFISSIONAL_HOSPITALAR));
        usuarioRepository.save(new Usuario("Otávio Lima", "operador", senha, Perfil.OPERADOR_HEMOCENTRO));
        usuarioRepository.save(new Usuario("Carla Nunes", "logistica", senha, Perfil.COORDENADOR_LOGISTICA));
        usuarioRepository.save(new Usuario("Gustavo Reis", "gestor", senha, Perfil.GESTOR));
    }

    private void carregarDemonstracao() {
        LocalDate hoje = LocalDate.now();

        // US09: hospitais conveniados
        Hospital santaClara = hospitalRepository.save(new Hospital("Hospital Santa Clara", "12345678000190", "Norte"));
        Hospital boaVista = hospitalRepository.save(new Hospital("Hospital Boa Vista", "98765432000110", "Sul"));

        // US05: malha do cenario 1
        trechoRepository.save(new Trecho(RotaService.HEMOCENTRO, "Hospital Santa Clara", 25, 12));
        trechoRepository.save(new Trecho(RotaService.HEMOCENTRO, "Hospital Boa Vista", 10, 5));
        trechoRepository.save(new Trecho("Hospital Boa Vista", "Hospital Santa Clara", 8, 4));

        // US11 cenario 1 / US06: REQ-0001 em transporte com a bolsa BOL-0001 (criada primeiro para ser a 0001)
        criarPedidoEmTransporte(santaClara, hoje);

        // US08: consumo dos 3 meses anteriores, no dia 1 de cada mes (fora da janela do painel US07)
        int[] consumoONeg = {10, 14, 12};
        int[] consumoAPos = {20, 22, 24};
        YearMonth atual = YearMonth.now();
        for (int i = 0; i < 3; i++) {
            LocalDate dia = atual.minusMonths(3 - i).atDay(1);
            criarEntregues("HIS-ON-" + i, TipoSanguineo.O_NEG, consumoONeg[i], dia);
            criarEntregues("HIS-AP-" + i, TipoSanguineo.A_POS, consumoAPos[i], dia);
        }
        // US08 cenario 2: AB- com consumo registrado em apenas 1 mes
        criarEntregues("HIS-ABN", TipoSanguineo.AB_NEG, 2, atual.minusMonths(1).atDay(1));

        // US08: estoque disponivel O- 8 e A+ 30 (29 aqui + BOL-1002 da US10)
        for (int i = 1; i <= 8; i++) {
            bolsaRepository.save(new Bolsa(String.format("BOL-ON-%02d", i), HEMACIAS,
                    TipoSanguineo.O_NEG, hoje.minusDays(5), hoje.plusDays(10 + i)));
        }
        for (int i = 1; i <= 29; i++) {
            bolsaRepository.save(new Bolsa(String.format("BOL-AP-%02d", i), HEMACIAS,
                    TipoSanguineo.A_POS, hoje.minusDays(5), hoje.plusDays(10 + i)));
        }

        // US07: 20 pedidos entregues com 36 bolsas (16 pedidos de 2 bolsas e 4 de 1 bolsa)
        LocalDate diaPainel = hoje.minusDays(DIAS_ATRAS_PAINEL);
        int pedido = 0;
        for (int i = 0; i < 5; i++) {   // Norte: 5 x 20 min e 5 x 24 min, previsto 25 -> todos no prazo
            criarPedidoEntregue(santaClara, 25, 12, 20, quantidadeDoPedido(pedido++), diaPainel);
            criarPedidoEntregue(santaClara, 25, 12, 24, quantidadeDoPedido(pedido++), diaPainel);
        }
        for (int i = 0; i < 5; i++) {   // Sul: 5 x 30 min e 5 x 40 min; 2 dos de 40 min tinham previsto 35
            int previsto40min = i < 2 ? 35 : 40;
            criarPedidoEntregue(boaVista, 40, 5, 30, quantidadeDoPedido(pedido++), diaPainel);
            criarPedidoEntregue(boaVista, previsto40min, 5, 40, quantidadeDoPedido(pedido++), diaPainel);
        }

        // US07: 4 bolsas descartadas por validade no periodo
        for (int i = 1; i <= 4; i++) {
            Bolsa descartada = new Bolsa("DEM-DESC-0" + i, HEMACIAS, TipoSanguineo.B_POS,
                    diaPainel.minusDays(40), diaPainel.minusDays(1));
            descartada.descartar(Bolsa.MOTIVO_VALIDADE, diaPainel);
            bolsaRepository.save(descartada);
        }

        // US10 cenario 1: BOL-1001 disponivel vencida ontem e BOL-1002 que vence daqui a 4 dias
        bolsaRepository.save(new Bolsa("BOL-1001", HEMACIAS, TipoSanguineo.A_POS,
                hoje.minusDays(36), hoje.minusDays(1)));
        bolsaRepository.save(new Bolsa("BOL-1002", HEMACIAS, TipoSanguineo.A_POS,
                hoje.minusDays(31), hoje.plusDays(4)));

        // US10 cenario 2: BOL-1003 reservada para um pedido e vencida ontem
        Requisicao reservado = new Requisicao(boaVista, HEMACIAS, TipoSanguineo.B_POS, 1, Urgencia.URGENTE);
        reservado.atribuirCodigo(proximoPedido++);
        reservado.marcarAlocada();
        reservado = requisicaoRepository.save(reservado);
        Bolsa reservadaVencida = new Bolsa("BOL-1003", HEMACIAS, TipoSanguineo.B_POS,
                hoje.minusDays(36), hoje.minusDays(1));
        reservadaVencida.reservarPara(reservado);
        bolsaRepository.save(reservadaVencida);
    }

    /** 16 pedidos de 2 bolsas e 4 de 1 bolsa = 36 bolsas. */
    private int quantidadeDoPedido(int indice) {
        return indice % 5 == 4 ? 1 : 2;
    }

    private void criarPedidoEmTransporte(Hospital destino, LocalDate hoje) {
        Requisicao pedido = new Requisicao(destino, HEMACIAS, TipoSanguineo.O_POS, 1, Urgencia.EMERGENCIA);
        pedido.atribuirCodigo(proximoPedido++);
        pedido.marcarAlocada();
        pedido.registrarRota(new Rota(List.of(RotaService.HEMOCENTRO, "Hospital Boa Vista", destino.getNome()), 18, 9));
        pedido.despachar(LocalDateTime.now().minusMinutes(5));
        pedido = requisicaoRepository.save(pedido);

        Bolsa bolsa = new Bolsa("BOL-0001", HEMACIAS, TipoSanguineo.O_POS, hoje.minusDays(2), hoje.plusDays(20));
        bolsa.reservarPara(pedido);
        bolsa.despachar();
        bolsaRepository.save(bolsa);
    }

    private void criarEntregues(String prefixo, TipoSanguineo tipo, int quantidade, LocalDate dataEntrega) {
        for (int i = 1; i <= quantidade; i++) {
            Bolsa bolsa = new Bolsa(prefixo + "-" + i, HEMACIAS, tipo,
                    dataEntrega.minusDays(7), dataEntrega.plusDays(28));
            bolsa.entregar(dataEntrega);
            bolsaRepository.save(bolsa);
        }
    }

    private void criarPedidoEntregue(Hospital hospital, int tempoPrevistoMin, double distanciaKm,
                                     int duracaoMin, int quantidade, LocalDate dia) {
        LocalDateTime despacho = dia.atTime(10, 0);

        Requisicao pedido = new Requisicao(hospital, HEMACIAS, TipoSanguineo.AB_POS, quantidade, Urgencia.URGENTE);
        pedido.atribuirCodigo(proximoPedido++);
        pedido.marcarAlocada();
        pedido.registrarRota(new Rota(List.of(RotaService.HEMOCENTRO, hospital.getNome()),
                tempoPrevistoMin, distanciaKm));
        pedido.despachar(despacho);
        pedido.confirmarRecebimento(despacho.plusMinutes(duracaoMin));
        pedido = requisicaoRepository.save(pedido);

        for (int i = 1; i <= quantidade; i++) {
            Bolsa bolsa = new Bolsa(pedido.getCodigo().replace("REQ", "ENT") + "-" + i, HEMACIAS,
                    TipoSanguineo.AB_POS, dia.minusDays(7), dia.plusDays(28));
            bolsa.reservarPara(pedido);
            bolsa.entregar(dia);
            bolsaRepository.save(bolsa);
        }
    }
}
