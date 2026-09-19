package br.edu.rotavital;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static br.edu.rotavital.TipoSanguineo.*;

class EstruturasTeste {
    @Test void listaInsereConsultaRemoveInicioMeioFim() {
        ListaEstoque lista=new ListaEstoque();assertTrue(lista.remover(1).isEmpty());
        for(int i=1;i<=3;i++)assertTrue(lista.inserir(new Bolsa(i,O_POS,450)));
        assertFalse(lista.inserir(new Bolsa(2,O_POS,450)));
        assertEquals(2,lista.consultar(2).orElseThrow().id());
        assertArrayEquals(new int[]{3,2,1},java.util.Arrays.stream(lista.todos()).mapToInt(Bolsa::id).toArray());
        assertEquals(2,lista.remover(2).orElseThrow().id());
        assertEquals(1,lista.remover(1).orElseThrow().id());
        assertEquals(3,lista.remover(3).orElseThrow().id());assertEquals(0,lista.todos().length);
    }
    @Test void filaMantemFifoEReutilizaDepoisDeEsvaziar() {
        FilaRequisicoes fila=new FilaRequisicoes();assertTrue(fila.remover().isEmpty());
        fila.inserir(new Requisicao(1,10,O_POS,1));fila.inserir(new Requisicao(2,10,A_POS,1));
        assertFalse(fila.inserir(new Requisicao(1,10,O_POS,1)));
        assertEquals(1,fila.consultar().orElseThrow().id());assertEquals(1,fila.remover().orElseThrow().id());
        assertEquals(2,fila.remover().orElseThrow().id());assertTrue(fila.consultar().isEmpty());
        assertTrue(fila.inserir(new Requisicao(3,10,O_POS,1)));assertEquals(3,fila.remover().orElseThrow().id());
    }
    @Test void pilhaMantemLifo() {
        PilhaHistorico pilha=new PilhaHistorico();assertTrue(pilha.remover().isEmpty());
        pilha.inserir(new Operacao(1,10,2,O_POS));pilha.inserir(new Operacao(2,10,1,O_POS));
        assertEquals(2,pilha.consultar().orElseThrow().requisicaoId());
        assertEquals(2,pilha.remover().orElseThrow().requisicaoId());
        assertEquals(1,pilha.remover().orElseThrow().requisicaoId());assertTrue(pilha.consultar().isEmpty());
    }
    @Test void abbPercursosEAsTresRemocoes() {
        ArvoreHospitais arvore=new ArvoreHospitais();
        for(int id:new int[]{50,30,70,20,40,60,80})assertTrue(arvore.inserir(new Hospital(id,"Hospital "+id)));
        assertFalse(arvore.inserir(new Hospital(50,"Duplicado")));
        assertArrayEquals(new int[]{50,30,20,40,70,60,80},ids(arvore,ArvoreHospitais.Ordem.PRE_ORDEM));
        assertArrayEquals(new int[]{20,30,40,50,60,70,80},ids(arvore,ArvoreHospitais.Ordem.EM_ORDEM));
        assertArrayEquals(new int[]{20,40,30,60,80,70,50},ids(arvore,ArvoreHospitais.Ordem.POS_ORDEM));
        for(int id:new int[]{20,30,50})assertEquals(id,arvore.remover(id).orElseThrow().id());
        assertArrayEquals(new int[]{40,60,70,80},ids(arvore,ArvoreHospitais.Ordem.EM_ORDEM));
        assertEquals("Hospital 60",arvore.consultar(60).orElseThrow().nome());assertTrue(arvore.consultar(25).isEmpty());
        for(int id:new int[]{40,60,70,80})arvore.remover(id);assertEquals(0,arvore.contar());
        arvore.inserir(new Hospital(10,"Raiz"));arvore.inserir(new Hospital(5,"Filho esquerdo"));
        arvore.remover(10);assertEquals(5,arvore.estrutura().hospital().id());
    }
    private int[] ids(ArvoreHospitais arvore,ArvoreHospitais.Ordem ordem) {
        return java.util.Arrays.stream(arvore.todos(ordem)).mapToInt(Hospital::id).toArray();
    }
    @Test void entidadesRecusamCamposInvalidos() {
        assertThrows(IllegalArgumentException.class,()->new Bolsa(0,O_POS,450));
        assertThrows(IllegalArgumentException.class,()->new Hospital(1,"","Recife"));
        assertThrows(IllegalArgumentException.class,()->new Requisicao(1,10,O_POS,0));
        assertThrows(RuntimeException.class,()->new Bolsa(1,O_POS,450,"B",Hemocomponente.PLASMA,"2026-02-30","2026-03-10"));
        assertThrows(IllegalArgumentException.class,()->new Bolsa(1,O_POS,450,"B",Hemocomponente.PLASMA,"2026-02-10","2026-02-09"));
    }
}
