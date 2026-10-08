# HemoTrack - Histórias de Usuário

Este documento reúne as 11 histórias do HemoTrack. Cada história descreve a necessidade de negócio (sem definir a solução técnica) e traz cenários de validação em BDD com dados concretos, para que possam ser automatizados.

<a id="indice"></a>

## Índice

| ID | História | Entrega |
|:--:|----------|:-------:|
| [US01](#us01) | Requisição emergencial de hemocomponentes | 03 |
| [US02](#us02) | Criação de conta e acesso à plataforma | 03 |
| [US03](#us03) | Cadastro e entrada de bolsas no estoque | 02 |
| [US04](#us04) | Alocação de bolsas compatíveis com prioridade por vencimento | 02 |
| [US05](#us05) | Rota de entrega mais rápida até o hospital | 04 |
| [US06](#us06) | Monitoramento de temperatura em trânsito | 04 |
| [US07](#us07) | Painel gerencial de descarte e eficiência | 04 |
| [US08](#us08) | Projeção de demanda por tipo sanguíneo | 04 |
| [US09](#us09) | Cadastro de hospitais conveniados | 03 |
| [US10](#us10) | Descarte de bolsas vencidas | 03 |
| [US11](#us11) | Acompanhamento do pedido e confirmação de recebimento | 03 |

---

<a id="us01"></a>

## US01 - Requisição Emergencial de Hemocomponentes

> **História:** Como profissional de saúde hospitalar, eu gostaria de solicitar hemocomponentes informando o tipo sanguíneo do paciente e o nível de urgência, para que o hemocentro receba o pedido imediatamente e inicie a separação do material adequado.

### Regras de negócio

- Hospital de destino, hemocomponente, tipo sanguíneo, quantidade e urgência são obrigatórios.
- A quantidade deve ser de 1 a 10 bolsas por requisição.
- Toda requisição nasce com status "Pendente de Alocação" e recebe um identificador único no formato `REQ-0001`.

### Cenário 1: Solicitação enviada com sucesso

- **Dado** que o profissional hospitalar entrou na plataforma HemoTrack com sua conta
- **E** o hospital "Hospital Santa Clara" está cadastrado e ativo
- **Quando** preenche a requisição com hospital "Hospital Santa Clara", hemocomponente "Concentrado de Hemácias", tipo sanguíneo "O+", quantidade "2" e urgência "Emergência" e confirma o envio
- **Então** a requisição é cadastrada com status "Pendente de Alocação"
- **E** o identificador "REQ-0001" é exibido na tela.

### Cenário 2: Requisição sem tipo sanguíneo

- **Dado** que o profissional hospitalar está na tela de solicitação
- **Quando** tenta enviar a requisição com hospital "Hospital Santa Clara", hemocomponente "Concentrado de Hemácias", quantidade "2", urgência "Emergência" e tipo sanguíneo em branco
- **Então** a requisição NÃO é registrada
- **E** o sistema exibe a mensagem "O tipo sanguíneo do paciente é obrigatório para validação de compatibilidade".

### Cenário 3: Quantidade fora do limite

- **Dado** que o profissional hospitalar está na tela de solicitação
- **Quando** informa quantidade "0" e confirma o envio
- **Então** a requisição NÃO é registrada
- **E** o sistema exibe a mensagem "A quantidade deve ser de 1 a 10 bolsas".

[↑ Voltar ao índice](#indice)

---

<a id="us02"></a>

## US02 - Criação de Conta e Acesso à Plataforma

> **História:** Como profissional que utiliza o HemoTrack, eu gostaria de criar minha conta e entrar na plataforma com usuário e senha, para que apenas pessoas identificadas consultem e registrem informações sobre o estoque e os pedidos de sangue.

### Regras de negócio

- Nome, usuário, perfil, senha e confirmação da senha são obrigatórios para criar a conta.
- O usuário é único, tem pelo menos 3 caracteres e não contém espaços; a senha tem pelo menos 6 caracteres.
- Perfis disponíveis: "Profissional hospitalar", "Operador do hemocentro", "Coordenador de logística" e "Gestor do hemocentro".
- Nenhuma tela do sistema pode ser acessada sem que a pessoa tenha entrado na plataforma.

### Cenário 1: Criação de conta com sucesso

- **Dado** que não existe conta com o usuário "marina"
- **Quando** a pessoa cria a conta com nome "Marina Costa", usuário "marina", perfil "Profissional hospitalar", senha "segredo123" e confirmação "segredo123"
- **Então** a conta é criada
- **E** a pessoa entra na plataforma e o painel exibe "Marina Costa (Profissional hospitalar)".

### Cenário 2: Usuário já cadastrado

- **Dado** que já existe uma conta com o usuário "marina"
- **Quando** outra pessoa tenta criar uma conta com o usuário "marina"
- **Então** a conta NÃO é criada
- **E** o sistema exibe a mensagem "Já existe uma conta com o usuário marina".

### Cenário 3: Senha curta

- **Dado** que a pessoa está na tela de criação de conta
- **Quando** informa a senha "12345" e a confirmação "12345"
- **Então** a conta NÃO é criada
- **E** o sistema exibe a mensagem "A senha deve ter pelo menos 6 caracteres".

### Cenário 4: Confirmação diferente da senha

- **Dado** que a pessoa está na tela de criação de conta
- **Quando** informa a senha "segredo123" e a confirmação "segredo124"
- **Então** a conta NÃO é criada
- **E** o sistema exibe a mensagem "A confirmação não confere com a senha".

### Cenário 5: Entrada com usuário e senha corretos

- **Dado** que existe a conta de usuário "marina" com senha "segredo123"
- **Quando** a pessoa informa o usuário "marina" e a senha "segredo123" na tela de entrada
- **Então** o sistema exibe o painel do hemocentro
- **E** o menu exibe "Marina Costa (Profissional hospitalar)".

### Cenário 6: Entrada com senha incorreta

- **Dado** que existe a conta de usuário "marina" com senha "segredo123"
- **Quando** a pessoa informa o usuário "marina" e a senha "errada999"
- **Então** o acesso NÃO é liberado
- **E** o sistema exibe a mensagem "Usuário ou senha inválidos".

### Cenário 7: Tentativa de acesso sem entrar na plataforma

- **Dado** que a pessoa não entrou na plataforma
- **Quando** tenta abrir a tela de estoque
- **Então** o sistema exibe a tela de entrada no lugar da tela de estoque.

### Cenário 8: Saída da plataforma

- **Dado** que "Marina Costa" entrou na plataforma
- **Quando** clica em "Sair"
- **Então** o sistema exibe a tela de entrada
- **E** as demais telas voltam a exigir usuário e senha.

[↑ Voltar ao índice](#indice)

---

<a id="us03"></a>

## US03 - Cadastro e Entrada de Bolsas de Sangue no Estoque

> **História:** Como operador do hemocentro, eu gostaria de registrar a entrada de novas bolsas de sangue informando código, hemocomponente, tipo ABO/Rh e data de validade, para que o estoque permaneça atualizado e rastreável.

### Regras de negócio

- O código da bolsa é único.
- A data de validade deve ser posterior à data atual.
- Toda bolsa entra com status "Disponível".

### Cenário 1: Cadastro de bolsa válida

- **Dado** que o operador está na tela de entrada de estoque
- **E** o saldo de bolsas "A+" disponíveis é 3
- **Quando** cadastra a bolsa de código "BOL-9821", hemocomponente "Plaquetas", tipo "A+", coleta na data de hoje e validade daqui a 5 dias
- **Então** a bolsa é adicionada ao inventário com status "Disponível"
- **E** o saldo de bolsas "A+" no painel passa a ser 4.

### Cenário 2: Bolsa com validade vencida

- **Dado** que o operador está na tela de entrada de estoque
- **Quando** cadastra a bolsa "BOL-9822" com data de validade igual a ontem
- **Então** o registro é bloqueado
- **E** o sistema exibe a mensagem "Data de validade inválida. Não é permitido cadastrar bolsas vencidas".

### Cenário 3: Código duplicado

- **Dado** que já existe a bolsa "BOL-9821" no inventário
- **Quando** o operador tenta cadastrar outra bolsa com o código "BOL-9821"
- **Então** o registro é bloqueado
- **E** o sistema exibe a mensagem "Já existe uma bolsa cadastrada com o código BOL-9821".

[↑ Voltar ao índice](#indice)

---

<a id="us04"></a>

## US04 - Alocação de Bolsas Compatíveis com Prioridade por Vencimento

> **História:** Como operador do hemocentro, eu gostaria que o sistema sugerisse automaticamente as bolsas compatíveis priorizando as de vencimento mais próximo, para que a transfusão seja segura e o descarte por validade seja minimizado.

### Regras de negócio

- Só são consideradas bolsas "Disponível", do hemocomponente solicitado e compatíveis em ABO/Rh com o paciente.
- Entre as compatíveis, é escolhida a de vencimento mais próximo.

### Cenário 1: Seleção da bolsa compatível mais próxima do vencimento

- **Dado** que existe a requisição "REQ-0001" de 1 bolsa de "Concentrado de Hemácias" para paciente "B+"
- **E** o estoque possui a bolsa "BOL-0001" ("B+", vence em 3 dias) e a bolsa "BOL-0002" ("O-", vence em 10 dias), ambas "Disponível"
- **Quando** o operador processa a alocação da requisição
- **Então** o sistema seleciona a bolsa "BOL-0001"
- **E** o status da bolsa passa para "Reservada para Despacho"
- **E** o status da requisição passa para "Alocada".

### Cenário 2: Nenhuma bolsa compatível

- **Dado** que existe a requisição "REQ-0002" de 1 bolsa de "Concentrado de Hemácias" para paciente "O-"
- **E** o estoque possui apenas a bolsa "BOL-0003" ("A+", vence em 5 dias)
- **Quando** o operador processa a alocação da requisição
- **Então** nenhuma bolsa é reservada
- **E** a requisição permanece "Pendente de Alocação"
- **E** o sistema exibe a mensagem "Não há bolsas compatíveis disponíveis para o tipo O-".

[↑ Voltar ao índice](#indice)

---

<a id="us05"></a>

## US05 - Rota de Entrega Mais Rápida até o Hospital

> **História:** Como coordenador de logística, eu gostaria de saber a rota mais rápida entre o hemocentro e o hospital requisitante, para que o tempo de transporte seja o menor possível e o atendimento ocorra no prazo.

### Regras de negócio

- A malha é formada por trechos cadastrados entre pontos (hemocentro e hospitais), cada um com tempo (min) e distância (km).
- A rota escolhida é a de menor tempo total; a rota fica registrada no pedido.

### Cenário 1: Rota mais rápida passa por um ponto intermediário

- **Dado** que o pedido "REQ-0001" está "Alocada" com destino "Hospital Santa Clara"
- **E** a malha possui os trechos:

| Origem | Destino | Tempo (min) | Distância (km) |
|--------|---------|-------------|----------------|
| Hemocentro Central | Hospital Santa Clara | 25 | 12 |
| Hemocentro Central | Hospital Boa Vista | 10 | 5 |
| Hospital Boa Vista | Hospital Santa Clara | 8 | 4 |

- **Quando** o coordenador solicita a geração da rota do pedido
- **Então** o sistema exibe o itinerário "Hemocentro Central → Hospital Boa Vista → Hospital Santa Clara"
- **E** exibe a distância total de "9 km" e o tempo previsto de "18 min".

### Cenário 2: Destino sem ligação na malha

- **Dado** que o pedido "REQ-0003" tem destino "Hospital Ilha Verde"
- **E** não existe nenhum trecho cadastrado que chegue ao "Hospital Ilha Verde"
- **Quando** o coordenador solicita a geração da rota do pedido
- **Então** nenhuma rota é registrada
- **E** o sistema exibe a mensagem "Não existe rota cadastrada entre Hemocentro Central e Hospital Ilha Verde".

[↑ Voltar ao índice](#indice)

---

<a id="us06"></a>

## US06 - Monitoramento de Temperatura em Trânsito

> **História:** Como responsável pelo controle de qualidade, eu gostaria de acompanhar as leituras de temperatura das caixas térmicas durante o transporte, para que anomalias térmicas sejam detectadas antes que o hemocomponente se torne inviável.

### Regras de negócio

- Faixa segura do Concentrado de Hemácias: 2,0 °C a 6,0 °C (limites inclusos).
- Uma leitura fora da faixa muda o status de conservação para "Alerta - Fora da Faixa".
- Duas leituras consecutivas fora da faixa mudam o status para "Comprometido".
- Toda leitura é guardada no histórico do pedido.

### Cenário 1: Temperatura dentro da faixa segura

- **Dado** que o pedido "REQ-0001" de "Concentrado de Hemácias" está "Em Transporte"
- **Quando** o sensor envia as leituras "4,2 °C" e "4,2 °C"
- **Então** o painel exibe o status de conservação "Normal / Seguro"
- **E** o histórico de telemetria do pedido passa a ter 2 leituras.

### Cenário 2: Temperatura fora da faixa (alerta)

- **Dado** que o pedido "REQ-0001" de "Concentrado de Hemácias" está "Em Transporte" com status de conservação "Normal / Seguro"
- **Quando** o sensor envia a leitura "8,5 °C"
- **Então** o painel exibe o status de conservação "Alerta - Fora da Faixa"
- **E** o sistema exibe o alerta "Temperatura de 8,5 °C fora da faixa segura (2,0 °C a 6,0 °C)"
- **E** a leitura é registrada no histórico marcada como fora da faixa.

### Cenário 3: Duas leituras consecutivas fora da faixa

- **Dado** que o pedido "REQ-0001" está com status de conservação "Alerta - Fora da Faixa" após a leitura "8,5 °C"
- **Quando** o sensor envia a leitura "9,0 °C"
- **Então** o painel exibe o status de conservação "Comprometido".

### Cenário 4: Leitura no limite da faixa

- **Dado** que o pedido "REQ-0001" está "Em Transporte" com status de conservação "Normal / Seguro"
- **Quando** o sensor envia a leitura "6,0 °C"
- **Então** o status de conservação permanece "Normal / Seguro".

[↑ Voltar ao índice](#indice)

---

<a id="us07"></a>

## US07 - Painel Gerencial de Descarte e Eficiência Operacional

> **História:** Como gestor do hemocentro, eu gostaria de visualizar indicadores de bolsas descartadas e tempo de atendimento, para que eu possa identificar gargalos e tomar decisões corretivas na cadeia de suprimentos.

### Regras de negócio

- Taxa de descarte = bolsas descartadas por validade ÷ bolsas que saíram do estoque (entregues + descartadas) no período.
- SLA = pedidos entregues dentro do tempo previsto ÷ pedidos entregues no período.

### Cenário 1: Indicadores dos últimos 30 dias

- **Dado** que nos últimos 30 dias 36 bolsas foram entregues e 4 foram descartadas por validade
- **E** 20 pedidos foram entregues, sendo 18 dentro do tempo previsto
- **E** as entregas da região "Norte" duraram 20 e 24 min e as da região "Sul" duraram 30 e 40 min
- **Quando** o gestor seleciona o período "Últimos 30 dias" e clica em "Gerar Dashboard Operacional"
- **Então** a tela exibe a taxa de descarte por validade de "10%"
- **E** o percentual de atendimento no prazo (SLA) de "90%"
- **E** o tempo médio de entrega de "22 min" para "Norte" e "35 min" para "Sul"
- **E** disponibiliza a opção "Exportar resumo".

### Cenário 2: Período sem movimentação

- **Dado** que não houve entregas nem descartes nos últimos 7 dias
- **Quando** o gestor seleciona o período "Últimos 7 dias" e clica em "Gerar Dashboard Operacional"
- **Então** o sistema exibe a mensagem "Não há dados para o período selecionado".

[↑ Voltar ao índice](#indice)

---

<a id="us08"></a>

## US08 - Projeção de Demanda por Tipo Sanguíneo

> **História:** Como analista de dados do hemocentro, eu gostaria de visualizar a projeção de demanda e a variação do consumo por tipo sanguíneo, para que as campanhas de doação sejam direcionadas aos tipos com maior risco de desabastecimento.

### Regras de negócio

- A projeção do próximo mês é a média (μ) do consumo dos últimos 3 meses; a dispersão é o desvio padrão (σ) desses 3 valores.
- Um tipo está em "Risco de ruptura" quando o estoque disponível é menor que μ + σ.

### Cenário 1: Projeção com destaque de risco

- **Dado** que o consumo de bolsas nos últimos 3 meses e o estoque disponível são:

| Tipo | Mês 1 | Mês 2 | Mês 3 | Estoque disponível |
|------|-------|-------|-------|--------------------|
| O-   | 10    | 14    | 12    | 8                  |
| A+   | 20    | 22    | 24    | 30                 |

- **Quando** o analista acessa o módulo "Demanda Preditiva"
- **Então** o sistema exibe para "O-" a média "12,0" e o desvio "1,63"
- **E** exibe para "A+" a média "22,0" e o desvio "1,63"
- **E** destaca "O-" como "Risco de ruptura"
- **E** exibe "A+" como "Estoque adequado".

### Cenário 2: Histórico insuficiente

- **Dado** que o tipo "AB-" possui consumo registrado em apenas 1 mês
- **Quando** o analista acessa o módulo "Demanda Preditiva"
- **Então** o sistema exibe para "AB-" a mensagem "Histórico insuficiente para projeção (mínimo de 3 meses)".

[↑ Voltar ao índice](#indice)

---

<a id="us09"></a>

## US09 - Cadastro de Hospitais Conveniados

> **História:** Como operador do hemocentro, eu gostaria de cadastrar os hospitais conveniados com seus dados de identificação e região, para que apenas instituições reconhecidas possam solicitar hemocomponentes e as entregas possam ser analisadas por região.

### Regras de negócio

- Nome, CNPJ e região são obrigatórios; o CNPJ é único e possui 14 dígitos.
- Hospital inativo não pode ser escolhido em novas requisições.

### Cenário 1: Cadastro de hospital com sucesso

- **Dado** que o operador está na tela de cadastro de hospitais
- **Quando** cadastra o hospital "Hospital Santa Clara", CNPJ "12.345.678/0001-90" e região "Norte"
- **Então** o hospital é salvo com situação "Ativo"
- **E** passa a aparecer na lista de hospitais de destino da requisição.

### Cenário 2: CNPJ já cadastrado

- **Dado** que já existe um hospital com CNPJ "12.345.678/0001-90"
- **Quando** o operador tenta cadastrar o hospital "Clínica Santa Clara" com o CNPJ "12.345.678/0001-90"
- **Então** o cadastro é bloqueado
- **E** o sistema exibe a mensagem "Já existe um hospital cadastrado com este CNPJ".

### Cenário 3: Hospital inativado

- **Dado** que o hospital "Hospital Boa Vista" está "Ativo"
- **Quando** o operador inativa o hospital
- **Então** a situação passa para "Inativo"
- **E** o hospital deixa de aparecer na lista de hospitais de destino da requisição.

[↑ Voltar ao índice](#indice)

---

<a id="us10"></a>

## US10 - Descarte de Bolsas Vencidas

> **História:** Como operador do hemocentro, eu gostaria que as bolsas com validade vencida fossem retiradas do estoque e registradas como descarte, para que nenhuma bolsa imprópria seja alocada a um paciente e as perdas fiquem documentadas.

### Regras de negócio

- A verificação pode ser acionada pelo operador e também ocorre periodicamente.
- Bolsa vencida passa para "Descartada" com motivo "Validade vencida" e data do descarte.
- Se a bolsa vencida estava reservada, a requisição volta para "Pendente de Alocação".

### Cenário 1: Bolsa disponível vencida

- **Dado** que a bolsa "BOL-1001" ("A+") está "Disponível" com validade igual a ontem
- **E** a bolsa "BOL-1002" ("A+") está "Disponível" com validade daqui a 4 dias
- **Quando** a verificação de validade é executada
- **Então** a bolsa "BOL-1001" passa para o status "Descartada" com motivo "Validade vencida"
- **E** a bolsa "BOL-1002" permanece "Disponível"
- **E** o sistema informa "1 bolsa descartada por validade".

### Cenário 2: Bolsa reservada vencida

- **Dado** que a bolsa "BOL-1003" está "Reservada para Despacho" para a requisição "REQ-0004" com validade igual a ontem
- **Quando** a verificação de validade é executada
- **Então** a bolsa "BOL-1003" passa para o status "Descartada"
- **E** a requisição "REQ-0004" volta para "Pendente de Alocação".

### Cenário 3: Nenhuma bolsa vencida

- **Dado** que todas as bolsas do estoque têm validade futura
- **Quando** a verificação de validade é executada
- **Então** nenhum status é alterado
- **E** o sistema informa "Nenhuma bolsa vencida encontrada".

[↑ Voltar ao índice](#indice)

---

<a id="us11"></a>

## US11 - Acompanhamento do Pedido e Confirmação de Recebimento

> **História:** Como profissional de saúde hospitalar, eu gostaria de acompanhar a situação do meu pedido e confirmar o recebimento das bolsas, para que eu saiba quando o material chegará e o hemocentro tenha o registro de que a entrega foi concluída.

### Regras de negócio

- Sequência de status: "Pendente de Alocação" → "Alocada" → "Em Transporte" → "Entregue".
- Só é possível confirmar o recebimento de pedido "Em Transporte".
- Na confirmação, grava-se a data/hora da entrega e as bolsas passam para "Entregue".

### Cenário 1: Consulta do pedido pelo identificador

- **Dado** que a requisição "REQ-0001" está "Em Transporte" com a bolsa "BOL-0001"
- **Quando** o profissional consulta o identificador "REQ-0001"
- **Então** o sistema exibe o status "Em Transporte", o hospital "Hospital Santa Clara" e a bolsa "BOL-0001".

### Cenário 2: Confirmação de recebimento

- **Dado** que a requisição "REQ-0001" está "Em Transporte" com a bolsa "BOL-0001"
- **Quando** o profissional confirma o recebimento
- **Então** a requisição passa para o status "Entregue" com a data e hora da confirmação
- **E** a bolsa "BOL-0001" passa para o status "Entregue".

### Cenário 3: Confirmação de pedido que ainda não saiu

- **Dado** que a requisição "REQ-0002" está "Pendente de Alocação"
- **Quando** o profissional tenta confirmar o recebimento
- **Então** o status NÃO é alterado
- **E** o sistema exibe a mensagem "Só é possível confirmar o recebimento de pedidos em transporte".

### Cenário 4: Identificador inexistente

- **Quando** o profissional consulta o identificador "REQ-9999"
- **Então** o sistema exibe a mensagem "Pedido REQ-9999 não encontrado".

[↑ Voltar ao índice](#indice)
