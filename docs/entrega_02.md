# 🩸 Entrega 02 — Estoque de Bolsas e Alocação FEFO

Nesta entrega do **HemoTrack** foram implementadas funcionalidades relacionadas ao **controle de estoque de bolsas de sangue** e à **alocação de bolsas para requisições hospitalares**.

As duas principais histórias implementadas foram:

- 📦 **US02 — Cadastro e Entrada de Bolsas de Sangue no Estoque**
- 🔄 **US03 — Alocação Automatizada com Compatibilidade ABO/Rh e Regra FEFO**

> 💻 A aplicação foi desenvolvida utilizando **Java 17**, **Spring Boot**, **Spring MVC**, **Spring Data JPA**, **Thymeleaf** e banco de dados **H2**.

---

# 📚 Histórias Implementadas

## 📦 US02 — Cadastro e Entrada de Bolsas de Sangue no Estoque

### 📖 História

> 👤 **Como** operador do hemocentro,  
> 🩸 **eu gostaria de** registrar a entrada de novas bolsas de sangue informando código de identificação, hemocomponente, tipo ABO/Rh, data de coleta e data de validade,  
> 🎯 **para que** o estoque permaneça atualizado e rastreável.

---

### ✅ Cenário 1 — Cadastro de bolsa válida no estoque

**Dado** que o operador do hemocentro está na tela de entrada de estoque  
**Quando** informa os dados de uma nova bolsa de sangue  
**E** informa código, hemocomponente, tipo sanguíneo, data de coleta e data de validade  
**Então** a bolsa é cadastrada no sistema  
**E** recebe automaticamente o status **"Disponível"**  
**E** passa a aparecer na listagem de estoque.

---

### ❌ Cenário 2 — Validação das datas da bolsa

**Dado** que o operador está cadastrando uma nova bolsa  
**Quando** informa uma data de validade que não seja posterior à data de coleta  
**Então** o sistema não permite o cadastro  
**E** apresenta uma mensagem informando que a data de validade é inválida.

---

## ⚙️ Funcionalidades Implementadas

A funcionalidade de cadastro permite registrar:

- 🆔 Código de identificação da bolsa;
- 🔢 Quantidade de bolsas;
- 🩸 Hemocomponente;
- 🅰️ Tipo sanguíneo ABO/Rh;
- 📅 Data de coleta;
- ⏳ Data de validade;
- 🟢 Estado da bolsa.

O sistema também permite realizar o **cadastro em lote**.

### 📝 Exemplo

Por exemplo, ao informar:

```text
Código: BOL-9821
Quantidade: 3
```

O sistema gera automaticamente:

```text
BOL-9821-001
BOL-9821-002
BOL-9821-003
```

> 💡 Isso evita a necessidade de cadastrar individualmente várias bolsas com as mesmas características.

---

## 🔍 Validações Realizadas

Durante o cadastro o sistema verifica:

- ✅ se a quantidade é maior que zero;
- ✅ se o código foi informado;
- ✅ se já existe uma bolsa com o mesmo código;
- ✅ se o hemocomponente foi informado;
- ✅ se o tipo sanguíneo foi informado;
- ✅ se a data de coleta foi informada;
- ✅ se a data de validade foi informada;
- ✅ se a data de validade é posterior à data de coleta.

Após um cadastro válido, a bolsa recebe automaticamente:

```text
Status: Disponível
```

---

## 📁 Principais Arquivos Envolvidos

```text
src/main/java/br/com/hemotrack/modelo/BolsaSangue.java
src/main/java/br/com/hemotrack/modelo/EstadoBolsa.java
src/main/java/br/com/hemotrack/modelo/TipoSanguineo.java
src/main/java/br/com/hemotrack/modelo/TipoHemocomponente.java

src/main/java/br/com/hemotrack/controlador/ControladorBolsa.java

src/main/java/br/com/hemotrack/servico/ServicoBolsa.java

src/main/java/br/com/hemotrack/repositorio/RepositorioBolsa.java

src/main/resources/templates/bolsas/formulario.html
src/main/resources/templates/bolsas/lista.html
```

---

## 🔄 Fluxo da US02

