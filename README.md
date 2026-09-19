# HemoTrack adaptado — Rota Vital / Unidade 1

Esta entrega adapta o HemoTrack fornecido à entrega de Estruturas de Dados.
Contém **duas aplicações independentes com as mesmas operações de domínio e as
mesmas telas**: Java com Spring Boot e C/C++ com ponteiros e malloc/free.

A versão C/C++ não chama o Java. A interface HTTP e a interface de terminal usam
as funções implementadas em `c/rota_vital.c`. A versão Java reimplementa os nós e
operações nas classes em `src/main/java/br/edu/rotavital`.

O original enviado era a Entrega 02, com FEFO e compatibilidade ABO/Rh. Essas regras
foram retiradas da adaptação. Datas, hemocomponente, tipo e urgência são dados de
cadastro. As bolsas são escolhidas manualmente; a urgência não ultrapassa FIFO.
Não há algoritmos de roteirização, FEFO, hash ou compatibilidade no domínio.

## Comece aqui — Java no Windows, sem compilar

1. Extraia o ZIP inteiro em uma pasta nova.
2. Tenha **Java 17 ou superior** instalado e disponível no PATH (`java -version`).
3. Abra `EXECUTAR_JAVA.bat`, ou execute na pasta do projeto:

```bat
java -jar executar\hemotrack-u1.jar
```

4. Quando aparecer a mensagem de inicialização, abra **http://127.0.0.1:8080**.
5. Clique em **Carregar exemplo** para preencher um cenário, ou cadastre seus dados.

O JAR já está compilado e contém as dependências. Não precisa instalar banco de
dados ou baixar bibliotecas para executá-lo. O terminal deve permanecer aberto.
Use Ctrl+C para encerrar. O estado fica somente na memória e é perdido ao sair.

## C/C++ no Windows — as mesmas telas ou o terminal

É necessário um compilador **MinGW-w64 com gcc e g++** no PATH. Confira:

```bat
gcc --version
g++ --version
```

Na pasta do projeto, execute:

```bat
compilar_cpp.bat
```

Depois escolha uma interface:

```bat
build-cpp\hemotrack_cpp.exe
```

Abra **http://127.0.0.1:8081** para usar as mesmas telas sem Java.

```bat
build-cpp\hemotrack_cpp.exe --terminal
```

Os atalhos `EXECUTAR_CPP_TELAS.bat` e `EXECUTAR_CPP_TERMINAL.bat` também compilam se
o executável não existir. Depois de editar C/C++, execute compilar_cpp.bat de novo.
Os compiladores e scripts Windows são instruções de reprodução; a compilação e os
testes desta entrega foram realizados no Linux, não em uma máquina Windows.

## C/C++ no Linux / WSL

```bash
bash compilar_cpp.sh
./build-cpp/hemotrack_cpp --terminal
# Ou, para usar o navegador:
./build-cpp/hemotrack_cpp
```

Para outra porta: `./build-cpp/hemotrack_cpp --porta 8082`.
Execute a partir da pasta principal para localizar `src/main/resources/static`.
Também pode informar `--static CAMINHO` para essa pasta.

## Editar e recompilar Java

Abra a pasta inteira no VS Code, IntelliJ ou Eclipse. O ponto de entrada Spring é
`AplicacaoRotaVital.java`. Execute essa classe com o projeto Maven carregado.
Não execute arquivos de entidade ou estrutura isoladamente.

Após editar, gere novamente o JAR:

```bat
mvnw.cmd clean package
java -jar target\rota-vital-1.0.0.jar
```

No Linux/WSL:

```bash
./mvnw clean package
java -jar target/rota-vital-1.0.0.jar
```

A recompilação requer JDK 17+ e internet para baixar Maven/dependências na primeira
vez. Spring Boot 4.1.1 foi escolhido por suportar Java 17 até Java 26, conforme
https://docs.spring.io/spring-boot/system-requirements.html.
O JAR em `executar` é a versão pronta desta entrega; após editar, use o novo JAR de
`target` ou copie-o para `executar/hemotrack-u1.jar`.

## O que existe nas duas versões

