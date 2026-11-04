# 🥟 Sistema de Gestão de Produção de Salgados

> Aplicação web local para gerenciamento de ingredientes, receitas, custos, estoque e produção de salgados.

**Status:** 🚧 Em desenvolvimento  
**Versão do projeto:** 1.0 — Escopo inicial  
**Plataforma inicial:** Linux (Fedora)  
**Usuário-alvo:** Pequeno produtor individual

---

## 📌 Sobre o projeto

O **Sistema de Gestão de Produção de Salgados** é uma aplicação desenvolvida para centralizar e organizar os processos operacionais de uma pequena empresa de produção e fornecimento de salgados.

O objetivo principal é oferecer uma ferramenta que permita gerenciar ingredientes, cadastrar receitas, calcular custos de produção, controlar o estoque e armazenar o histórico das operações realizadas.

A aplicação foi planejada para funcionar localmente em um computador pessoal, utilizando uma interface web acessível pelo navegador. O processamento das regras de negócio e o gerenciamento dos dados serão realizados pelo backend em Java, enquanto o armazenamento será feito em um banco de dados SQLite.

O projeto busca substituir controles manuais e planilhas dispersas por um sistema integrado, organizado e capaz de preservar o histórico financeiro e operacional da produção.

## 🎯 Objetivos

- Centralizar o cadastro e gerenciamento de ingredientes.
- Controlar entradas, saídas e disponibilidade do estoque.
- Cadastrar produtos e suas respectivas receitas.
- Calcular automaticamente as quantidades de ingredientes necessárias para diferentes volumes de produção.
- Estimar o peso e o custo de fabricação dos produtos.
- Auxiliar na formação do preço de venda.
- Registrar compras e custos de aquisição.
- Armazenar o histórico detalhado das produções.
- Preservar os valores e informações utilizados em operações anteriores.
- Disponibilizar relatórios para acompanhamento dos custos e da operação.

---

## 🛠️ Tecnologias utilizadas

O projeto utiliza uma arquitetura web local, dividida entre frontend, backend e persistência de dados.

| Tecnologia | Finalidade |
|---|---|
| Java | Linguagem principal e regras de negócio |
| Spring Boot | Framework para construção do backend |
| HTML5 | Estrutura da interface |
| CSS3 | Estilização e organização visual |
| JavaScript | Interatividade e comunicação com a API |
| SQLite | Banco de dados relacional local |
| Spring JDBC / JdbcTemplate | Acesso e persistência de dados |
| Maven | Gerenciamento de dependências e build |
| JUnit 5 | Testes automatizados |
| Git | Controle de versão |
| GitHub | Hospedagem do código-fonte |

### Arquitetura

A aplicação utiliza uma arquitetura monolítica modular, com comunicação entre frontend e backend por meio de uma API REST.

**Frontend — HTML, CSS e JavaScript**

Responsável pela interface gráfica, formulários, tabelas, visualização de informações e interação com o usuário.

**Backend — Java e Spring Boot**

Responsável pelo processamento das requisições, validação dos dados, regras de negócio, cálculos, gerenciamento de estoque e execução das operações.

**Banco de dados — SQLite**

Responsável pelo armazenamento persistente dos ingredientes, produtos, receitas, compras, produções e movimentações de estoque.

O próprio Spring Boot disponibilizará os arquivos do frontend e a API, mantendo a aplicação sob uma única origem local.

```text
                 USUÁRIO
                    |
                    v
             NAVEGADOR WEB
                    |
                    v
          HTML + CSS + JavaScript
                    |
                    v
                API REST
                    |
                    v
            JAVA + SPRING BOOT
                    |
          +---------+---------+
          |         |         |
          v         v         v
       Services  Validation  Controllers
          |
          v
       Repositories
          |
          v
       SQLite Database
          |
          v
        empresa.db
```

---

## ⚙️ Funcionalidades

O sistema será desenvolvido em módulos independentes, organizados de acordo com suas responsabilidades.

### 1. Painel inicial

Apresentar uma visão geral da operação.

