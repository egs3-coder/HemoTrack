# HemoTrack - Histórias de Usuário (Entrega 01)

Documento de especificação de requisitos e critérios de aceitação no padrão BDD (Dado-Quando-Então).

---

## US01 - Requisição Emergencial de Hemocomponentes

**História:**
Como profissional de saúde hospitalar, eu gostaria de solicitar hemocomponentes informando o tipo sanguíneo do paciente e o nível de urgência, para que o hemocentro receba o pedido imediatamente e inicie a separação do material adequado.

**Cenário 1: Solicitação hospitalar enviada com sucesso (Positivo)**
* **Dado** que o profissional hospitalar está autenticado na plataforma HemoTrack
* **Quando** preenche o formulário de requisição com hospital de destino, tipo de hemocomponente ("Concentrado de Hemácias"), tipo sanguíneo ("O+"), quantidade ("2 bolsas") e nível de urgência ("Emergência") e confirma o envio
* **Então** a requisição é cadastrada com status "Pendente de Alocação"
* **E** um identificador único de rastreamento do pedido é gerado e retornado na tela.

**Cenário 2: Tentativa de requisição com campos obrigatórios ausentes (Negativo)**
* **Dado** que o profissional hospitalar está na tela de solicitação
* **Quando** tenta submeter o formulário sem informar o tipo sanguíneo do paciente
* **Então** a requisição NÃO é registrada
* **E** o sistema exibe a mensagem de alerta "O tipo sanguíneo do paciente é obrigatório para validação de compatibilidade".

---

## US02 - Cadastro e Entrada de Bolsas de Sangue no Estoque

**História:**
Como operador do hemocentro, eu gostaria de registrar a entrada de novas bolsas de sangue informando código de identificação, hemocomponente, tipo ABO/Rh e data de validade, para que o estoque permaneça atualizado e rastreável.

**Cenário 1: Cadastro de bolsa válida no estoque (Positivo)**
* **Dado** que o operador do hemocentro está na tela de entrada de estoque
* **Quando** insere os dados da bolsa com código "BOL-9821", tipo "Plaquetas", ABO/Rh "A+", data de coleta válida e data de validade futura
* **Então** a bolsa é adicionada ao inventário do hemocentro com status "Disponível"
* **E** o saldo do respectivo tipo sanguíneo é incrementado no painel.

**Cenário 2: Cadastro de bolsa com data de validade vencida ou retroativa (Negativo)**
* **Dado** que o operador do hemocentro preenche os dados de entrada de uma bolsa
* **Quando** insere uma data de validade anterior à data atual do sistema
* **Então** o registro da bolsa é bloqueado
* **E** o sistema exibe a mensagem "Data de validade inválida. Não é permitido cadastrar bolsas vencidas".

---

## US03 - Alocação Automatizada com Compatibilidade ABO/Rh e Regra FEFO

**História:**
Como operador do hemocentro, eu gostaria que o sistema sugira automaticamente as bolsas compatíveis priorizando as de vencimento mais próximo (FEFO), para que a transfusão seja segura e o descarte por validade seja minimizado.

**Cenário 1: Seleção da bolsa compatível mais próxima do vencimento (Positivo)**
* **Dado** que existe uma requisição para paciente com sangue "B+" e o estoque possui duas bolsas compatíveis: Bolsa X ("B+", validade em 3 dias) e Bolsa Y ("O-", validade em 10 dias)
* **Quando** o algoritmo de alocação processa a requisição
* **Então** o sistema seleciona prioritariamente a Bolsa X (critério FEFO)
* **E** altera o status da bolsa para "Reservada para Despacho".

**Cenário 2: Ausência de bolsas compatíveis no estoque da unidade (Negativo)**
* **Dado** que há uma solicitação de urgência para sangue "AB-" e o hemocentro local não possui bolsas "AB-", "A-", "B-" ou "O-" disponíveis
* **Quando** o sistema tenta realizar a alocação automática
* **Então** a alocação local é rejeitada
* **E** o sistema atualiza o status do pedido para "Estoque Crítico / Necessita Remanejamento" e emite um alerta operacional imediato.

---

## US04 - Otimização de Rota de Entrega via Teoria dos Grafos (Dijkstra)

**História:**
Como coordenador de logística, eu gostaria de calcular a rota mais rápida entre o hemocentro e o hospital requisitante, para que o tempo de transporte seja o menor possível garantindo o atendimento no prazo.

