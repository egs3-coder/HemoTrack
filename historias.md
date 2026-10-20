  (`@DisplayName("Cenário N: ...")`) na classe indicada em **Testes automatizados**.

<a id="indice"></a>

## 📑 Índice

| ID | História | Entrega | Testes automatizados |
|:--:|----------|:-------:|----------------------|
| [US01](#us01) | 🚨 Requisição emergencial de hemocomponentes | 03 | `US01RequisicaoEmergencialTest` |
| [US02](#us02) | 🔐 Criação de conta, acesso e recuperação de senha | 03 | `US02ContaEAcessoTest`, `US02TelasTest` |
| [US03](#us03) | 🩸 Cadastro e entrada de bolsas no estoque | 02 | `US03EntradaDeBolsasTest` |
| [US04](#us04) | 🧬 Alocação de bolsas compatíveis com prioridade por vencimento | 02 | `US04AlocacaoTest` |
| [US05](#us05) | 🚑 Rota de entrega mais rápida e despacho do pedido | 04 | `US05RotaTest` |
| [US06](#us06) | 🌡️ Monitoramento de temperatura em trânsito | 04 | `US06TemperaturaTest` |
| [US07](#us07) | 📊 Painel gerencial de descarte e eficiência | 04 | `US07PainelGerencialTest` |
| [US08](#us08) | 📈 Projeção de demanda por tipo sanguíneo | 04 | `US08DemandaPreditivaTest` |
| [US09](#us09) | 🏥 Cadastro de hospitais conveniados | 03 | `US09HospitaisTest` |
| [US10](#us10) | 🗑️ Descarte de bolsas vencidas | 03 | `US10DescarteTest` |
| [US11](#us11) | 📦 Acompanhamento do pedido e confirmação de recebimento | 03 | `US11AcompanhamentoTest` |

Ao final há o [Apêndice](#apendice) com os dados de demonstração que reproduzem os cenários na aplicação.

---

<a id="us01"></a>

## 🚨 US01 - Requisição Emergencial de Hemocomponentes

> **História:** Como profissional de saúde hospitalar, eu gostaria de solicitar hemocomponentes informando o tipo sanguíneo do paciente e o nível de urgência, para que o hemocentro receba o pedido imediatamente e inicie a separação do material adequado.

### 📋 Regras de negócio

- Hospital de destino, hemocomponente, tipo sanguíneo, quantidade e urgência são obrigatórios.
- Hemocomponentes disponíveis: "Concentrado de Hemácias", "Plaquetas", "Plasma Fresco Congelado" e "Crioprecipitado".
- Níveis de urgência: "Rotina", "Urgente" e "Emergência".
- A quantidade deve ser de 1 a 10 bolsas por requisição.
- Só hospitais ativos aparecem na lista de destino (ver US09); um hospital inativo não pode receber requisição.
- Toda requisição nasce com status "Pendente de Alocação" e recebe um identificador único no formato `REQ-0001`, em ordem de cadastro.
- Quando a requisição não é registrada, o formulário volta com a mensagem de erro e com os dados já preenchidos.

### Cenário 1: Solicitação enviada com sucesso

- **Dado** que o profissional hospitalar entrou na plataforma HemoTrack com sua conta
- **E** o hospital "Hospital Santa Clara" está cadastrado e ativo
- **E** ainda não existe nenhuma requisição cadastrada
- **Quando** preenche a requisição com hospital "Hospital Santa Clara", hemocomponente "Concentrado de Hemácias", tipo sanguíneo "O+", quantidade "2" e urgência "Emergência" e confirma o envio
- **Então** a requisição é cadastrada com status "Pendente de Alocação"
- **E** o sistema exibe a tela do pedido com a mensagem "Requisição REQ-0001 cadastrada com status Pendente de Alocação".

### Cenário 2: Requisição sem tipo sanguíneo

- **Dado** que o profissional hospitalar está na tela de solicitação
- **Quando** tenta enviar a requisição com hospital "Hospital Santa Clara", hemocomponente "Concentrado de Hemácias", quantidade "2", urgência "Emergência" e tipo sanguíneo em branco
- **Então** a requisição NÃO é registrada
- **E** o sistema exibe a mensagem "O tipo sanguíneo do paciente é obrigatório para validação de compatibilidade".

### Cenário 3: Quantidade fora do limite

- **Dado** que o profissional hospitalar está na tela de solicitação
- **Quando** informa quantidade "0" (ou "11") com os demais campos preenchidos e confirma o envio
- **Então** a requisição NÃO é registrada
- **E** o sistema exibe a mensagem "A quantidade deve ser de 1 a 10 bolsas".

### Cenário 4: Campos obrigatórios em branco

- **Dado** que o profissional hospitalar está na tela de solicitação
- **Quando** envia a requisição sem hospital, sem hemocomponente ou sem urgência
- **Então** a requisição NÃO é registrada
- **E** o sistema exibe, respectivamente, "O hospital de destino é obrigatório", "O tipo de hemocomponente é obrigatório" ou "O nível de urgência é obrigatório".

### Cenário 5: Hospital inativo

- **Dado** que o hospital "Hospital Santa Clara" foi inativado
- **Quando** uma requisição é enviada para esse hospital
- **Então** a requisição NÃO é registrada
- **E** o sistema exibe a mensagem "O hospital selecionado está inativo".

[↑ Voltar ao índice](#indice)

---

<a id="us02"></a>

## 🔐 US02 - Criação de Conta, Acesso à Plataforma e Recuperação de Senha

> **História:** Como profissional que utiliza o HemoTrack, eu gostaria de criar minha conta, entrar na plataforma com usuário e senha e redefinir minha senha quando a esquecer, para que apenas pessoas identificadas consultem e registrem informações sobre o estoque e os pedidos de sangue.

### 📋 Regras de negócio

- Nome, usuário, perfil, senha e confirmação da senha são obrigatórios para criar a conta.
- O usuário é único, tem pelo menos 3 caracteres e não contém espaços; é gravado em letras minúsculas (digitar "Marina" equivale a "marina").
- A senha tem pelo menos 6 caracteres e é guardada apenas como hash (nunca em texto puro).
- Perfis disponíveis: "Profissional hospitalar", "Operador do hemocentro", "Coordenador de logística" e "Gestor do hemocentro".
- A existência do usuário é verificada antes das regras de senha: quem escolhe um usuário já usado recebe esse aviso primeiro.
- Enquanto o usuário é digitado na tela "Criar conta", o sistema avisa se ele já existe ou é inválido; a validação é repetida no envio.
- Ao criar a conta, a pessoa já entra na plataforma.
- Nenhuma tela do sistema pode ser acessada sem que a pessoa tenha entrado na plataforma (exceto entrada, criação de conta e "Esqueci minha senha"). As telas não ficam guardadas no navegador após a saída.
- "Esqueci minha senha": a pessoa confirma o usuário e o nome completo da conta (sem diferenciar maiúsculas nem espaços extras) e escolhe uma nova senha, que segue as mesmas regras da criação de conta e deve ser diferente da atual. Para não revelar qual dado está errado, a mensagem de erro é única.
- Em caso de erro, as telas voltam com a mensagem e com os dados digitados (exceto as senhas).

### Cenário 1: Criação de conta com sucesso

- **Dado** que não existe conta com o usuário "marina"
- **Quando** a pessoa cria a conta com nome "Marina Costa", usuário "marina", perfil "Profissional hospitalar", senha "segredo123" e confirmação "segredo123"
- **Então** a conta é criada
- **E** a pessoa entra na plataforma e o painel exibe "Marina Costa (Profissional hospitalar)".

### Cenário 2: Usuário já cadastrado

- **Dado** que já existe uma conta com o usuário "marina"
- **Quando** outra pessoa digita o usuário "marina" na tela de criação de conta
- **Então** a tela avisa imediatamente "Já existe uma conta com o usuário marina"
- **E** se a pessoa enviar o formulário, mesmo com uma senha inválida, a conta NÃO é criada
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

### Cenário 9: Redefinição de senha ("Esqueci minha senha")

- **Dado** que existe a conta de usuário "marina", nome "Marina Costa" e senha "segredo123"
- **Quando** a pessoa clica em "Esqueci minha senha" e informa o usuário "marina", o nome "Marina Costa", a nova senha "novaSenha9" e a confirmação "novaSenha9"
- **Então** a senha é redefinida
- **E** o sistema exibe a tela de entrada com a mensagem "Senha redefinida. Entre com o usuário marina e a nova senha"
- **E** a pessoa entra na plataforma com o usuário "marina" e a senha "novaSenha9".

### Cenário 10: Redefinição com dados que não conferem

- **Dado** que existe a conta de usuário "marina" com nome "Marina Costa"
- **Quando** a pessoa informa o usuário "marina" e o nome "Marina Souza" (ou um usuário inexistente) na tela "Esqueci minha senha"
- **Então** a senha NÃO é alterada
- **E** o sistema exibe a mensagem "Não encontramos uma conta com este usuário e nome completo".

### Cenário 11: Nova senha fora das regras

- **Dado** que existe a conta de usuário "marina", nome "Marina Costa" e senha "segredo123"
- **Quando** a pessoa tenta redefinir a senha para "12345", ou com confirmação diferente, ou para a mesma senha "segredo123"
- **Então** a senha NÃO é alterada
- **E** o sistema exibe, respectivamente, "A senha deve ter pelo menos 6 caracteres", "A confirmação não confere com a senha" ou "A nova senha deve ser diferente da senha atual".

### Cenário 12: Campos obrigatórios e formato do usuário

- **Dado** que a pessoa está na tela de criação de conta
- **Quando** deixa em branco o nome, o usuário, o perfil, a senha ou a confirmação
- **Então** a conta NÃO é criada e o sistema exibe "O nome é obrigatório", "O usuário é obrigatório", "O perfil é obrigatório", "A senha é obrigatória" ou "A confirmação da senha é obrigatória"
- **E** se informar o usuário "ma" exibe "O usuário deve ter pelo menos 3 caracteres", e se informar "ma rina" exibe "O usuário não pode conter espaços".

[↑ Voltar ao índice](#indice)

---

<a id="us03"></a>

## 🩸 US03 - Cadastro e Entrada de Bolsas de Sangue no Estoque

> **História:** Como operador do hemocentro, eu gostaria de registrar a entrada de novas bolsas de sangue informando código, hemocomponente, tipo ABO/Rh, data de coleta e data de validade, para que o estoque permaneça atualizado e rastreável.

### 📋 Regras de negócio

- Código, hemocomponente, tipo ABO/Rh e data de validade são obrigatórios; a data de coleta é opcional e não pode ser futura.
- O código da bolsa é único e é gravado em letras maiúsculas.
- A data de validade deve ser posterior à data atual (validade igual a hoje também é bloqueada).
- Toda bolsa entra com status "Disponível".
- O saldo do painel conta, por tipo sanguíneo, apenas bolsas "Disponível" ainda dentro da validade.

### Cenário 1: Cadastro de bolsa válida

- **Dado** que o operador está na tela de entrada de estoque
- **E** o saldo de bolsas "A+" disponíveis é 3
- **Quando** cadastra a bolsa de código "BOL-9821", hemocomponente "Plaquetas", tipo "A+", coleta na data de hoje e validade daqui a 5 dias
- **Então** a bolsa é adicionada ao inventário com status "Disponível"
- **E** o sistema exibe a mensagem "Bolsa BOL-9821 (A+) adicionada ao inventário com status Disponível"
- **E** o saldo de bolsas "A+" no painel passa a ser 4.

### Cenário 2: Bolsa com validade vencida

- **Dado** que o operador está na tela de entrada de estoque
- **Quando** cadastra a bolsa "BOL-9822" com data de validade igual a ontem (ou igual a hoje)
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

## 🧬 US04 - Alocação de Bolsas Compatíveis com Prioridade por Vencimento

> **História:** Como operador do hemocentro, eu gostaria que o sistema sugerisse automaticamente as bolsas compatíveis priorizando as de vencimento mais próximo, para que a transfusão seja segura e o descarte por validade seja minimizado.

### 📋 Regras de negócio

- Só é possível alocar requisições "Pendente de Alocação".
- Só são consideradas bolsas "Disponível", ainda dentro da validade, do hemocomponente solicitado e compatíveis em ABO/Rh com o paciente (o paciente não pode receber antígenos A, B ou Rh que não possui; ex.: "B+" recebe de "B+", "B-", "O+" e "O-"; "O-" só recebe de "O-"; "AB+" recebe de todos).
- Entre as compatíveis, são escolhidas as de vencimento mais próximo, na quantidade pedida.
- Se não houver bolsas suficientes, nenhuma bolsa é reservada e a requisição permanece "Pendente de Alocação".
- As bolsas escolhidas passam para "Reservada para Despacho" e a requisição para "Alocada".

### Cenário 1: Seleção da bolsa compatível mais próxima do vencimento

- **Dado** que existe a requisição "REQ-0001" de 1 bolsa de "Concentrado de Hemácias" para paciente "B+"
- **E** o estoque possui a bolsa "BOL-0001" ("B+", vence em 3 dias) e a bolsa "BOL-0002" ("O-", vence em 10 dias), ambas "Disponível"
- **Quando** o operador clica em "Processar alocação"
- **Então** o sistema seleciona a bolsa "BOL-0001" e exibe "Bolsa BOL-0001 selecionada e reservada para despacho. Requisição Alocada"
- **E** o status da bolsa passa para "Reservada para Despacho"
- **E** a bolsa "BOL-0002" permanece "Disponível"
- **E** o status da requisição passa para "Alocada".

### Cenário 2: Nenhuma bolsa compatível

- **Dado** que existe a requisição "REQ-0002" de 1 bolsa de "Concentrado de Hemácias" para paciente "O-"
- **E** o estoque possui apenas a bolsa "BOL-0003" ("A+", vence em 5 dias)
- **Quando** o operador processa a alocação da requisição
- **Então** nenhuma bolsa é reservada
- **E** a requisição permanece "Pendente de Alocação"
- **E** o sistema exibe a mensagem "Não há bolsas compatíveis disponíveis para o tipo O-".

### Cenário 3: Bolsas compatíveis em quantidade insuficiente

- **Dado** que existe a requisição "REQ-0003" de 3 bolsas de "Concentrado de Hemácias" para paciente "O-"
- **E** o estoque possui apenas 2 bolsas "O-" de "Concentrado de Hemácias" disponíveis
- **Quando** o operador processa a alocação da requisição
- **Então** nenhuma bolsa é reservada
- **E** a requisição permanece "Pendente de Alocação"
- **E** o sistema exibe a mensagem "Há apenas 2 bolsa(s) compatível(is) disponível(is) para o tipo O-; o pedido precisa de 3".

### Cenário 4: Bolsa de outro hemocomponente não é considerada

- **Dado** que existe a requisição de 1 bolsa de "Concentrado de Hemácias" para paciente "B+"
- **E** o estoque possui apenas a bolsa "BOL-0004" de "Plaquetas" ("B+", vence em 2 dias)
- **Quando** o operador processa a alocação da requisição
- **Então** nenhuma bolsa é reservada e a requisição permanece "Pendente de Alocação".

[↑ Voltar ao índice](#indice)

---

<a id="us05"></a>

## 🚑 US05 - Rota de Entrega Mais Rápida e Despacho do Pedido

> **História:** Como coordenador de logística, eu gostaria de saber a rota mais rápida entre o hemocentro e o hospital requisitante e despachar o pedido por ela, para que o tempo de transporte seja o menor possível e o atendimento ocorra no prazo.

### 📋 Regras de negócio

- A malha é formada por trechos cadastrados na tela "Malha de rotas" entre pontos (o "Hemocentro Central" e os hospitais), cada um com tempo (min) e distância (km). Cada trecho vale nos dois sentidos.
- No cadastro de trecho, origem e destino são obrigatórios e diferentes, e tempo e distância devem ser maiores que zero.
- A rota escolhida é a de menor tempo total; a distância exibida é a soma dos trechos dessa rota.
- A rota só pode ser gerada para pedidos "Alocada" e fica registrada no pedido (itinerário, distância total e tempo previsto). O tempo previsto é usado no SLA (US07).
- O pedido só pode ser despachado se estiver "Alocada" e com a rota gerada. Ao despachar, o pedido e suas bolsas passam para "Em Transporte" e o monitoramento de temperatura começa com "Normal / Seguro" (US06).

### Cenário 1: Rota mais rápida passa por um ponto intermediário

- **Dado** que o pedido "REQ-0001" está "Alocada" com destino "Hospital Santa Clara"
- **E** a malha possui os trechos:

| Origem | Destino | Tempo (min) | Distância (km) |
|--------|---------|-------------|----------------|
| Hemocentro Central | Hospital Santa Clara | 25 | 12 |
| Hemocentro Central | Hospital Boa Vista | 10 | 5 |
| Hospital Boa Vista | Hospital Santa Clara | 8 | 4 |

- **Quando** o coordenador clica em "Gerar rota"
- **Então** o sistema exibe a mensagem "Rota gerada: Hemocentro Central → Hospital Boa Vista → Hospital Santa Clara (9 km, 18 min)"
- **E** o pedido exibe o itinerário "Hemocentro Central → Hospital Boa Vista → Hospital Santa Clara", a distância total de "9 km" e o tempo previsto de "18 min".

### Cenário 2: Destino sem ligação na malha

- **Dado** que o pedido "REQ-0003" tem destino "Hospital Ilha Verde"
- **E** não existe nenhum trecho cadastrado que chegue ao "Hospital Ilha Verde"
- **Quando** o coordenador solicita a geração da rota do pedido
- **Então** nenhuma rota é registrada
- **E** o sistema exibe a mensagem "Não existe rota cadastrada entre Hemocentro Central e Hospital Ilha Verde".

### Cenário 3: Despacho sem rota gerada

- **Dado** que o pedido "REQ-0001" está "Alocada" e ainda não teve a rota gerada
- **Quando** o coordenador tenta despachar o pedido
- **Então** o pedido continua "Alocada"
- **E** o sistema exibe a mensagem "Gere a rota de entrega antes de despachar o pedido".

### Cenário 4: Despacho do pedido

- **Dado** que o pedido "REQ-0001" está "Alocada", com a rota gerada e a bolsa "BOL-0001" reservada
- **Quando** o coordenador clica em "Despachar pedido"
- **Então** o sistema exibe a mensagem "Pedido despachado. Status: Em Transporte"
- **E** o pedido e a bolsa "BOL-0001" passam para "Em Transporte"
- **E** o status de conservação do pedido é "Normal / Seguro".

[↑ Voltar ao índice](#indice)

---

<a id="us06"></a>

## 🌡️ US06 - Monitoramento de Temperatura em Trânsito

> **História:** Como responsável pelo controle de qualidade, eu gostaria de acompanhar as leituras de temperatura das caixas térmicas durante o transporte, para que anomalias térmicas sejam detectadas antes que o hemocomponente se torne inviável.

### 📋 Regras de negócio

- Cada hemocomponente tem sua faixa segura (limites inclusos): "Concentrado de Hemácias" 2,0 °C a 6,0 °C; "Plaquetas" 20,0 °C a 24,0 °C; "Plasma Fresco Congelado" e "Crioprecipitado" -40,0 °C a -18,0 °C.
- Só pedidos "Em Transporte" recebem leituras; a temperatura é obrigatória.
- As leituras chegam pelo sensor (API `POST /api/requisicoes/{codigo}/leituras`, sem login, pois quem envia é um equipamento) ou são registradas na tela do pedido.
- Uma leitura fora da faixa muda o status de conservação para "Alerta - Fora da Faixa".
- Duas leituras consecutivas fora da faixa mudam o status para "Comprometido". Um pedido comprometido não volta a ser considerado seguro.
- Uma leitura dentro da faixa após um alerta devolve o status para "Normal / Seguro".
- Toda leitura é guardada no histórico do pedido, com data e hora e a indicação "Dentro da faixa" ou "Fora da faixa".

### Cenário 1: Temperatura dentro da faixa segura

- **Dado** que o pedido "REQ-0001" de "Concentrado de Hemácias" está "Em Transporte"
- **Quando** o sensor envia as leituras "4,2 °C" e "4,2 °C"
- **Então** o pedido exibe o status de conservação "Normal / Seguro"
- **E** o histórico de telemetria do pedido passa a ter 2 leituras ("Histórico de telemetria: 2 leituras").

### Cenário 2: Temperatura fora da faixa (alerta)

- **Dado** que o pedido "REQ-0001" de "Concentrado de Hemácias" está "Em Transporte" com status de conservação "Normal / Seguro"
- **Quando** o sensor envia a leitura "8,5 °C"
- **Então** o pedido exibe o status de conservação "Alerta - Fora da Faixa"
- **E** o sistema exibe o alerta "Temperatura de 8,5 °C fora da faixa segura (2,0 °C a 6,0 °C)"
- **E** a leitura é registrada no histórico marcada como fora da faixa.

### Cenário 3: Duas leituras consecutivas fora da faixa

- **Dado** que o pedido "REQ-0001" está com status de conservação "Alerta - Fora da Faixa" após a leitura "8,5 °C"
- **Quando** o sensor envia a leitura "9,0 °C"
- **Então** o pedido exibe o status de conservação "Comprometido".

### Cenário 4: Leitura no limite da faixa

- **Dado** que o pedido "REQ-0001" está "Em Transporte" com status de conservação "Normal / Seguro"
- **Quando** o sensor envia a leitura "6,0 °C"
- **Então** o status de conservação permanece "Normal / Seguro"
- **E** o sistema exibe a mensagem "Leitura de 6,0 °C registrada".

### Cenário 5: Volta à faixa depois de um alerta

- **Dado** que o pedido "REQ-0001" está com status de conservação "Alerta - Fora da Faixa" após a leitura "8,5 °C"
- **Quando** o sensor envia a leitura "4,0 °C"
- **Então** o status de conservação volta para "Normal / Seguro".

### Cenário 6: Pedido comprometido continua comprometido

- **Dado** que o pedido "REQ-0001" está com status de conservação "Comprometido"
- **Quando** o sensor envia a leitura "4,0 °C"
- **Então** o status de conservação permanece "Comprometido"
- **E** a leitura é registrada no histórico.

### Cenário 7: Leitura de pedido que não está em transporte

- **Dado** que a requisição "REQ-0002" está "Pendente de Alocação"
- **Quando** o sensor envia a leitura "4,2 °C" para ela
- **Então** a leitura NÃO é registrada
- **E** o sistema exibe a mensagem "Só é possível registrar leituras de pedidos em transporte".

[↑ Voltar ao índice](#indice)

---

<a id="us07"></a>

## 📊 US07 - Painel Gerencial de Descarte e Eficiência Operacional

> **História:** Como gestor do hemocentro, eu gostaria de visualizar indicadores de bolsas descartadas e tempo de atendimento, para que eu possa identificar gargalos e tomar decisões corretivas na cadeia de suprimentos.

### 📋 Regras de negócio

- Períodos disponíveis: "Últimos 7 dias", "Últimos 30 dias" e "Últimos 90 dias"; o período inclui o dia de hoje.
- Taxa de descarte = bolsas descartadas com motivo "Validade vencida" ÷ bolsas que saíram do estoque (entregues + descartadas por validade) no período, arredondada para o inteiro mais próximo.
- SLA = pedidos entregues dentro do tempo previsto da rota (US05) ÷ pedidos entregues no período, arredondado para o inteiro mais próximo. Sem pedidos entregues, o SLA é exibido como "-".
- Tempo de entrega = minutos entre o despacho e a confirmação de recebimento; a média é calculada por região do hospital (US09).
- "Exportar resumo" baixa o arquivo `resumo-operacional.csv` (separado por ponto e vírgula) com período, bolsas entregues, bolsas descartadas, taxa de descarte, pedidos entregues, pedidos no prazo, SLA e tempo médio por região.

### Cenário 1: Indicadores dos últimos 30 dias

- **Dado** que nos últimos 30 dias 36 bolsas foram entregues e 4 foram descartadas por validade
- **E** 20 pedidos foram entregues, sendo 18 dentro do tempo previsto
- **E** as entregas da região "Norte" duraram em média 22 min (20 e 24 min) e as da região "Sul" 35 min (30 e 40 min)
- **Quando** o gestor seleciona o período "Últimos 30 dias" e clica em "Gerar Dashboard Operacional"
- **Então** a tela exibe a taxa de descarte por validade de "10%" com "4 descartadas de 40 saídas"
- **E** o percentual de atendimento no prazo (SLA) de "90%" com "18 de 20 pedidos entregues"
- **E** o tempo médio de entrega de "22 min" para "Norte" e "35 min" para "Sul"
- **E** disponibiliza a opção "Exportar resumo", cujo arquivo contém "Taxa de descarte por validade (%);10" e "Atendimento no prazo - SLA (%);90".

### Cenário 2: Período sem movimentação

- **Dado** que não houve entregas nem descartes nos últimos 7 dias
- **Quando** o gestor seleciona o período "Últimos 7 dias" e clica em "Gerar Dashboard Operacional"
- **Então** o sistema exibe a mensagem "Não há dados para o período selecionado".

[↑ Voltar ao índice](#indice)

---

<a id="us08"></a>

## 📈 US08 - Projeção de Demanda por Tipo Sanguíneo

> **História:** Como analista de dados do hemocentro, eu gostaria de visualizar a projeção de demanda e a variação do consumo por tipo sanguíneo, para que as campanhas de doação sejam direcionadas aos tipos com maior risco de desabastecimento.

### 📋 Regras de negócio

- O consumo de um mês é a quantidade de bolsas entregues naquele mês. São analisados os 3 meses completos anteriores ao mês atual.
- O estoque disponível considera apenas bolsas "Disponível" ainda dentro da validade.
- A projeção do próximo mês é a média (μ) do consumo dos 3 meses; a dispersão é o desvio padrão populacional (σ) desses 3 valores. μ é exibida com 1 casa decimal e σ com 2.
- É preciso ter consumo registrado nos 3 meses; caso contrário o tipo mostra "Histórico insuficiente para projeção (mínimo de 3 meses)".
- Um tipo está em "Risco de ruptura" quando o estoque disponível é menor que μ + σ; caso contrário, "Estoque adequado".
- Indicadores complementares por tipo:
  - **Nível de segurança** = μ + σ (1 casa decimal).
  - **Bolsas a captar** = quanto falta para o estoque alcançar μ + σ, arredondado para cima (0 quando adequado).
  - **Cobertura** = estoque ÷ μ × 30, em dias.
  - **Tendência** = último mês comparado ao primeiro: variação acima de 10% é "Alta", abaixo de −10% é "Queda", senão "Estável".
  - **Variabilidade** = σ ÷ μ: até 10% "Baixa", até 25% "Moderada", acima "Alta".
  - **Prioridade de campanha** (só para tipos em risco): "Crítica" quando o estoque é menor que μ; "Alta" quando é menor apenas que μ + σ.
- O topo da tela resume: tipos em risco de ruptura, tipos com estoque adequado, tipos sem histórico suficiente, demanda projetada (soma das médias) e total de bolsas a captar.
- A lista "Prioridade para campanhas de doação" mostra os tipos em risco do que precisa de mais bolsas para o que precisa de menos (no empate, menor cobertura primeiro).
- Os cartões por tipo podem ser filtrados por "Todos", "Em risco", "Adequados" e "Sem histórico".
- O painel do hemocentro também exibe um alerta com os tipos em risco de ruptura.

### Cenário 1: Projeção com destaque de risco

- **Dado** que o consumo de bolsas nos últimos 3 meses e o estoque disponível são:

| Tipo | Mês 1 | Mês 2 | Mês 3 | Estoque disponível |
|------|-------|-------|-------|--------------------|
| O-   | 10    | 14    | 12    | 8                  |
| A+   | 20    | 22    | 24    | 30                 |

- **Quando** o analista acessa o módulo "Demanda preditiva"
- **Então** o sistema exibe para "O-" a média "12,0" e o desvio "1,63"
- **E** exibe para "A+" a média "22,0" e o desvio "1,63"
- **E** destaca "O-" como "Risco de ruptura"
- **E** exibe "A+" como "Estoque adequado".

### Cenário 2: Histórico insuficiente

- **Dado** que o tipo "AB-" possui consumo registrado em apenas 1 mês
- **Quando** o analista acessa o módulo "Demanda preditiva"
- **Então** o sistema exibe para "AB-" a mensagem "Histórico insuficiente para projeção (mínimo de 3 meses)".

### Cenário 3: Estoque exatamente igual a μ + σ

- **Dado** que o tipo "B+" consumiu 10, 10 e 10 bolsas (μ = 10, σ = 0)
- **Quando** o estoque disponível é 10, o sistema exibe "Estoque adequado"
- **E** quando o estoque disponível é 9, o sistema exibe "Risco de ruptura".

### Cenário 4: Indicadores complementares

- **Dado** os dados do Cenário 1
- **Quando** o analista acessa o módulo "Demanda preditiva"
- **Então** o cartão de "O-" exibe μ + σ "13,6", "6 bolsas" a captar, cobertura de "20 dias", tendência "▲ Alta" e variabilidade "Moderada (14%)"
- **E** o cartão de "A+" exibe "0 bolsas" a captar, cobertura de "41 dias" e variabilidade "Baixa (7%)".

### Cenário 5: Prioridade para campanhas de doação

- **Dado** os dados do Cenário 1, o tipo "B-" com consumo 5, 6 e 7 e estoque 4, e o tipo "AB-" do Cenário 2
- **Quando** o analista acessa o módulo "Demanda preditiva"
- **Então** a lista "Prioridade para campanhas de doação" mostra "O-" (prioridade "Crítica", captar ao menos 6 bolsas) e depois "B-" (captar ao menos 3 bolsas)
- **E** o resumo exibe 2 tipos em risco de ruptura, 1 com estoque adequado, 1 sem histórico suficiente, demanda projetada de 40 bolsas e 9 bolsas a captar.

### Cenário 6: Alerta no painel do hemocentro

- **Dado** os dados do Cenário 1
- **Quando** a pessoa abre o painel do hemocentro
- **Então** o painel exibe "Risco de ruptura: O- (captar 6 bolsas)" com o link "Ver demanda preditiva".

[↑ Voltar ao índice](#indice)

---

<a id="us09"></a>

## 🏥 US09 - Cadastro de Hospitais Conveniados

> **História:** Como operador do hemocentro, eu gostaria de cadastrar os hospitais conveniados com seus dados de identificação e região, para que apenas instituições reconhecidas possam solicitar hemocomponentes e as entregas possam ser analisadas por região.

### 📋 Regras de negócio

- Nome, CNPJ e região são obrigatórios.
- O CNPJ pode ser digitado com ou sem pontuação; é guardado com 14 dígitos, é único e exibido no formato "12.345.678/0001-90".
- Todo hospital é cadastrado com situação "Ativo" e pode ser inativado e reativado.
- Hospital inativo não aparece na lista de destino e não pode ser escolhido em novas requisições (US01).

### Cenário 1: Cadastro de hospital com sucesso

- **Dado** que o operador está na tela de cadastro de hospitais
- **Quando** cadastra o hospital "Hospital Santa Clara", CNPJ "12.345.678/0001-90" e região "Norte"
- **Então** o hospital é salvo com situação "Ativo"
- **E** o sistema exibe a mensagem "Hospital Santa Clara cadastrado com situação Ativo"
- **E** passa a aparecer na lista de hospitais de destino da requisição.

### Cenário 2: CNPJ já cadastrado

- **Dado** que já existe um hospital com CNPJ "12.345.678/0001-90"
- **Quando** o operador tenta cadastrar o hospital "Clínica Santa Clara" com o CNPJ "12.345.678/0001-90"
- **Então** o cadastro é bloqueado
- **E** o sistema exibe a mensagem "Já existe um hospital cadastrado com este CNPJ".

### Cenário 3: Hospital inativado

- **Dado** que o hospital "Hospital Boa Vista" está "Ativo"
- **Quando** o operador clica em "Inativar"
- **Então** a situação passa para "Inativo" e o sistema exibe "Hospital Boa Vista agora está Inativo"
- **E** o hospital deixa de aparecer na lista de hospitais de destino da requisição.

### Cenário 4: Hospital reativado

- **Dado** que o hospital "Hospital Boa Vista" está "Inativo"
- **Quando** o operador clica em "Ativar"
- **Então** a situação passa para "Ativo" e o sistema exibe "Hospital Boa Vista agora está Ativo"
- **E** o hospital volta a aparecer na lista de hospitais de destino da requisição.

### Cenário 5: Dados obrigatórios e CNPJ inválido

- **Dado** que o operador está na tela de cadastro de hospitais
- **Quando** deixa o nome, o CNPJ ou a região em branco, ou informa o CNPJ "12.345.678/0001"
- **Então** o cadastro é bloqueado
- **E** o sistema exibe, respectivamente, "O nome do hospital é obrigatório", "O CNPJ do hospital é obrigatório", "A região do hospital é obrigatória" ou "O CNPJ deve possuir 14 dígitos".

[↑ Voltar ao índice](#indice)

---

<a id="us10"></a>

## 🗑️ US10 - Descarte de Bolsas Vencidas

> **História:** Como operador do hemocentro, eu gostaria que as bolsas com validade vencida fossem retiradas do estoque e registradas como descarte, para que nenhuma bolsa imprópria seja alocada a um paciente e as perdas fiquem documentadas.

### 📋 Regras de negócio

- A verificação pode ser acionada pelo operador (botão "Verificar validade agora" na tela de estoque) e também ocorre automaticamente todos os dias às 00:05.
- São verificadas as bolsas "Disponível" e "Reservada para Despacho" com validade anterior à data atual.
- Bolsa vencida passa para "Descartada" com motivo "Validade vencida" e data do descarte.
- Se a bolsa vencida estava reservada, a requisição volta para "Pendente de Alocação", perde a rota gerada e as demais bolsas reservadas para ela voltam a ficar "Disponível".
- Mensagem do resultado: "Nenhuma bolsa vencida encontrada", "1 bolsa descartada por validade" ou "N bolsas descartadas por validade".

### Cenário 1: Bolsa disponível vencida

- **Dado** que a bolsa "BOL-1001" ("A+") está "Disponível" com validade igual a ontem
- **E** a bolsa "BOL-1002" ("A+") está "Disponível" com validade daqui a 4 dias
- **Quando** a verificação de validade é executada
- **Então** a bolsa "BOL-1001" passa para o status "Descartada" com motivo "Validade vencida" e data do descarte igual a hoje
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

## 📦 US11 - Acompanhamento do Pedido e Confirmação de Recebimento

> **História:** Como profissional de saúde hospitalar, eu gostaria de acompanhar a situação do meu pedido e confirmar o recebimento das bolsas, para que eu saiba quando o material chegará e o hemocentro tenha o registro de que a entrega foi concluída.

### 📋 Regras de negócio

- Sequência de status: "Pendente de Alocação" → "Alocada" → "Em Transporte" → "Entregue". A tela do pedido mostra essas etapas, destacando a atual.
- A consulta pelo identificador não diferencia maiúsculas e ignora espaços ("req-0001 " encontra "REQ-0001").
- Só é possível confirmar o recebimento de pedido "Em Transporte".
- Na confirmação, grava-se a data/hora da entrega e as bolsas passam para "Entregue" com a data da entrega.
- O painel do hemocentro mostra a "Situação da operação": requisições "Pendentes de alocação", "Alocadas", "Em transporte" e bolsas disponíveis que "Vencem em até 3 dias".

### Cenário 1: Consulta do pedido pelo identificador

- **Dado** que a requisição "REQ-0001" está "Em Transporte" com a bolsa "BOL-0001"
- **Quando** o profissional consulta o identificador "REQ-0001"
- **Então** o sistema exibe o status "Em Transporte", o hospital "Hospital Santa Clara" e a bolsa "BOL-0001".

### Cenário 2: Confirmação de recebimento

- **Dado** que a requisição "REQ-0001" está "Em Transporte" com a bolsa "BOL-0001"
- **Quando** o profissional clica em "Confirmar recebimento"
- **Então** o sistema exibe a mensagem "Recebimento confirmado. Status: Entregue"
- **E** a requisição passa para o status "Entregue" com a data e hora da confirmação
- **E** a bolsa "BOL-0001" passa para o status "Entregue".

### Cenário 3: Confirmação de pedido que ainda não saiu

- **Dado** que a requisição "REQ-0002" está "Pendente de Alocação"
- **Quando** o profissional tenta confirmar o recebimento
- **Então** o status NÃO é alterado
- **E** o sistema exibe a mensagem "Só é possível confirmar o recebimento de pedidos em transporte".

### Cenário 4: Identificador inexistente

- **Quando** o profissional consulta o identificador "REQ-9999"
- **Então** o sistema exibe a mensagem "Pedido REQ-9999 não encontrado".

### Cenário 5: Consulta sem identificador

- **Quando** o profissional consulta com o identificador em branco
- **Então** o sistema exibe a mensagem "Informe o identificador do pedido (ex.: REQ-0001)".

### Cenário 6: Situação da operação no painel

- **Dado** que existem 1 requisição "Pendente de Alocação", 1 "Alocada" e 1 "Em Transporte"
- **E** 2 bolsas "Disponível" vencem nos próximos 3 dias
- **Quando** a pessoa abre o painel do hemocentro
- **Então** a seção "Situação da operação" exibe 1 em "Pendentes de alocação", 1 em "Alocadas", 1 em "Em transporte" e 2 em "Vencem em até 3 dias".

[↑ Voltar ao índice](#indice)

---

<a id="apendice"></a>

## 🧪 Apêndice - Dados de demonstração

Com `hemotrack.dados-demo=true` (padrão) e o banco vazio (pasta `data` apagada), a aplicação carrega dados que reproduzem os cenários:

| Onde ver | O que aparece | Cenários |
|----------|---------------|----------|
| Entrada | usuários `hospital`, `operador`, `logistica` e `gestor` (senha `hemotrack123`); a conta `marina` é criada pela tela | US02 |
| Malha de rotas | os 3 trechos da tabela da US05 | US05 - Cenário 1 |
| Pedido REQ-0001 | "Em Transporte", "Hospital Santa Clara", bolsa "BOL-0001", conservação "Normal / Seguro", rota de 9 km e 18 min | US06 e US11 - Cenários 1 e 2 |
| Dashboard - Últimos 30 dias | descarte "10%" (4 de 40), SLA "90%" (18 de 20), Norte "22 min", Sul "35 min" | US07 - Cenário 1 |
| Dashboard - Últimos 7 dias | "Não há dados para o período selecionado" | US07 - Cenário 2 |
| Demanda preditiva | O- 10/14/12 com estoque 8 (risco, captar 6); A+ 20/22/24 com estoque 30 (adequado); AB- com consumo em 1 mês | US08 - Cenários 1, 2, 4 e 6 |
| Hospitais | "Hospital Santa Clara" (Norte) e "Hospital Boa Vista" (Sul), ativos | US09 |
| Estoque | "BOL-1001" vencida ontem, "BOL-1002" vence em 4 dias, "BOL-1003" reservada e vencida | US10 - Cenários 1 e 2 |

Observações:
- Com os dados de demonstração os identificadores já começam ocupados (REQ-0001 a REQ-0022). Para reproduzir o Cenário 1 da US01 (primeira requisição = "REQ-0001"), use `hemotrack.dados-demo=false`.
- Ao executar "Verificar validade agora", "BOL-1001" e "BOL-1003" são descartadas hoje e o dashboard passa a contar esses descartes.

[↑ Voltar ao índice](#indice)
