package br.edu.rotavital;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiRotaVital {
    private final RotaVitalServico servico;
    public ApiRotaVital(RotaVitalServico servico) { this.servico = servico; }
    public record Lote(Bolsa modelo, int quantidade) { }
    @PostMapping("/bolsas/lote") @ResponseStatus(HttpStatus.CREATED)
    public Bolsa[] lote(@RequestBody Lote lote) {
        if (lote.modelo() == null) throw new IllegalArgumentException("Modelo obrigatório.");
        return servico.cadastrarLote(lote.modelo(), lote.quantidade());
    }
    public record Conclusao(int requisicaoId, int[] bolsasIds) { }
    @GetMapping("/estado") public RotaVitalServico.Estado estado() { return servico.estado(); }
    @PostMapping("/bolsas") @ResponseStatus(HttpStatus.CREATED)
    public Bolsa cadastrarBolsa(@RequestBody Bolsa bolsa) { return servico.cadastrarBolsa(bolsa); }
    @GetMapping("/bolsas/{id}") public Bolsa consultarBolsa(@PathVariable int id) { return servico.consultarBolsa(id); }
    @DeleteMapping("/bolsas/{id}") public Bolsa removerBolsa(@PathVariable int id) { return servico.removerBolsa(id); }
    @PostMapping("/hospitais") @ResponseStatus(HttpStatus.CREATED)
    public Hospital cadastrarHospital(@RequestBody Hospital hospital) { return servico.cadastrarHospital(hospital); }
    @GetMapping("/hospitais/{id}") public Hospital consultarHospital(@PathVariable int id) { return servico.consultarHospital(id); }
    @DeleteMapping("/hospitais/{id}") public Hospital removerHospital(@PathVariable int id) { return servico.removerHospital(id); }
    @GetMapping("/hospitais/percurso")
    public Hospital[] percurso(@RequestParam(defaultValue="EM_ORDEM") ArvoreHospitais.Ordem ordem) { return servico.percorrer(ordem); }
    @PostMapping("/requisicoes") @ResponseStatus(HttpStatus.CREATED)
    public Requisicao solicitar(@RequestBody Requisicao requisicao) { return servico.solicitar(requisicao); }
    @GetMapping("/requisicoes/primeira") public Requisicao primeira() { return servico.consultarPrimeira(); }
    @DeleteMapping("/requisicoes/primeira/{id}") public Requisicao cancelar(@PathVariable int id) { return servico.cancelarPrimeira(id); }
    @PostMapping("/requisicoes/concluir")
    public Operacao concluir(@RequestBody Conclusao dados) { return servico.concluirPrimeira(dados.requisicaoId(), dados.bolsasIds()); }
    @GetMapping("/historico/topo") public Operacao topo() { return servico.consultarTopo(); }
    @DeleteMapping("/historico/topo/{id}") public Operacao removerTopo(@PathVariable int id) { return servico.removerTopo(id); }
}
