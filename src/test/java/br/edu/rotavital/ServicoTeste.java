package br.edu.rotavital;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.rotavital.TipoSanguineo.*;

class ServicoTeste {
    private RotaVitalServico preparar() {
        RotaVitalServico s=new RotaVitalServico();s.cadastrarHospital(new Hospital(10,"Central"));return s;
    }
    @Test void selecaoManualSemCompatibilidadeEComFifo() {
        var s=preparar();s.cadastrarBolsa(new Bolsa(101,O_NEG,450));s.cadastrarBolsa(new Bolsa(102,AB_POS,450));
        s.solicitar(new Requisicao(1,10,A_NEG,2));s.solicitar(new Requisicao(2,10,O_POS,1));
        assertThrows(FalhaDominio.class,()->s.concluirPrimeira(2,new int[]{101}));
        var o=s.concluirPrimeira(1,new int[]{101,102});assertEquals(2,o.quantidade());
        assertEquals(0,s.estado().bolsas().length);assertEquals(2,s.consultarPrimeira().id());assertEquals(1,s.consultarTopo().requisicaoId());
    }
    @Test void recusasNaoAlteramEstado() {
        var s=preparar();s.cadastrarBolsa(new Bolsa(101,O_POS,450));s.solicitar(new Requisicao(1,10,O_POS,2));
        assertThrows(FalhaDominio.class,()->s.concluirPrimeira(1,new int[]{101}));
        assertThrows(FalhaDominio.class,()->s.concluirPrimeira(1,new int[]{101,101}));
        assertThrows(FalhaDominio.class,()->s.concluirPrimeira(1,new int[]{101,999}));
        assertEquals(1,s.estado().bolsas().length);assertEquals(1,s.estado().requisicoes().length);assertEquals(0,s.estado().historico().length);
    }
    @Test void loteAtomicoEIdECodigoUnicos() {
        var s=preparar();s.cadastrarBolsa(new Bolsa(102,O_POS,450));
        assertThrows(FalhaDominio.class,()->s.cadastrarLote(new Bolsa(100,O_POS,450),3));
        assertEquals(1,s.estado().bolsas().length);assertThrows(FalhaDominio.class,()->s.consultarBolsa(100));
        s.cadastrarLote(new Bolsa(200,O_POS,450),3);assertEquals("B-200-003",s.consultarBolsa(202).codigo());
        Bolsa repetida=new Bolsa(300,A_POS,450,"B-200-003",Hemocomponente.PLASMA,"2026-01-01","2026-02-01");
        assertThrows(FalhaDominio.class,()->s.cadastrarBolsa(repetida));
    }
    @Test void hospitalReferenciadoNaoPodeSerRemovido() {
        var s=preparar();s.cadastrarBolsa(new Bolsa(101,O_POS,450));s.solicitar(new Requisicao(1,10,O_POS,1));
        assertThrows(FalhaDominio.class,()->s.removerHospital(10));s.concluirPrimeira(1,new int[]{101});
        assertThrows(FalhaDominio.class,()->s.removerHospital(10));s.removerTopo(1);s.removerHospital(10);
        assertEquals(0,s.estado().hospitais().length);
    }
    @Test void urgenciaNaoUltrapassaFilaECancelamentoConfereId() {
        var s=preparar();s.solicitar(new Requisicao(1,10,O_POS,1));
        s.solicitar(new Requisicao(2,10,O_POS,1,Hemocomponente.PLASMA,Urgencia.EMERGENCIA));
        assertEquals(1,s.consultarPrimeira().id());assertThrows(FalhaDominio.class,()->s.cancelarPrimeira(2));
        s.cancelarPrimeira(1);assertEquals(2,s.consultarPrimeira().id());s.cancelarPrimeira(2);
        assertThrows(FalhaDominio.class,s::consultarPrimeira);
    }
    @Test void duasTentativasConcorrentesNaoConsomemDuasVezes() throws Exception {
        var s=preparar();s.cadastrarBolsa(new Bolsa(1,O_POS,450));s.solicitar(new Requisicao(1,10,O_POS,1));
        java.util.concurrent.atomic.AtomicInteger sucessos=new java.util.concurrent.atomic.AtomicInteger();
        Runnable acao=()->{try{s.concluirPrimeira(1,new int[]{1});sucessos.incrementAndGet();}catch(FalhaDominio esperado){}};
        Thread a=new Thread(acao),b=new Thread(acao);a.start();b.start();a.join();b.join();
        assertEquals(1,sucessos.get());assertEquals(1,s.estado().historico().length);assertEquals(0,s.estado().bolsas().length);
    }
}