- Cadastro, consulta, listagem e remoção de bolsas em lista encadeada.
- Cadastro em lote com códigos e IDs únicos; falha não deixa lote parcial.
- Código, volume, tipo, hemocomponente, coleta e validade da bolsa.
- Cadastro, consulta, remoção e três percursos de hospitais em ABB.
- Nome e cidade dos hospitais.
- Inserção de requisições na fila, consulta e cancelamento da primeira.
- Quantidade, hospital, tipo, hemocomponente e urgência informativa do pedido.
- Conclusão da primeira requisição com IDs de bolsas escolhidos manualmente.
- Histórico em pilha; consulta e retirada somente pelo topo.
- Verificação de duplicidade, referências e dados inválidos.
- Visualização da lista, fila, pilha e ABB nas mesmas telas HTML/CSS/JS.

As aplicações Java e C/C++ têm estados independentes. Não compartilham banco nem
sincronizam entre si. Para comparar, insira o mesmo cenário nas duas.

## Teste guiado no navegador

1. Carregue o exemplo na página inicial. Isso cria sete hospitais, três bolsas e
   duas requisições. Também permite visualizar uma ABB com ambos os lados.
2. Abra Requisições. Somente o primeiro pedido pode ser concluído ou cancelado.
3. Clique em Concluir e tente confirmar sem escolher a quantidade exigida:
   a aplicação deve recusar sem alterar o estoque.
4. Selecione a quantidade correta de IDs. Confirme.
5. Observe a remoção das bolsas da lista e da requisição da fila.
6. Abra Histórico: o pedido concluído está no topo. Conclua o próximo e confira LIFO.
7. Abra Estruturas: alterne os percursos pré-ordem, em-ordem e pós-ordem.
8. Tente remover um hospital referenciado por pedido ou histórico: deve ser recusado.

A seleção manual não valida se uma bolsa é adequada para um paciente; este é um
exercício de estruturas e não um sistema para decisões clínicas.

## Testes reproduzíveis

```bash
./mvnw test
bash testar_c.sh
```

O teste C inclui falhas de alocação, AddressSanitizer/UBSan e contagem de nós ainda
alocados. LeakSanitizer foi desabilitado porque não funciona com a instrumentação
do ambiente usado; isso é diferente de afirmar que esse detector aprovou a execução.

Para comparar as APIs, inicie as duas aplicações com estado vazio e, em outro
terminal com Python 3, execute:

```bash
python documentacao/testar_equivalencia_http.py
```

O script executa operações nos dois servidores, verifica códigos de resposta e
compara todo o estado após cada passo. Ele modifica os dados de teste e termina com
as estruturas vazias. Não rode sobre uma execução com cadastros que queira manter.

## Onde o professor deve olhar

| Conteúdo | Arquivo/pasta |
|---|---|
| Structs, ponteiros e contratos | `c/rota_vital.h` |
| Lista, fila, pilha, ABB, recursão, malloc/free | `c/rota_vital.c` |
| Menu C++ com iostream e stdio; adaptador HTTP | `cpp/programa.cpp` |
| Reimplementação em Java | `src/main/java/br/edu/rotavital/*` |
| API e aplicação Spring | `ApiRotaVital.java`, `AplicacaoRotaVital.java` |
| Telas compartilhadas | `src/main/resources/static` |
| Comparação com o HemoTrack original e equivalência | `documentacao/Comparacao_e_Equivalencia.pdf` |
| Versão editável do documento | `documentacao/Comparacao_e_Equivalencia.md` |
| Testes e evidências | `src/test`, `c/testes.c`, `documentacao` |

As bibliotecas de HTTP/JSON ficam isoladas em `cpp/terceiros` com suas licenças.
Elas fazem o transporte e a serialização; não implementam as estruturas avaliadas.
O núcleo C compila e executa os testes sem essas bibliotecas, Spring ou Java.

## Diferenças deliberadas em relação ao HemoTrack original

O original usa JPA/H2, Thymeleaf, alocação parcial automática, compatibilidade e FEFO.
A adaptação usa estruturas em memória, a mesma interface estática para ambos os
servidores e conclusão manual integral em FIFO. O visual mantém a referência escura
com rosa e ciano, mas a navegação foi ampliada para expor histórico, hospitais e
estruturas. Não é uma cópia literal de todas as regras da Entrega 02.

O código original de sala continua não fornecido. Confira com o professor se ele
exige especificamente a autoria/continuidade daquela implementação e preencha os
dados acadêmicos do grupo antes de entregar. Nada foi enviado ao professor.
