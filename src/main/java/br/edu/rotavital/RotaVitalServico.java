package br.edu.rotavital;
import static br.edu.rotavital.FalhaDominio.Motivo.*;
import java.util.Objects;

/** Coordena as estruturas. Uma instância em memória, protegida pelo mesmo monitor.
 * Não há seleção automática, FEFO, compatibilidade, hash ou roteirização. */
public final class RotaVitalServico {
    private final ListaEstoque estoque = new ListaEstoque();
    private final FilaRequisicoes requisicoes = new FilaRequisicoes();
    private final PilhaHistorico historico = new PilhaHistorico();
    private final ArvoreHospitais hospitais = new ArvoreHospitais();
    public record Estado(Bolsa[] bolsas, Requisicao[] requisicoes, Operacao[] historico,
                         Hospital[] hospitais, ArvoreHospitais.Ramo arvore) { }
    public synchronized Estado estado() {
        return new Estado(estoque.todos(), requisicoes.todos(), historico.todos(),
                hospitais.todos(ArvoreHospitais.Ordem.EM_ORDEM), hospitais.estrutura());
    }
    public synchronized Bolsa cadastrarBolsa(Bolsa bolsa) {
        Objects.requireNonNull(bolsa);
        if (!estoque.inserir(bolsa)) throw new FalhaDominio(CONFLITO, "Já existe uma bolsa com esse ID no estoque.");
        return bolsa;
    }
    public synchronized Bolsa[] cadastrarLote(Bolsa modelo, int quantidade) {
        Objects.requireNonNull(modelo);
        if (quantidade < 1 || quantidade > 1000 || (long)modelo.id()+quantidade-1 > Integer.MAX_VALUE)
            throw new FalhaDominio(INVALIDO, "Lote deve ter de 1 a 1000 bolsas e IDs válidos.");
        Bolsa[] bolsas = new Bolsa[quantidade];
        for (int i = 0; i < quantidade; i++) {
            String codigo = quantidade == 1 ? modelo.codigo() : modelo.codigo()+String.format(java.util.Locale.ROOT,"-%03d",i+1);
            bolsas[i] = new Bolsa(modelo.id()+i, modelo.tipo(), modelo.volumeMl(), codigo,
                    modelo.hemocomponente(), modelo.dataColeta(), modelo.dataValidade());
        }
        if (!estoque.inserirLote(bolsas)) throw new FalhaDominio(CONFLITO, "ID ou código de bolsa duplicado. Nenhuma bolsa foi inserida.");
        return bolsas;
    }
    public synchronized Bolsa consultarBolsa(int id) {
        return estoque.consultar(id).orElseThrow(() -> new FalhaDominio(NAO_ENCONTRADO, "Bolsa não encontrada."));
    }
    public synchronized Bolsa removerBolsa(int id) {
        return estoque.remover(id).orElseThrow(() -> new FalhaDominio(NAO_ENCONTRADO, "Bolsa não encontrada."));
    }
    public synchronized Hospital cadastrarHospital(Hospital hospital) {
        Objects.requireNonNull(hospital);
        if (!hospitais.inserir(hospital)) throw new FalhaDominio(CONFLITO, "Já existe um hospital com esse ID.");
        return hospital;
    }
    public synchronized Hospital consultarHospital(int id) {
        return hospitais.consultar(id).orElseThrow(() -> new FalhaDominio(NAO_ENCONTRADO, "Hospital não encontrado."));
    }
    public synchronized Hospital removerHospital(int id) {
        consultarHospital(id);
        for (Requisicao r : requisicoes.todos())
            if (r.hospitalId() == id) throw new FalhaDominio(CONFLITO, "Hospital possui requisições pendentes.");
        for (Operacao o : historico.todos())
            if (o.hospitalId() == id) throw new FalhaDominio(CONFLITO, "Hospital possui registros no histórico.");
        return hospitais.remover(id).orElseThrow();
    }
    public synchronized Hospital[] percorrer(ArvoreHospitais.Ordem ordem) { return hospitais.todos(ordem); }
    public synchronized Requisicao solicitar(Requisicao requisicao) {
        Objects.requireNonNull(requisicao);
        consultarHospital(requisicao.hospitalId());
        for (Operacao o : historico.todos())
            if (o.requisicaoId() == requisicao.id()) throw new FalhaDominio(CONFLITO, "ID já registrado no histórico.");
        if (!requisicoes.inserir(requisicao)) throw new FalhaDominio(CONFLITO, "ID de requisição já está na fila.");
        return requisicao;
    }
    public synchronized Requisicao consultarPrimeira() {
        return requisicoes.consultar().orElseThrow(() -> new FalhaDominio(NAO_ENCONTRADO, "Fila vazia."));
    }
    public synchronized Requisicao cancelarPrimeira(int idEsperado) {
        Requisicao primeira = consultarPrimeira();
        if (primeira.id() != idEsperado) throw new FalhaDominio(CONFLITO, "A primeira requisição mudou. Atualize a tela.");
        return requisicoes.remover().orElseThrow();
    }
    /** Simulação acadêmica: bolsas são indicadas MANUALMENTE por ID pelo operador.
     * Valida existência, contagem e repetição. Não compara ABO/Rh nem validade. */
    public synchronized Operacao concluirPrimeira(int idEsperado, int[] idsInformados) {
        Requisicao primeira = consultarPrimeira();
        if (primeira.id() != idEsperado) throw new FalhaDominio(CONFLITO, "A primeira requisição mudou. Atualize a tela.");
        if (idsInformados == null || idsInformados.length != primeira.quantidade())
            throw new FalhaDominio(INVALIDO, "Informe exatamente " + primeira.quantidade() + " IDs de bolsas.");
        int[] ids = idsInformados.clone();
        for (int i = 0; i < ids.length; i++) {
            for (int j = 0; j < i; j++)
                if (ids[i] == ids[j]) throw new FalhaDominio(INVALIDO, "Não repita o ID de uma bolsa.");
            consultarBolsa(ids[i]);
        }
        // Todas as recusas de domínio acima acontecem antes da alteração do estado.
        Operacao operacao = new Operacao(primeira.id(), primeira.hospitalId(), primeira.quantidade(), primeira.tipo());
        historico.inserir(operacao);
        for (int id : ids) estoque.remover(id);
        requisicoes.descartarInicio();
        return operacao;
    }
    public synchronized Operacao consultarTopo() {
        return historico.consultar().orElseThrow(() -> new FalhaDominio(NAO_ENCONTRADO, "Histórico vazio."));
    }
    public synchronized Operacao removerTopo(int idEsperado) {
        Operacao topo = consultarTopo();
        if (topo.requisicaoId() != idEsperado) throw new FalhaDominio(CONFLITO, "O topo mudou. Atualize a tela.");
        return historico.remover().orElseThrow();
    }
}
