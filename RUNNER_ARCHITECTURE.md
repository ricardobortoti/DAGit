# WorkflowRunner - Arquitetura e Funcionamento

## Visão Geral

O sistema `WorkflowRunner` foi implementado para orquestrar e rastrear a execução de workflows. Ele segue a estrutura **1 Workflow → N Runs**, permitindo múltiplas execuções independentes do mesmo workflow.

---

## Componentes

### 1. `TaskStatus` (Enum)

Define os 4 estados possíveis de uma tarefa:

```
WAITING  → RUNNING → DONE
              ↓
           FAILED
```

- **WAITING**: Aguardando execução (dependências não foram todas concluídas)
- **RUNNING**: Em execução
- **DONE**: Concluída com sucesso
- **FAILED**: Falhou durante a execução

---

### 2. `WorkflowRun` (Gerencia uma execução específica)

Representa **uma única execução** de um workflow. Rastreia:

```
WorkflowRun {
  runId: String                           // ID único desta execução
  workflow: WorkflowDefinition            // O workflow que está sendo executado
  taskStatuses: Map<taskId → TaskStatus>  // Estado de cada tarefa
  taskCompletionTimes: Map<...>           // Quando cada tarefa foi concluída
  startTime: LocalDateTime                // Quando o run começou
  endTime: LocalDateTime                  // Quando o run terminou (null se em progresso)
}
```

**Métodos principais:**

- `markRunning(taskId)` - Marca tarefa como em execução
- `markDone(taskId)` - Marca tarefa como concluída
- `markFailed(taskId)` - Marca tarefa como falha
- **`getReadyTasks()`** - ⭐ Retorna tarefas que podem ser executadas agora

---

### 3. `WorkflowRunner` (Orquestrador)

Gerencia **múltiplos runs** do mesmo workflow:

```
WorkflowRunner {
  workflow: WorkflowDefinition       // O workflow template
  runs: Map<runId → WorkflowRun>     // Todas as execuções deste workflow
}
```

**Fluxo de uso:**

```
1. runner.startRun("run-001")           // Cria novo run
                ↓
2. runner.getReadyTasks("run-001")      // Quais tarefas podem rodar?
                ↓
3. runner.startTask("run-001", "task-a") // Marca como em execução
                ↓
4. runner.notifyTaskCompleted(...)      // Tarefa terminou
                ↓
5. Volta ao passo 2 até completion
```

---

## Exemplo Prático: Workflow Linear

### Estrutura
```
task-a → task-b → task-c → task-d
```

### Execução Passo a Passo

#### Passo 1: Inicializar
```java
WorkflowRunner runner = new WorkflowRunner(workflow);
WorkflowRun run = runner.startRun("run-001");
```

Estados iniciais:
```
task-a: WAITING
task-b: WAITING (depende de task-a)
task-c: WAITING (depende de task-b)
task-d: WAITING (depende de task-c)
```

#### Passo 2: Quais tarefas podem rodar?
```java
Set<String> ready = runner.getReadyTasks("run-001");
// Resultado: [task-a]
```

Por quê task-a? Porque:
- `task-a` está em WAITING ✓
- `task-a` não tem dependências ✓
- Logo, pode ser executada!

#### Passo 3: Executar task-a
```java
runner.startTask("run-001", "task-a");
// Estado de task-a muda para RUNNING
```

#### Passo 4: Task-a concluída
```java
Set<String> nextReady = runner.notifyTaskCompleted("run-001", "task-a");
// Resultado: [task-b]
```

O que aconteceu:
- `task-a` mudou para DONE
- `task-b` estava WAITING e suas dependências (task-a) estão DONE
- Logo, `task-b` agora está pronta!

---

## Exemplo Prático: Workflow Complexo (com paralelismo)

### Estrutura
```
    fetch-data ──→ process-a ──┐
                                 ├─→ merge-results
    validate-input ──→ check-rules ──→ apply-rules ──→ save-output
                                      ↑                ↓
                                      └─ notify-user
```

### Execução

#### Inicialização
```
Todos os task começam em WAITING
```

#### Primeiro getReadyTasks()
```java
Set<String> ready = runner.getReadyTasks("run-complex-001");
// Resultado: [fetch-data, validate-input]
```

**Ambas podem rodar em paralelo!** Porque:
- São tarefas raiz (sem dependências)
- Nenhuma precisa esperar a outra

#### Simulando execução paralela
```
runner.startTask("run-complex-001", "fetch-data");
runner.startTask("run-complex-001", "validate-input");
// Ambas agora estão RUNNING simultaneamente

runner.notifyTaskCompleted("run-complex-001", "fetch-data");
runner.notifyTaskCompleted("run-complex-001", "validate-input");
```