- Resumo do estoque.
- Alertas de ingredientes abaixo do estoque mínimo.
- Produções recentes.
- Indicadores de custos.
- Atalhos para as principais funcionalidades.

### 2. Gerenciamento de ingredientes

Permitir o cadastro e gerenciamento dos insumos utilizados na fabricação dos produtos.

Informações previstas:

- Nome do ingrediente.
- Categoria.
- Unidade de medida.
- Quantidade mínima em estoque.
- Custo médio de aquisição.
- Disponibilidade atual.
- Estado ativo ou inativo.

### 3. Gerenciamento de produtos e receitas

Permitir cadastrar os produtos fabricados e suas respectivas composições.

Cada receita deverá possuir:

- Produto relacionado.
- Identificação e versão da receita.
- Rendimento esperado.
- Lista de ingredientes.
- Quantidade necessária de cada ingrediente.
- Unidade de medida.
- Peso unitário final, quando aplicável.

As receitas serão versionadas para preservar as configurações utilizadas em produções anteriores.

### 4. Compras e aquisição de ingredientes

Registrar as compras realizadas e atualizar automaticamente o estoque.

Cada compra deverá armazenar:

- Data da aquisição.
- Ingrediente adquirido.
- Quantidade comprada.
- Unidade de compra.
- Valor total pago.
- Observações.

O sistema calculará o custo médio ponderado de aquisição de cada ingrediente.

### 5. Calculadora de produção

Módulo responsável por calcular os ingredientes e custos necessários para produzir determinada quantidade de um produto.

Funcionalidades previstas:

- Seleção do produto e da receita.
- Definição da quantidade desejada.
- Cálculo proporcional dos ingredientes.
- Cálculo do peso estimado da produção.
- Consulta da disponibilidade em estoque.
- Estimativa do custo total.
- Cálculo do custo por unidade.
- Simulação de diferentes quantidades.
- Estimativa de preço de venda.

A simulação não deverá modificar o estoque.

### 6. Controle de estoque

Gerenciar as movimentações dos ingredientes.

Operações previstas:

- Entrada por compra.
- Saída por produção.
- Registro de perdas e desperdícios.
- Ajustes manuais.
- Consulta de saldo atual.
- Histórico de movimentações.
- Identificação de estoque insuficiente.

Todas as movimentações confirmadas deverão possuir registro histórico.

### 7. Histórico de produção

Armazenar e disponibilizar os registros das produções realizadas.

Cada produção deverá preservar:

- Produto fabricado.
- Versão da receita utilizada.
- Quantidade solicitada.
- Quantidade efetivamente produzida.
- Ingredientes utilizados.
- Quantidades consumidas.
- Custos individuais.
- Custo total.
- Custo por unidade.
- Data da produção.

Os valores históricos não deverão ser alterados quando os preços dos ingredientes ou as receitas forem modificados posteriormente.

### 8. Relatórios

Disponibilizar informações para acompanhamento da operação.

Relatórios planejados:

- Custos de produção por período.
- Consumo de ingredientes.
- Histórico de compras.
- Evolução dos custos de aquisição.
- Produtos mais fabricados.
- Despesas com ingredientes.
- Comparação entre custos de diferentes produções.

---

## 🏗️ Estrutura planejada do backend

A organização do backend seguirá a separação de responsabilidades entre as camadas da aplicação.

```text
br.com.salgados/
│
├── config/
│   ├── WebConfig.java
│   ├── DatabaseConfig.java
│   └── AppProperties.java
│
├── controller/
│   ├── DashboardController.java
│   ├── IngredientController.java
│   ├── ProductController.java
│   ├── RecipeController.java
│   ├── PurchaseController.java
│   ├── ProductionController.java
│   ├── StockController.java
│   ├── ReportController.java
│   └── BackupController.java
│
├── dto/
│   ├── request/
│   └── response/
│
├── domain/
│   ├── model/
│   ├── enums/
│   └── service/
│
├── repository/
│
├── exception/
│
├── validation/
│
├── util/
│
└── SistemaSalgadosApplication.java
```

### Responsabilidade das camadas

