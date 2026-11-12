# 🧠 Unidade 2 — Algoritmos no HemoTrack

Este documento mostra **onde** estão os quatro algoritmos pedidos na U2, **como** eles se ligam no
fluxo de um pedido e **qual a complexidade de cada um, justificada pelo nosso próprio código**.

Todos os algoritmos foram escritos por nós, no pacote
[`br.com.hemotrack.algoritmo`](../hemotrack/src/main/java/br/com/hemotrack/algoritmo). Eles **não usam**
`HashMap`, `PriorityQueue`, `LinkedList`, `Collections.sort` nem `ORDER BY` do banco.

| # | Algoritmo pedido | Classe | O que substituiu |
|---|---|---|---|
| 1 | Rota por **vizinho mais próximo** | [`RoteirizadorVizinhoMaisProximo`](../hemotrack/src/main/java/br/com/hemotrack/algoritmo/RoteirizadorVizinhoMaisProximo.java) | Dijkstra com `PriorityQueue` + `HashMap` no `RotaService` |
| 2 | **FEFO** por inserção ordenada | [`ListaFefo`](../hemotrack/src/main/java/br/com/hemotrack/algoritmo/ListaFefo.java) | `...OrderByDataValidadeAsc` (ORDER BY do banco) |
| 3 | **Índice de estoque por hash** | [`TabelaHashEstoque`](../hemotrack/src/main/java/br/com/hemotrack/algoritmo/TabelaHashEstoque.java) | filtro `TipoSanguineoIn` feito pelo banco |
| 4 | Compatibilidade ABO/Rh por **matriz 8×8** | [`MatrizCompatibilidade`](../hemotrack/src/main/java/br/com/hemotrack/algoritmo/MatrizCompatibilidade.java) | regra de antígenos em `TipoSanguineo.podeReceberDe` |

---

## 🔀 Integração: o que acontece com um pedido

Na tela do pedido, o botão **"Alocar e gerar rota"** chama `RequisicaoService.atender(id)`, que roda o fluxo
completo (os botões separados "Processar alocação" e "Gerar rota" continuam funcionando e usam os mesmos algoritmos):

```
RequisicaoService.atender(id)
 ├─ alocar(id)
 │   ├─ bolsaRepository.findByStatus(DISPONIVEL)        ← o banco só filtra pelo status (sem ORDER BY)
 │   └─ selecionarBolsas(...)
 │       ├─ (1) TabelaHashEstoque.construir(bolsas)       → cada bolsa entra na ListaFefo da sua chave
 │       │                                                  (componente, tipo) por INSERÇÃO ORDENADA
 │       ├─ (2) MatrizCompatibilidade.doadoresCompativeis → coluna do receptor na matriz 8×8
 │       ├─ (3) para cada doador: tabela.buscar(...)      → lista FEFO daquele tipo; pula as vencidas
 │       └─ (4) q vezes: compara as CABEÇAS das listas e retira a que vence primeiro
 └─ gerarRota(id)
     └─ RotaService.calcular → RoteirizadorVizinhoMaisProximo.calcular(trechos, hemocentro, hospital)
```

Se faltar bolsa compatível, nada é reservado, o pedido continua "Pendente de Alocação" e a rota não é calculada.

**Regra de escolha:** entre **todos** os tipos compatíveis, sai primeiro a bolsa que vence primeiro.
Cada lista já está ordenada, então basta comparar as cabeças (no máximo 8), como na intercalação do Merge Sort.

---

## ⏱️ Complexidade (justificada pelo código)

**Variáveis usadas**

| Símbolo | Significado |
|---|---|
| `n` | bolsas "Disponível" lidas do banco |
| `m` | bolsas de uma mesma chave (mesmo hemocomponente **e** mesmo tipo), ou seja, o tamanho de uma `ListaFefo` |
| `v` | bolsas vencidas no início de uma lista |
| `q` | bolsas pedidas (1 a 10, regra da US01) |
| `K` | chaves possíveis no hash = 4 hemocomponentes × 8 tipos = **32** |
| `M` | posições do vetor do hash = **11** |
| `P` | pontos da malha (hemocentro + hospitais) |
| `T` | trechos cadastrados |

### 4) Matriz ABO/Rh 8×8 — `MatrizCompatibilidade`

| Operação | Custo | Por quê (no nosso código) |
|---|---|---|
| `compativel(componente, doador, receptor)` | **O(1)** | Uma linha só: `matrizDe(componente)[doador.ordinal()][receptor.ordinal()] == 1`. `matrizDe` é um `if`, e o acesso `[i][j]` a um vetor é direto. **Não tem laço.** |
| `doadoresCompativeis(componente, receptor)` | **O(1)** | Dois `for (int doador = 0; doador < N_TIPOS; doador++)`, com `N_TIPOS = 8`. São sempre 16 passos, qualquer que seja o estoque: o número de tipos sanguíneos é fixo. |
| Memória | O(1) | Duas matrizes `int[8][8]` (hemácias e plasma) = 128 inteiros fixos. |