```text
👤 Operador
     ↓
🖥️ Tela "Nova Bolsa"
     ↓
🎮 ControladorBolsa
     ↓
⚙️ ServicoBolsa
     ↓
🔍 Validação dos dados
     ↓
📦 RepositorioBolsa
     ↓
🗄️ Banco H2
     ↓
✅ Bolsa cadastrada como DISPONÍVEL
```

---

# 🔄 US03 — Alocação Automatizada com Compatibilidade ABO/Rh e Regra FEFO

### 📖 História

> 👤 **Como** operador do hemocentro,  
> 🩸 **eu gostaria de** que o sistema selecione automaticamente bolsas compatíveis com o tipo sanguíneo do paciente, priorizando aquelas com vencimento mais próximo,  
> 🎯 **para que** a transfusão seja realizada de forma segura e o desperdício de bolsas por vencimento seja reduzido.

---

### ✅ Cenário — Seleção da bolsa compatível mais próxima do vencimento

**Dado** que existe uma requisição para um paciente com sangue **B+**  
**E** existem bolsas compatíveis disponíveis no estoque  
**Quando** o operador solicita a alocação  
**Então** o sistema verifica a compatibilidade **ABO/Rh**  
**E** considera apenas bolsas que estejam com status **Disponível**  
**E** considera apenas bolsas do hemocomponente solicitado  
**E** ordena as bolsas pela data de validade  
**E** seleciona primeiro a bolsa que vence mais cedo  
**E** altera o status da bolsa selecionada para **"Reservada para despacho"**.

---

## 🧪 Exemplo

Considere uma requisição para um paciente:

```text
Tipo sanguíneo: B+
```

E o estoque:

| 🩸 Bolsa | 🅰️ Tipo | ⏳ Validade | ✅ Compatível |
|----------|----------|-------------|---------------|
| Bolsa X | B+ | 3 dias | ✅ Sim |
| Bolsa Y | O- | 10 dias | ✅ Sim |
| Bolsa Z | A+ | 2 dias | ❌ Não |

Mesmo que a **Bolsa Z** tenha vencimento mais próximo, ela não poderá ser utilizada por não ser compatível.

Entre as bolsas compatíveis, o sistema seleciona:

```text
Bolsa X — B+ — validade em 3 dias
```

> ✅ A **Bolsa X** é selecionada pois possui a menor data de validade entre as bolsas compatíveis.

---

# 🩸 Compatibilidade ABO/Rh

A verificação de compatibilidade está implementada na classe:

```text
ServicoCompatibilidade.java
```

O sistema considera as seguintes possibilidades de doação:

| 👤 Paciente | 🩸 Bolsas aceitas |
|-------------|------------------|
| **O-** | O- |
| **O+** | O-, O+ |
| **A-** | O-, A- |
| **A+** | O-, O+, A-, A+ |
| **B-** | O-, B- |
| **B+** | O-, O+, B-, B+ |
| **AB-** | O-, A-, B-, AB- |
| **AB+** | Todos os tipos |

---

## 📝 Exemplo Implementado

```text
Paciente: B+

✅ Compatíveis:
O-
O+
B-
B+

❌ Não compatíveis:
A-
A+
AB-
AB+
```

---

# ⏳ Regra FEFO

A estratégia utilizada para selecionar as bolsas é a **FEFO — First Expired, First Out**.

Em português:

> 🥇 **Primeiro que vence, primeiro que sai.**

Após localizar as bolsas compatíveis, o sistema realiza a ordenação utilizando a data de validade.

### 💻 Trecho correspondente da lógica

```java
.sorted(Comparator.comparing(BolsaSangue::getDataValidade))
```

Dessa forma, uma bolsa que vence antes possui prioridade sobre uma bolsa com validade mais distante.

---

## 🔄 Processo da Regra FEFO

```text
🏥 Requisição hospitalar
          ↓
🩸 Identificar hemocomponente solicitado
          ↓
📦 Buscar bolsas DISPONÍVEIS
          ↓
🔍 Verificar compatibilidade ABO/Rh
          ↓
📅 Ordenar por data de validade
          ↓
⏳ Aplicar FEFO
          ↓
✅ Selecionar bolsa(s)
          ↓
🔒 Alterar status para
RESERVADA_PARA_DESPACHO
```

---

# 🚦 Status das Bolsas

As bolsas podem possuir os seguintes estados:

```text
🟢 DISPONIVEL
🟡 RESERVADA_PARA_DESPACHO
🔵 DISTRIBUIDA
🔴 VENCIDA
⚫ DESCARTADA
```

