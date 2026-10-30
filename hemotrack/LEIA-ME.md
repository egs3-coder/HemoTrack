# HemoTrack - como rodar

Requisitos: Java 17 ou superior e Maven.

```bash
mvn test                 # executa os testes automatizados (um arquivo por historia: US01 ... US11)
mvn spring-boot:run      # sobe a aplicacao em http://localhost:8080
```

## Onde ficam os dados (contas, hospitais, bolsas, pedidos)

O banco H2 e gravado em arquivo, sempre na mesma pasta, independente de onde o VS Code
(ou o terminal) inicia a aplicacao:

| Sistema | Arquivo do banco |
|---------|------------------|
| Windows | `C:\Users\<voce>\hemotrack-dados\hemotrack.mv.db` |
| Linux / Mac | `~/hemotrack-dados/hemotrack.mv.db` |

Ao iniciar, o console mostra a linha `Banco de dados HemoTrack: jdbc:h2:file:... (N conta(s) cadastrada(s))`.
Uma conta criada pela tela "Criar conta" continua valendo nas proximas execucoes: basta entrar com
o mesmo usuario e senha.

> **Por que antes a conta "sumia"?** O endereco do banco era relativo (`./data/hemotrack`). Ele era
> resolvido a partir da pasta em que o VS Code iniciava o Java; rodando de outra pasta (ou de uma copia
> nova do projeto) a aplicacao criava um banco novo, vazio, e a conta criada antes nao era encontrada.

- Para usar outro local, defina a variavel de ambiente `HEMOTRACK_DB` (ex.: `C:/dados/hemotrack`).
- Para **recomecar do zero**, pare a aplicacao e apague a pasta `hemotrack-dados`.
- Console do banco: `http://localhost:8080/h2-console` (abre depois de entrar na plataforma), com a
  JDBC URL exibida no log, usuario `sa` e senha em branco. A tabela `USUARIO` mostra as contas
  (a senha aparece apenas como hash).

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
| US02ContaEAcessoTest, US02TelasTest, US02PersistenciaContaTest | US02 - Conta e acesso (inclui "Esqueci minha senha" e a conta continuar existindo apos reiniciar) |
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

Para recomecar: pare a aplicacao e apague a pasta `hemotrack-dados`.
Para comecar sem dados (primeira requisicao = REQ-0001, como no cenario 1 da US01):
apague a pasta `hemotrack-dados` e use `hemotrack.dados-demo=false`.
Os identificadores seguem a ordem de cadastro, sem saltos (REQ-0001, REQ-0002, ...).

## API do sensor (US06)

```bash
curl -X POST http://localhost:8080/api/requisicoes/REQ-0001/leituras \
     -H "Content-Type: application/json" -d '{"temperatura": 4.2}'
```