**Cenário 1: Cálculo do menor caminho entre nós da rede hospitalar (Positivo)**
* **Dado** que um pedido foi aprovado para transporte a partir do Hemocentro Central com destino ao Hospital Santa Clara
* **Quando** o coordenador solicita a geração da rota
* **Então** o algoritmo de Dijkstra calcula o caminho de menor custo temporal na malha viária
* **E** exibe o itinerário otimizado, a distância total estimada e o tempo previsto de entrega.

**Cenário 2: Falha na determinação de rota por interdição total da via/nó (Negativo)**
* **Dado** que todos os caminhos cadastrados no grafo para o hospital de destino estão com peso infinito (bloqueados ou indisponíveis)
* **Quando** o cálculo de rota é disparado
* **Então** o sistema não gera o trajeto de navegação
* **E** exibe a mensagem "Nenhuma rota viável encontrada no grafo viário. Verifique os pontos de acesso cadastrados".

---

## US05 - Telemetria e Monitoramento de Temperatura em Trânsito

**História:**
Como responsável pelo controle de qualidade e transporte, eu gostaria de acompanhar em tempo real as leituras de temperatura das caixas térmicas, para que anomalias térmicas sejam detectadas antes que o hemocomponente se torne inviável.

**Cenário 1: Monitoramento com temperatura dentro da faixa segura (Positivo)**
* **Dado** que um lote de Concentrado de Hemácias está em transporte (faixa segura de 2°C a 6°C)
* **Quando** o sensor de telemetria envia leituras consecutivas de 4.2°C via API
* **Então** o painel de rastreamento exibe o status de conservação como "Normal / Seguro"
* **E** o histórico de telemetria do pedido é atualizado com sucesso.

**Cenário 2: Alerta de violação de limite térmico (Negativo)**
* **Dado** que o transporte do lote está em andamento
* **Quando** a telemetria registra uma leitura de 9.5°C por mais de 5 minutos consecutivos
* **Então** o sistema altera o status do transporte para "Alerta Crítico: Desvio Térmico"
* **E** dispara uma notificação urgente para a central de logística e para o hospital de destino.

---

## US06 - Painel Gerencial de Descarte e Eficiência Operacional

**História:**
Como gestor do hemocentro, eu gostaria de visualizar indicadores de bolsas descartadas e tempo médio de atendimento, para que eu possa identificar gargalos e tomar decisões corretivas na cadeia de suprimentos.

**Cenário 1: Visualização consolidada dos indicadores de desempenho (Positivo)**
* **Dado** que o gestor acessa o módulo de relatórios e seleciona o período dos últimos 30 dias
* **Quando** clica em "Gerar Dashboard Operacional"
* **Então** a tela exibe o gráfico de taxa de descarte por validade, o percentual de atendimento no prazo (SLA) e o tempo médio de entrega por região
* **E** disponibiliza a opção de exportar o resumo consolidado.

**Cenário 2: Consulta em intervalo sem movimentações registradas (Negativo)**
* **Dado** que o gestor seleciona um intervalo de datas em que não houve entradas, requisições ou descartes
* **Quando** solicita a exibição dos gráficos
* **Então** os gráficos não exibem dados inconsistentes
* **E** o sistema apresenta o estado vazio com a mensagem "Nenhum registro encontrado para o período selecionado".

---

## US07 - Análise Preditiva de Demanda por Tipo Sanguíneo

**História:**
Como analista de dados do sistema, eu gostaria de visualizar as projeções de demanda futura e os desvios estatísticos de consumo, para que as campanhas de doação sejam direcionadas aos tipos sanguíneos com maior risco de desabastecimento.

**Cenário 1: Projeção de demanda calculada com parâmetros estatísticos (Positivo)**
* **Dado** que a base histórica de consumo dos últimos meses foi processada
* **Quando** o analista acessa o módulo de "Demanda Preditiva" para o próximo trimestre
* **Então** o sistema plota a média estimada ($\mu$) e as faixas de dispersão ($\sigma$) para cada tipo sanguíneo (A, B, AB, O, Rh+/-)
* **E** destaca os tipos com probabilidade de ruptura de estoque (ex: O-).

**Cenário 2: Histórico insuficiente para projeção estatística (Negativo)**
* **Dado** que uma nova unidade hospitalar possui menos de 10 dias de histórico cadastrado
* **Quando** o analista tenta gerar a previsão trimestral exclusiva para essa unidade
* **Então** o modelo preditivo não executa a projeção
* **E** exibe a mensagem "Volume histórico de dados insuficiente para calcular a dispersão e demanda preditiva da unidade".