Na funcionalidade de alocação desta entrega, uma bolsa inicialmente:

```text
DISPONIVEL
```

passa para:

```text
RESERVADA_PARA_DESPACHO
```

quando é associada a uma requisição.

---

# 📦 Alocação de Múltiplas Bolsas

O sistema também permite informar a quantidade de bolsas que devem ser alocadas para uma requisição.

Antes da alocação são verificadas:

- 🔢 quantidade solicitada;
- 📋 quantidade ainda pendente na requisição;
- 📦 quantidade de bolsas compatíveis no estoque;
- 🩸 hemocomponente solicitado;
- 🅰️ compatibilidade ABO/Rh;
- ✅ disponibilidade da bolsa.

Caso não exista estoque suficiente, o sistema apresenta uma mensagem de erro.

### ⚠️ Exemplo

```text
Estoque insuficiente.

Existem 2 bolsas compatíveis disponíveis,
mas foram solicitadas 3.
```

---

# 📋 Estado das Requisições

Dependendo da quantidade de bolsas atendidas, uma requisição pode ficar como:

### 🟡 Nenhuma bolsa alocada

```text
PENDENTE_ALOCACAO
```

Quando nenhuma bolsa foi alocada.

### 🟠 Parte da solicitação atendida

```text
PARCIALMENTE_ALOCADA
```

Quando somente parte da quantidade solicitada foi atendida.

### 🟢 Solicitação completamente atendida

```text
ALOCADA
```

Quando toda a quantidade solicitada foi atendida.

---

# 📁 Principais Arquivos da US03

```text
src/main/java/br/com/hemotrack/servico/ServicoAlocacao.java
src/main/java/br/com/hemotrack/servico/ServicoCompatibilidade.java

src/main/java/br/com/hemotrack/modelo/Alocacao.java
src/main/java/br/com/hemotrack/modelo/Requisicao.java
src/main/java/br/com/hemotrack/modelo/BolsaSangue.java
src/main/java/br/com/hemotrack/modelo/EstadoBolsa.java
src/main/java/br/com/hemotrack/modelo/EstadoRequisicao.java

src/main/java/br/com/hemotrack/repositorio/RepositorioAlocacao.java
src/main/java/br/com/hemotrack/repositorio/RepositorioBolsa.java
src/main/java/br/com/hemotrack/repositorio/RepositorioRequisicao.java

src/main/java/br/com/hemotrack/controlador/ControladorRequisicao.java

src/main/resources/templates/requisicoes/lista.html
src/main/resources/templates/requisicoes/formulario.html
```

---

# 🧪 Testes

A aplicação possui testes para verificar regras de compatibilidade sanguínea.

### 📄 Arquivo

```text
src/test/java/br/com/hemotrack/servico/ServicoCompatibilidadeTeste.java
```

Entre os casos testados estão:

```text
✅ B+ → B+ = compatível
✅ O- → B+ = compatível
❌ A+ → B+ = incompatível
```

---

# 🛠️ Tecnologias Utilizadas

- ☕ **Java 17**
- 🍃 **Spring Boot 3.5**
- 🌐 **Spring MVC**
- 💾 **Spring Data JPA**
- 🍃 **Thymeleaf**
- 🗄️ **H2 Database**
- 📦 **Maven**
- 🧪 **JUnit 5**
- 🌐 **HTML**
- 🎨 **CSS**

---

# 🏗️ Estrutura Utilizada

O projeto segue uma separação em camadas:

```text
📂 controlador/
    └── Controladores responsáveis pelas requisições HTTP

📂 modelo/
    └── Entidades e enumerações do domínio

📂 repositorio/
    └── Interfaces Spring Data JPA responsáveis pelo banco

📂 servico/
    └── Regras de negócio da aplicação

📂 resources/templates/
    └── Páginas Thymeleaf

📂 resources/static/
    └── Arquivos CSS
```

---

## 🔄 Arquitetura Simplificada

```text
🌐 Interface HTML
        ↓
🎮 Controller
        ↓
⚙️ Service
        ↓
📦 Repository
        ↓
🗄️ Banco de Dados H2
```

---

# 🔀 Versionamento

O desenvolvimento da **Entrega 02** utiliza **Git** e **GitHub** para controle de versão.

