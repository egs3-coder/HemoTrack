# HemoTrack - como rodar

Requisitos: Java 17 ou superior e Maven.

```bash
mvn test                 # executa os testes automatizados (um arquivo por historia: US01 ... US11)
mvn spring-boot:run      # sobe a aplicacao em http://localhost:8080
```

> Se voce ja rodou uma versao anterior, **apague a pasta `data`** antes de subir,
> para que os dados de demonstracao novos sejam carregados.

## Usuarios de acesso (senha de todos: hemotrack123)

| Login | Perfil |
|-------|--------|
| hospital | Profissional hospitalar |
| operador | Operador do hemocentro |
| logistica | Coordenador de logística |
| gestor | Gestor do hemocentro |

A conta `marina` dos cenarios da US02 nao vem pronta: crie-a pela tela "Criar conta".

## US02 - Criar conta e "Esqueci minha senha"

- **Criar conta**: enquanto o usuario e digitado, a tela consulta `/cadastro/verificar-usuario`
  e avisa na hora "Já existe uma conta com o usuário ...". A mesma validacao e repetida no envio.
- **Esqueci minha senha** (link na tela de entrada): a pessoa confirma o usuario e o nome completo
  cadastrados e escolhe uma nova senha (mesmas regras: minimo de 6 caracteres e confirmacao igual).
  Em producao o ideal e trocar essa confirmacao por um codigo enviado por e-mail.

## Testes x historias

| Arquivo de teste | Historia |
|------------------|----------|
| US01RequisicaoEmergencialTest | US01 - Requisição emergencial |
| US02ContaEAcessoTest, US02TelasTest | US02 - Conta e acesso (inclui "Esqueci minha senha") |
| US03EntradaDeBolsasTest | US03 - Entrada de bolsas |
| US04AlocacaoTest | US04 - Alocação por vencimento |
| US05RotaTest | US05 - Rota mais rápida |
| US06TemperaturaTest | US06 - Temperatura em trânsito |
| US07PainelGerencialTest | US07 - Painel gerencial |
| US08DemandaPreditivaTest | US08 - Demanda preditiva |
| US09HospitaisTest | US09 - Hospitais conveniados |
| US10DescarteTest | US10 - Descarte de vencidas |
| US11AcompanhamentoTest | US11 - Acompanhamento do pedido |

Cada metodo de teste tem o nome do cenario (`@DisplayName("Cenário 1: ...")`).

## Dados de demonstracao (`hemotrack.dados-demo=true`, padrao)

Com o banco vazio, as telas reproduzem os numeros dos cenarios:

| Tela | O que aparece |
|------|---------------|
| Pedido REQ-0001 (US06/US11) | Em Transporte, Hospital Santa Clara, bolsa BOL-0001, conservação Normal / Seguro |
| Malha de rotas (US05) | rota Hemocentro Central → Hospital Boa Vista → Hospital Santa Clara, 9 km, 18 min |
| Dashboard, últimos 30 dias (US07) | descarte 10%, SLA 90%, Norte 22 min, Sul 35 min |
| Dashboard, últimos 7 dias (US07) | "Não há dados para o período selecionado" |
| Demanda preditiva (US08) | O- 12,0 / 1,63 / Risco de ruptura; A+ 22,0 / 1,63 / Estoque adequado; AB- histórico insuficiente |
| Estoque (US10) | BOL-1001 vencida, BOL-1002 vence em 4 dias, BOL-1003 reservada e vencida |

Observacao: ao executar "Verificar validade agora" (US10) as bolsas BOL-1001 e BOL-1003 sao descartadas
hoje; a partir dai o dashboard passa a contar esses 2 descartes (comportamento correto da regra).
A verificacao automatica roda todo dia as 00:05.

Para recomecar: pare a aplicacao e apague a pasta `data`.
Para comecar sem dados (primeira requisicao = REQ-0001, como no cenario 1 da US01):
use `hemotrack.dados-demo=false`.

## API do sensor (US06)

```bash
curl -X POST http://localhost:8080/api/requisicoes/REQ-0001/leituras \
     -H "Content-Type: application/json" -d '{"temperatura": 4.2}'
```
