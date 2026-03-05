# DAGit 🚀

Um sistema robusto de **gerenciamento de workflows baseado em DAG (Directed Acyclic Graph)** implementado em Java.

## 📋 Visão Geral

DAGit permite definir, validar e **executar** workflows compostos por tarefas interdependentes. O sistema garante que:
- ✅ Não há tarefas duplicadas
- ✅ Todas as dependências existem
- ✅ Não há ciclos (garantindo execução possível)
- ✅ Relacionamentos são mantidos em índices para acesso rápido
- ✅ **Execução orquestrada com paralelismo automático**
- ✅ **Múltiplas visualizações** (ASCII, Mermaid, compacta)

## 🏗️ Arquitetura

### Componentes Principais

#### `TaskDefinition`
Representa uma unidade de trabalho individual no workflow.

**Propriedades:**
- `taskId`: Identificador único da tarefa
- `dependsOn`: Conjunto de IDs de tarefas que esta tarefa depende
- `meta`: Metadados opcionais (mapa de chave-valor)

```java
// Criando uma tarefa simples
TaskDefinition task = TaskDefinition.of("processPayment", List.of("validateOrder"));

// Criando uma tarefa com metadados
TaskDefinition taskWithMeta = new TaskDefinition(
    "generateInvoice",
    List.of("processPayment"),
    Map.of("priority", "high", "timeout", "300s")
);
```

#### `WorkflowDefinition`
Define o workflow completo contendo múltiplas tarefas e gerenciando suas dependências.

**Funcionalidades:**
- `workflowType()`: Retorna o tipo/identificador do workflow
- `taskIds()`: Obtém todos os IDs de tarefas
- `task(taskId)`: Recupera uma tarefa específica
- `roots()`: Retorna tarefas sem dependências (pontos de início)
- `dependenciesOf(taskId)`: Obtém dependências de uma tarefa
- `dependentsOf(taskId)`: Obtém tarefas que dependem dela
- `toDependsOnMap()`: Exporta mapa completo de dependências

**Validações Automáticas:**
- Detecta tarefas duplicadas
- Valida que todas as dependências existem
- Detecta ciclos usando algoritmo de Kahn (topological sort)

```java
// Exemplo de workflow válido
WorkflowDefinition workflow = new WorkflowDefinition(
    "billing#v1",
    List.of(
        TaskDefinition.of("A", List.of()),           // Tarefa raiz
        TaskDefinition.of("B", List.of("A")),        // Depende de A
        TaskDefinition.of("C", List.of("A")),        // Depende de A
        TaskDefinition.of("D", List.of("B", "C"))    // Depende de B e C
    )
);

// Consultando o workflow
System.out.println(workflow.roots());           // [A]
System.out.println(workflow.dependentsOf("A")); // [B, C]
System.out.println(workflow.dependenciesOf("D")); // [B, C]
```

#### `TaskStatus` (Enum)
Define os 4 estados possíveis de uma tarefa em execução:

```java
enum TaskStatus {
    WAITING,  // Aguardando execução
    RUNNING,  // Em execução
    DONE,     // Concluída com sucesso
    FAILED    // Falhou
}
```

#### `WorkflowRun`
Representa **uma única execução** de um workflow. Rastreia o estado de cada tarefa durante a execução.

**Funcionalidades:**
- Rastreamento de status de cada tarefa
- Registro de tempos de conclusão
- **`getReadyTasks()`**: Calcula tarefas que podem ser executadas agora
- Métodos para transição de estados: `markRunning()`, `markDone()`, `markFailed()`

#### `WorkflowRunner`
Orquestrador central que gerencia **múltiplas execuções** do mesmo workflow.

**Funcionalidades:**
- **`startRun(runId)`**: Inicia nova execução
- **`startTask(runId, taskId)`**: Marca tarefa como em execução
- **`notifyTaskCompleted(runId, taskId)`**: Notifica conclusão e retorna próximas tarefas
- **`getReadyTasks(runId)`**: Obtém tarefas prontas para executar
- **`isRunComplete(runId)`**: Verifica se execução terminou
- **`hasRunFailed(runId)`**: Verifica se houve falhas

**Fluxo de Execução:**
```java
WorkflowRunner runner = new WorkflowRunner(workflow);

// 1. Iniciar execução
WorkflowRun run = runner.startRun("run-001");

// 2. Obter tarefas iniciais
Set<String> ready = runner.getReadyTasks("run-001");

// 3. Executar tarefas em loop
while (!runner.isRunComplete("run-001")) {
    for (String taskId : ready) {
        runner.startTask("run-001", taskId);
        // ... executar tarefa ...
        ready = runner.notifyTaskCompleted("run-001", taskId);
    }
}
```