| Camada | Responsabilidade |
|---|---|
| Controller | Receber requisições HTTP e disponibilizar os endpoints |
| Service | Implementar regras de negócio e casos de uso |
| Repository | Executar consultas e operações no banco de dados |
| Domain | Representar os conceitos e modelos do negócio |
| DTO | Definir os dados de entrada e saída da API |
| Config | Configurações da aplicação |
| Validation | Validar dados e regras de entrada |
| Exception | Padronizar o tratamento de erros |
| Util | Funções auxiliares compartilhadas |

### Serviços principais

- `IngredientService`: gerenciamento de ingredientes e consulta de custos.
- `PurchaseService`: registro de compras e atualização do custo médio.
- `RecipeService`: gerenciamento de receitas e versões.
- `ProductionCalculatorService`: cálculos e simulações de produção.
- `ProductionService`: confirmação de produção e atualização transacional do estoque.
- `StockService`: gerenciamento de movimentações e ajustes.
- `ReportService`: geração de relatórios.
- `BackupService`: criação, validação e restauração de backups.

---

## 🗄️ Modelo conceitual do banco de dados

O banco de dados será relacional, utilizando SQLite.

Principais entidades previstas:

| Entidade | Responsabilidade |
|---|---|
| `Ingredient` | Cadastro de ingredientes |
| `Product` | Cadastro de produtos fabricados |
| `Recipe` | Receitas e versões |
| `RecipeItem` | Ingredientes pertencentes a uma receita |
| `Purchase` | Registro de compras |
| `PurchaseItem` | Itens de uma compra |
| `Production` | Registro de uma produção |
| `ProductionItem` | Ingredientes e custos associados à produção |
| `StockMovement` | Histórico de movimentações de estoque |
| `AppSetting` | Configurações gerais do sistema |

### Relacionamentos principais

- Um produto pode possuir várias versões de receita.
- Uma receita possui vários ingredientes.
- Um ingrediente pode fazer parte de diversas receitas.
- Uma compra pode conter vários itens.
- Uma produção utiliza uma versão específica de receita.
- Uma produção possui vários registros de consumo.
- Um ingrediente possui diversas movimentações de estoque.

---

## 📐 Regras de negócio importantes

As regras abaixo deverão ser respeitadas durante toda a implementação.

**Precisão financeira**

Os cálculos monetários deverão utilizar `BigDecimal` no Java, evitando problemas de precisão de ponto flutuante.

**Conversão de unidades**

O sistema utilizará unidades-base:

- Massa: grama (g).
- Volume: mililitro (ml).
- Contagem: unidade (un).

Conversões previstas:

- 1 kg = 1.000 g.
- 1 L = 1.000 ml.

Não será permitida conversão automática entre massa e volume sem informação específica de densidade.

**Custo médio ponderado**

O custo médio dos ingredientes será atualizado a cada compra.

```text
Novo custo médio =

(saldo anterior × custo médio anterior
 + quantidade comprada × custo unitário da compra)
 /
(saldo anterior + quantidade comprada)
```

**Escalonamento de receitas**

```text
Fator de escala = quantidade desejada / rendimento original

Quantidade necessária =
quantidade cadastrada na receita × fator de escala
```

**Precificação**

Para uma margem-alvo percentual sobre o preço de venda:

```text
Custo unitário = custo total / quantidade produzida

Preço sugerido = custo unitário / (1 - margem)
```

A margem sobre o preço de venda não deve ser confundida com markup.

**Integridade do estoque**

- Simulações não alteram o estoque.
- Apenas produções confirmadas registram consumo.
- O estoque negativo será bloqueado por padrão.
- Compras, produções e ajustes deverão utilizar transações.
- Correções de movimentações deverão gerar registros compensatórios, sem apagar o histórico.

**Preservação histórica**

As produções confirmadas deverão armazenar snapshots dos ingredientes, quantidades, preços e custos utilizados na ocasião.

O histórico não deverá depender dos valores atuais dos cadastros.

---

## 🔌 API REST — Endpoints planejados

O backend disponibilizará endpoints para comunicação com o frontend.