#### Próximas tarefas prontas
```java
Set<String> ready = runner.getReadyTasks("run-complex-001");
// Resultado: [process-a, process-b, check-rules]
```

**Novamente 3 tarefas em paralelo!**
- `process-a` e `process-b` dependem de `fetch-data` (✓ DONE)
- `check-rules` depende de `validate-input` (✓ DONE)

#### Continuando...
```
process-a, process-b, check-rules → RUNNING (em paralelo)
     ↓ todas completam
merge-results → prontas (depende de process-a e process-b ✓)
     ↓ completa
apply-rules → prontas (depende de check-rules ✓ e merge-results ✓)
     ↓ completa
save-output, notify-user → prontas
     ↓ ambas completam
WORKFLOW COMPLETO! ✓
```

---

## Algoritmo: getReadyTasks()

```pseudo
readyTasks = []

Para cada tarefa T:
  1. Se T está em estado WAITING:
       2. Obter todas as dependências de T
       3. Se TODAS as dependências estão em DONE:
            4. Adicionar T a readyTasks

retornar readyTasks
```

**Complexidade:** O(n + m) onde:
- n = número de tarefas
- m = número de dependências

---

## Estados e Transições Válidas

```
WAITING ──startTask──→ RUNNING
           (↓ erro se não WAITING)

RUNNING ──markDone──→ DONE
RUNNING ──markFailed──→ FAILED
           (↓ erro se não RUNNING)
```

---

## Casos de Uso

### 1. Monitorar Execução
```java
TaskStatus status = runner.getTaskStatus("run-001", "task-a");
if (status == TaskStatus.RUNNING) {
    System.out.println("Task ainda está rodando...");
}
```

### 2. Detectar Conclusão
```java
if (runner.isRunComplete("run-001")) {
    System.out.println("Workflow concluído com sucesso!");
}
```

### 3. Detectar Falhas
```java
if (runner.hasRunFailed("run-001")) {
    System.out.println("Workflow falhou!");
}
```

### 4. Obter Resumo
```java
String summary = runner.getRunSummary("run-001");
// Output: "Run: run-001 | Total: 9 | Done: 9 | Failed: 0 | ..."
```

### 5. Múltiplos Runs
```java
WorkflowRunner runner = new WorkflowRunner(workflow);

// Primeiro run
runner.startRun("run-001");
// ... executar ...

// Segundo run (mesmo workflow, independente)
runner.startRun("run-002");
// ... executar ...

// Ambos mantidos em runner.getAllRuns()
```

---

## Validações e Segurança

### 1. Tarefas duplicadas
```java
// ❌ Erro: não pode ter 2 tarefas com mesmo ID
new WorkflowDefinition("wf", [
    TaskDefinition.of("task-a", ...),
    TaskDefinition.of("task-a", ...)  // Duplicado!
]);
```

### 2. Dependência inexistente
```java
// ❌ Erro: task-b depende de task-x que não existe
new WorkflowDefinition("wf", [
    TaskDefinition.of("task-b", ["task-x"])  // task-x não existe!
]);
```

### 3. Ciclos
```java
// ❌ Erro: task-a depende de task-b que depende de task-a (ciclo!)
new WorkflowDefinition("wf", [
    TaskDefinition.of("task-a", ["task-b"]),
    TaskDefinition.of("task-b", ["task-a"])
]);
```

### 4. Transições inválidas
```java
// ❌ Erro: task não está RUNNING, não pode completar
runner.notifyTaskCompleted("run-001", "task-a");
// Antes de: runner.startTask("run-001", "task-a");
```

---

## Performance

| Operação | Complexidade | Observação |
|----------|--------------|-----------|
| `startRun()` | O(n) | Inicializa todos os task |
| `getReadyTasks()` | O(n + m) | Itera tarefas e deps |
| `startTask()` | O(1) | Apenas muda status |
| `markDone()` | O(1) | Apenas muda status |
| `notifyTaskCompleted()` | O(n + m) | Chama getReadyTasks() |

---

## Thread Safety

⚠️ **Nota Importante:** A implementação atual **não é thread-safe**. Para ambientes multi-thread:
- Adicionar sincronização com `synchronized`
- Ou usar `ConcurrentHashMap` e locks apropriados
- Ou usar estruturas de dados imutáveis

---

## Próximas Melhorias Possíveis

1. **Callbacks/Listeners** - Notificar quando tarefa completa
2. **Timeout** - Falhar automaticamente se tarefa excede tempo
3. **Retry** - Reexecutar tarefas que falharam
4. **Cancelamento** - Cancelar um run em progresso
5. **Persistência** - Salvar/carregar estado de runs
6. **Métricas** - Tempo de execução, taxa de sucesso, etc.