A antiga regra de antígenos também era O(1). A matriz troca 3 comparações lógicas por **1 acesso indexado** e
deixa a regra visível na tabela. O teste `matrizConfereComRegraDeAntigenos` confere as **64** combinações
contra a regra antiga.

### 2) FEFO por inserção ordenada — `ListaFefo`

| Operação | Custo | Por quê (no nosso código) |
|---|---|---|
| `inserir(bolsa)`, pior caso | **O(m)** | `while (atual.prox != null && !venceAntes(bolsa, atual.prox.bolsa)) atual = atual.prox;` anda um nó por volta e, no máximo, passa pelos `m` nós (quando a bolsa nova vence depois de todas e vai para o fim). |
| `inserir(bolsa)`, melhor caso | O(1) | Se `venceAntes(bolsa, cabeca.bolsa)`, entra no `if` e vira a nova cabeça sem percorrer nada. |
| `removerPrimeira()` | **O(1)** | Só `cabeca = cabeca.prox`. A bolsa que vence primeiro está sempre na cabeça. |
| `primeira()` | O(1) | Devolve `cabeca.bolsa`. |
| `removerVencidas(hoje)` | **O(v)** | `while (cabeca != null && cabeca.bolsa.estaVencida(hoje))`. Como a lista está em ordem de validade, as vencidas formam um **prefixo**: o laço para na primeira bolsa válida e nunca olha o resto da lista. |
| Montar uma lista com `m` bolsas | O(m²) pior / O(m) melhor | São `m` chamadas de `inserir`, e a i-ésima anda até i nós: 1 + 2 + … + m = m(m+1)/2. Se as bolsas chegarem da que vence primeiro para a última, cada uma vai para o fim (pior caso). Se chegarem ao contrário, cada uma vira cabeça (melhor caso). |

### 3) Índice de estoque por hash — `TabelaHashEstoque`

* Chave: `componente.ordinal() * 8 + tipo.ordinal()`, de 0 a 31.
* Função hash: `chave % 11`.
* Colisão: **encadeamento separado**. Cada posição do vetor `slots[11]` aponta para uma cadeia de `Entrada`. Exemplo testado: Hemácias O− (chave 0) e Plaquetas A+ (chave 11) caem no slot 0.

| Operação | Custo | Por quê (no nosso código) |
|---|---|---|
| `chave(...)` e `hash(...)` | O(1) | Uma multiplicação, uma soma e um `%`. |
| `buscar(componente, tipo)` | **O(1 + α)** | `while (atual != null)` percorre **só a cadeia de um slot**. O tamanho médio da cadeia é o fator de carga α = chaves ÷ 11. Como existem no máximo 32 chaves, α ≤ 32 ÷ 11 ≈ 2,9. A busca olha no máximo umas 3 entradas em média, então é **O(1)**. |
| `inserir(bolsa)` | O(1 + α) + **O(m)** | Primeiro acha ou cria a entrada (mesma busca acima; uma entrada nova entra no **início** da cadeia, `entrada.prox = slots[indice]`, em O(1)). Depois chama `ListaFefo.inserir`, que custa O(m). |
| `construir(bolsas)` | **O(n + Σ m²)** | `for (Bolsa bolsa : bolsas)` chama `inserir` n vezes. A parte do hash soma O(n). A parte da FEFO soma o custo de montar cada lista (ver acima). **Pior caso O(n²)**: todas as bolsas na mesma chave, chegando da que vence primeiro para a última. Com as bolsas espalhadas pelas chaves, cada m é bem menor que n. |
| Pior caso do hash | O(K) = O(32) | Se todas as chaves caíssem no mesmo slot, a cadeia teria 32 entradas. Isso ainda é limitado por uma constante, porque K é fixo. |
| Memória | O(M + K + n) | Vetor de 11 posições, até 32 entradas e um nó por bolsa. |

### 1) Rota por vizinho mais próximo — `RoteirizadorVizinhoMaisProximo`

O método `calcular` tem quatro partes, marcadas com `(1)` a `(4)` no código:

| Parte | Custo | Por quê (no nosso código) |
|---|---|---|
| (1) Dar um número a cada ponto | O(T·P) | `for (Trecho trecho : trechos)` chama `registrar` duas vezes. `registrar` usa `indice`, uma **busca sequencial** (`for (int i = 0; i < quantidade; i++)`) no vetor de nomes, que custa O(P). |
| (2) Montar a matriz de adjacência | O(P² + T·P) | O laço duplo `for i … for j` preenche `tempo[P][P]` com "sem ligação" (P²). Depois, para cada trecho, duas buscas `indice` (O(P) cada) acham as posições. |
| (3) Vizinho mais próximo com retrocesso | **O(P²)** | Cada volta do `while (topo > 0)` **empilha** um ponto novo ou **desempilha** um beco sem saída. Cada ponto é marcado em `visitado[]` e entra na pilha **no máximo uma vez**, então são no máximo 2P voltas. Em cada volta, `for (int v = 0; v < p; v++)` varre a linha do ponto atual na matriz (P colunas) procurando o vizinho não visitado de menor tempo. Total: 2P × P = O(P²). |
| (4) Montar a rota | O(P) | `for (int i = 1; i < topo; i++)` soma tempo e km dos trechos da pilha. |
| **Total** | **O(P² + T·P)** | Numa malha esparsa como a nossa (cada ponto liga a poucos outros, T ≈ P), isso fica **O(P²)**. Memória: O(P²) da matriz. |

**Troca que fizemos conscientemente:** Dijkstra com heap custa O((P + T) log P) e **sempre** acha o menor
tempo. O vizinho mais próximo é **guloso**: olha só o próximo passo. É mais simples de seguir, mas pode errar.
O teste `vizinhoMaisProximoEGuloso` mostra isso: o caminho direto leva 20 min, mas o algoritmo vai primeiro ao
Posto X (5 min) e termina em 35 min. Na malha de demonstração (US05) ele acha a rota de 18 min, a mesma do Dijkstra.

O retrocesso existe para o algoritmo não travar: se entra num ponto sem saída, volta e tenta o próximo
vizinho mais próximo (teste `becoSemSaida`).

### Alocação integrada — `RequisicaoService.selecionarBolsas`

| Passo | Custo | Por quê |
|---|---|---|
| Ler as disponíveis | O(n) | `findByStatus(DISPONIVEL)`: o banco só filtra, sem ordenar. |
| Montar o índice | O(n + Σ m²) | `TabelaHashEstoque.construir` (ver acima). |
| Doadores compatíveis | O(1) | Coluna da matriz (8 linhas). |
| Buscar as listas | O(1) por tipo + O(v) | `for (TipoSanguineo doador : doadores)` roda no máximo 8 vezes. Cada volta faz uma `buscar` O(1) no hash e uma `removerVencidas` O(v). |
| Escolher as q bolsas | **O(q)** | `while (escolhidas.size() < q)` roda q vezes. O `for (int i = 0; i < nListas; i++)` interno compara no máximo 8 cabeças, e `removerPrimeira` é O(1). Então são 8·q passos, ou seja, O(q). |
| **Total** | **O(n + Σ m² + v + q)** | O passo dominante é montar o índice. **Pior caso O(n²)**. Depois do índice pronto, escolher as bolsas **não depende de n**. |

`atender(id)` = alocação + rota = **O(n + Σ m² + q) + O(P² + T·P)**.

---

## ✅ Testes

| Arquivo | O que cobre |
|---|---|
| [`US04AlocacaoTest`](../hemotrack/src/test/java/br/com/hemotrack/US04AlocacaoTest.java) | Cenários da US04 com a nova alocação. **Matriz 8×8**: 11 pares compatíveis e 10 incompatíveis de hemácias, 5 + 5 de plasma, conferência das 64 combinações e as colunas da matriz. **Integração**: nunca reserva bolsa incompatível, FEFO entre tipos diferentes sem depender da ordem do banco, pula vencidas, plasma com regra invertida, estoque insuficiente não reserva nada. |
| [`US05RotaTest`](../hemotrack/src/test/java/br/com/hemotrack/US05RotaTest.java) | Cenários da US05 com o vizinho mais próximo, mais o beco sem saída e o caso em que a escolha gulosa não acha o menor tempo. |
| [`U2FluxoIntegradoTest`](../hemotrack/src/test/java/br/com/hemotrack/U2FluxoIntegradoTest.java) | `atender(id)`: pedido → matriz → hash → FEFO → rota, e o caso sem bolsa compatível. |
| [`ListaFefoTest`](../hemotrack/src/test/java/br/com/hemotrack/algoritmo/ListaFefoTest.java) | Inserção no início, no meio e no fim, desempate pelo código, retirada e vencidas no início da lista. |
| [`TabelaHashEstoqueTest`](../hemotrack/src/test/java/br/com/hemotrack/algoritmo/TabelaHashEstoqueTest.java) | Chave e função hash, busca de chave existente e inexistente, colisão no slot 0 e as 32 combinações. |

```bash
cd hemotrack
mvn test
```

> 🩺 Matrizes **didáticas**: não substituem protocolo clínico. As plaquetas seguem a regra das hemácias e o crioprecipitado segue a do plasma.