| Método | Endpoint | Finalidade |
|---|---|---|
| GET | `/api/dashboard/summary` | Indicadores do painel |
| GET | `/api/ingredients` | Listar ingredientes |
| POST | `/api/ingredients` | Cadastrar ingrediente |
| PUT | `/api/ingredients/{id}` | Atualizar ingrediente |
| GET | `/api/products` | Listar produtos |
| POST | `/api/products` | Cadastrar produto |
| POST | `/api/products/{id}/recipes` | Criar versão de receita |
| POST | `/api/purchases` | Registrar compra |
| POST | `/api/production-simulations` | Simular produção |
| POST | `/api/productions` | Confirmar produção |
| GET | `/api/productions` | Consultar histórico |
| GET | `/api/productions/{id}` | Consultar detalhes |
| GET | `/api/stock/balance` | Consultar estoque |
| GET | `/api/stock/movements` | Consultar movimentações |
| POST | `/api/stock/adjustments` | Registrar ajuste |
| GET | `/api/reports/production-costs` | Consultar custos |
| POST | `/api/backups` | Criar backup |

*Os endpoints representam o contrato inicial proposto e poderão ser refinados durante a implementação.*

---

## 🖥️ Interface do usuário

A interface será desenvolvida com HTML, CSS e JavaScript, priorizando a utilização em computadores.

A navegação principal será organizada em uma barra lateral.

Seções previstas:

1. Painel.
2. Ingredientes.
3. Produtos e receitas.
4. Compras.
5. Produção.
6. Estoque.
7. Histórico.
8. Relatórios.
9. Configurações.

### Diretrizes visuais

- Interface limpa e objetiva.
- Navegação simples.
- Tabelas com busca e filtros.
- Formulários com unidades de medida explícitas.
- Indicadores visuais de estoque.
- Confirmação antes de operações que alterem o estoque.
- Mensagens claras de sucesso, erro e validação.

**Idioma:** Português (Brasil).  
**Moeda:** Real brasileiro (R$).  
**Formato de data:** DD/MM/AAAA.

---

## 🗺️ Roadmap de desenvolvimento

O desenvolvimento será dividido em marcos, permitindo validar cada parte antes de avançar.

| Marco | Etapa | Resultado esperado |
|---|---|---|
| M0 | Fundação | Projeto Spring Boot, SQLite e frontend inicial |
| M1 | Cadastros | Ingredientes, produtos e categorias |
| M2 | Receitas | Receitas, ingredientes e versões |
| M3 | Compras e estoque | Compras, custo médio e movimentações |
| M4 | Calculadora | Simulação de quantidades, peso e custos |
| M5 | Produção e histórico | Confirmação transacional e histórico detalhado |
| M6 | Relatórios e backup | Relatórios, backup e restauração |
| M7 | Finalização | Testes completos e empacotamento da aplicação |

### Prioridade de implementação

**P0 — Essencial para o MVP**

- Cadastros.
- Receitas.
- Compras.
- Controle de estoque.
- Calculadora de produção.
- Confirmação de produção.
- Histórico.
- Backup e restauração.

**P1 — Evolução após o fluxo principal**

- Relatórios aprimorados.
- Precificação configurável.
- Pesquisa e filtros avançados.
- Indicadores adicionais.

### Funcionalidades futuras

Ficam fora do escopo inicial:

- Gerenciamento de clientes.
- Pedidos e vendas.
- Controle de entregas.
- Estoque de produtos acabados.
- Controle avançado de lotes e validade.
- Integrações externas.
- Acesso remoto e sincronização em nuvem.
- Múltiplos usuários.

---

## 🧪 Qualidade e testes

O projeto deverá possuir testes unitários e de integração.

A cobertura inicial deverá priorizar:

- Cálculo proporcional de ingredientes.
- Conversões de unidades.
- Cálculo de custo médio ponderado.
- Cálculo de custo unitário.
- Precificação por margem.
- Validação de estoque insuficiente.
- Registro de compras.
- Confirmação de produção.
- Consistência das movimentações.
- Preservação dos valores históricos.
- Backup e restauração.