#### `WorkflowVisualizer`
Sistema de visualização com **4 formatos diferentes**:

1. **`visualize()`**: Detalhado com informações de dependências
2. **`visualizeGraph()`**: Grafo ASCII com conexões visuais
3. **`visualizeCompact()`**: Formato compacto em uma linha
4. **`visualizeMermaid()`**: Diagrama Mermaid (GitHub/GitLab compatível)

```java
WorkflowVisualizer visualizer = new WorkflowVisualizer();

// Visualização em grafo ASCII
System.out.println(visualizer.visualizeGraph(workflow));

// Diagrama Mermaid (copie para GitHub)
System.out.println(visualizer.visualizeMermaid(workflow));
```

## 🧪 Testes

O projeto inclui uma suite abrangente de testes unitários (`WorkflowDefinitionTest`) que cobrem:

✅ **Criação de workflows válidos**
- Workflows com múltiplas tarefas
- Workflows com múltiplas raízes

✅ **Validação de dependências**
- Rejeição de dependências não existentes
- Mensagens de erro descritivas

✅ **Detecção de ciclos**
- Rejeição de referências circulares
- Garantia de DAG válido

✅ **Consultas de relacionamentos**
- Raízes corretas
- Dependentes corretos
- Mapeamento de dependências

✅ **Execução de workflows**
- Transições de estado válidas
- Detecção de tarefas prontas
- Paralelismo automático

## 🔧 Stack Tecnológico

- **Linguagem**: Java
- **Build Tool**: Gradle
- **Framework de Testes**: JUnit 5
- **Versão**: 1.0-SNAPSHOT

## 📦 Dependências

```gradle
dependencies {
    testImplementation platform('org.junit:junit-bom:5.10.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

## 🚀 Como Usar

### Compilar o projeto
```bash
./gradlew build
```

### Executar os testes
```bash
./gradlew test
```

### Executar demonstrações
```bash
./gradlew run
```

### Estrutura do Projeto
```
DAGit/
├── src/
│   ├── main/java/br/bortoti/
│   │   ├── Main.java                    # Ponto de entrada com demos
│   │   └── core/
│   │       ├── TaskDefinition.java      # Definição de tarefa
│   │       ├── WorkflowDefinition.java  # Definição de workflow
│   │       ├── TaskStatus.java          # Estados de execução
│   │       ├── WorkflowRun.java         # Execução individual
│   │       └── WorkflowRunner.java      # Orquestrador
│   └── test/java/br/bortoti/core/
│       └── WorkflowDefinitionTest.java  # Testes unitários
├── build.gradle                         # Configuração Gradle
├── README.md                            # Este arquivo
└── RUNNER_ARCHITECTURE.md               # Documentação detalhada
```

## 💡 Casos de Uso

- **Orquestração de Pipelines**: Executar etapas de processamento de dados em ordem
- **Workflows de Billing**: Validar pedidos → Processar pagamento → Gerar fatura
- **CI/CD Pipelines**: Build → Test → Deploy com dependências complexas
- **Workflows de Processamento**: Qualquer sistema que necessite executar tarefas em uma ordem específica
- **Sistemas Distribuídos**: Coordenação de tarefas em clusters
- **ETL Pipelines**: Extract → Transform → Load com paralelismo

## 🎯 Funcionalidades Implementadas

### ✅ Definição e Validação
- [x] Definição de tarefas com dependências
- [x] Validação de DAG (sem ciclos)
- [x] Índices de dependências para acesso rápido

### ✅ Execução Orquestrada
- [x] Sistema de estados (WAITING → RUNNING → DONE/FAILED)
- [x] Detecção automática de paralelismo
- [x] Múltiplas execuções independentes (1 workflow → N runs)
- [x] Rastreamento de tempo e status

### ✅ Visualizações
- [x] Visualização detalhada em ASCII
- [x] Grafo visual com conexões
- [x] Formato compacto
- [x] Diagrama Mermaid (GitHub/GitLab compatível)

### ✅ Testes e Qualidade
- [x] Suite completa de testes unitários
- [x] Validações robustas
- [x] Exemplos funcionais

## 🔮 Próximos Passos (Futuro)

- [ ] Persistência (salvar/carregar workflows)
- [ ] Serialização (JSON/XML)
- [ ] Interface web/API REST
- [ ] Retry automático de tarefas falhadas
- [ ] Timeout e cancelamento
- [ ] Métricas e monitoramento
- [ ] Interface gráfica
- [ ] Integração com sistemas externos

## 👤 Autor

Ricardo Bortoti

## 📄 Licença

MIT