Os commits devem registrar alterações relacionadas ao desenvolvimento das funcionalidades durante as semanas da entrega.

### 📝 Exemplos de Commits

```text
feat: implementar cadastro de bolsas

feat: adicionar validações no cadastro de bolsas

feat: implementar compatibilidade ABO Rh

feat: implementar regra FEFO na alocação

feat: adicionar reserva de bolsas para despacho

test: adicionar testes de compatibilidade sanguínea

fix: corrigir validação de estoque na alocação
```

---

# 🐛 Issue / Bug Tracker

As atividades, melhorias e possíveis erros encontrados durante a implementação são registrados utilizando o sistema de **Issues do GitHub**.

### 📌 Exemplos de Issues da Entrega 02

```text
📦 US02 - Implementar cadastro de bolsas

✅ US02 - Validar dados de entrada da bolsa

🩸 US03 - Implementar matriz de compatibilidade ABO/Rh

⏳ US03 - Implementar ordenação FEFO

🔄 US03 - Atualizar status da bolsa após alocação

🐛 BUG - Impedir alocação acima do estoque disponível
```

---

## 📸 Evidência do Issue Tracker

Adicionar abaixo uma captura de tela das **Issues utilizadas durante a entrega**:

```markdown
![Issues da Entrega 02](docs/issues-entrega02.png)
```

> 📌 Coloque a imagem das Issues dentro da pasta `docs` do projeto com o nome `issues-entrega02.png`.

---

# 🎥 Screencast — Funcionamento do Sistema

Vídeo demonstrando a aplicação **Spring Boot** em execução e as duas histórias implementadas.

## 📋 Conteúdo Apresentado

1. ▶️ Inicialização da aplicação;
2. 🏠 Acesso à tela inicial do HemoTrack;
3. 📦 Acesso ao estoque;
4. ➕ Cadastro de uma nova bolsa;
5. 👁️ Visualização da bolsa cadastrada;
6. 📋 Criação/visualização de uma requisição;
7. 🩸 Demonstração da compatibilidade ABO/Rh;
8. 🔄 Alocação da bolsa;
9. ⏳ Demonstração da regra FEFO;
10. 🔒 Alteração do status para **Reservada para despacho**.

---

## 🔗 Link do Vídeo

```text
COLOCAR_LINK_DO_YOUTUBE_AQUI
```

---

# 🎬 Screencast — Explicação do Código

Vídeo apresentando a implementação da aplicação **Spring Boot** e as principais classes utilizadas nas histórias **US02** e **US03**.

---

## 📦 Arquivos Sugeridos para Apresentar — US02

```text
BolsaSangue.java
ControladorBolsa.java
ServicoBolsa.java
RepositorioBolsa.java
formulario.html
lista.html
```

---

## 🔄 Arquivos Sugeridos para Apresentar — US03

```text
ServicoCompatibilidade.java
ServicoAlocacao.java
ControladorRequisicao.java
RepositorioBolsa.java
Alocacao.java
EstadoBolsa.java
```

---

## 🔗 Link do Vídeo

```text
COLOCAR_LINK_DO_YOUTUBE_AQUI
```

---

# 🏆 Resultado da Entrega 02

Ao final desta entrega, o **HemoTrack** permite:

- ✅ cadastrar novas bolsas de sangue;
- ✅ cadastrar várias bolsas em lote;
- ✅ identificar individualmente cada bolsa;
- ✅ controlar tipo sanguíneo e hemocomponente;
- ✅ controlar data de coleta e validade;
- ✅ listar as bolsas cadastradas;
- ✅ controlar o estado das bolsas;
- ✅ cadastrar e visualizar requisições;
- ✅ verificar compatibilidade ABO/Rh;
- ✅ localizar bolsas compatíveis disponíveis;
- ✅ aplicar a estratégia **FEFO**;
- ✅ reservar bolsas para despacho;
- ✅ realizar alocação parcial ou completa de uma requisição;
- ✅ impedir alocações superiores ao estoque compatível disponível.

---

> 🎯 Essas funcionalidades formam a base do **controle de estoque e atendimento das requisições hospitalares do HemoTrack**.

---

# ✅ Entrega 02 — Concluída

**HemoTrack 🩸 — Sistema de Controle de Bolsas de Sangue e Requisições Hospitalares**