**Critério de qualidade:** nenhuma operação confirmada poderá deixar divergências entre o saldo de estoque e o histórico de movimentações.

---

## 🔐 Segurança, armazenamento e execução

O sistema será executado localmente, sem depender de conexão com a internet para seu funcionamento.

Endereço local previsto:

```text
http://127.0.0.1:8080
```

O backend deverá aceitar conexões somente pelo endereço de loopback no MVP.

Não haverá autenticação interna inicialmente, considerando que a aplicação será utilizada por uma única pessoa no próprio computador.

O banco de dados será armazenado fora do diretório do código-fonte.

```text
SistemaSalgados/
│
├── app/
│   └── sistema-salgados.jar
│
├── data/
│   ├── empresa.db
│   └── backups/
│
├── logs/
│
└── iniciar-sistema.sh
```

O arquivo `empresa.db` e os backups não deverão ser enviados ao GitHub.

A aplicação deverá possuir mecanismos de backup consistente, validação e restauração do banco de dados.

---

## 🚀 Execução do projeto

### Requisitos previstos

- JDK compatível com a versão configurada no projeto (Java 21 LTS ou superior como referência inicial).
- Maven.
- Navegador web.
- Sistema operacional Linux ou Windows.

### Inicialização em desenvolvimento

Após a configuração do backend com Spring Boot e Maven Wrapper:

```bash
git clone <URL_DO_REPOSITORIO>

cd SistemaSalgados

cd backend

./mvnw spring-boot:run
```

A aplicação deverá ficar disponível em:

```text
http://127.0.0.1:8080
```

No Fedora/Linux, pode ser necessário conceder permissão de execução ao Maven Wrapper:

```bash
chmod +x mvnw
```

> Os comandos representam o fluxo de execução previsto. A inicialização definitiva dependerá da configuração do projeto e do diretório de dados do SQLite.

---

## 📁 Organização geral do repositório

```text
SistemaSalgados/
│
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── br/com/salgados/
│   │       │       ├── config/
│   │       │       ├── controller/
│   │       │       ├── dto/
│   │       │       ├── domain/
│   │       │       ├── repository/
│   │       │       ├── validation/
│   │       │       ├── exception/
│   │       │       └── util/
│   │       │
│   │       └── resources/
│   │           ├── static/
│   │           │   ├── css/
│   │           │   ├── js/
│   │           │   ├── pages/
│   │           │   └── index.html
│   │           │
│   │           └── application.properties
│   │
│   ├── pom.xml
│   └── mvnw
│
├── docs/
│   └── especificacao-projeto.pdf
│
├── .gitignore
│
└── README.md
```

Os diretórios `data/` e `logs/` serão definidos no ambiente de execução e não deverão armazenar dados reais versionados no repositório.

---

## 📚 Documentação

O projeto possui uma especificação funcional e técnica que serve como documento de referência para o desenvolvimento.

A documentação contempla:

- Arquitetura da aplicação.
- Requisitos funcionais e não funcionais.
- Modelo do backend.
- Modelo conceitual do banco de dados.
- Regras de negócio.
- Contrato inicial da API REST.
- Referência visual do frontend.
- Fluxos operacionais.
- Roadmap de desenvolvimento.
- Critérios de aceite.
- Estratégia de backup e segurança.

Consulte o documento de especificação disponível no diretório `docs/`.

---

## 📌 Estado atual do projeto

O projeto encontra-se em fase inicial de desenvolvimento e planejamento técnico.

A arquitetura, os módulos, os requisitos e as regras de negócio foram definidos como referência para a construção do MVP.

As funcionalidades serão implementadas progressivamente, seguindo os marcos estabelecidos no roadmap.

---

## 👨‍💻 Desenvolvimento

Projeto desenvolvido individualmente, com foco na aplicação prática de conhecimentos de:

- Programação Orientada a Objetos.
- Java e Spring Boot.
- Desenvolvimento web.
- APIs REST.
- Modelagem de bancos de dados relacionais.
- SQL e persistência de dados.
- Engenharia de software.
- Testes automatizados.
- Controle de versão com Git.

---

## 📄 Licença

Licença ainda não definida.

