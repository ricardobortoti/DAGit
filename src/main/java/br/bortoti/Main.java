package br.bortoti;

import br.bortoti.core.*;
import br.bortoti.viz.WorkflowVisualizer;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        // Exemplo 1: Workflow simples linear
//        IO.println("=== Exemplo 1: Workflow Linear ===\n");
//        WorkflowDefinition simpleWorkflow = createSimpleWorkflow();
//        WorkflowVisualizer visualizer = new WorkflowVisualizer();
//        IO.println(visualizer.visualizeGraph(simpleWorkflow));
//
//        // Exemplo 2: Workflow com múltiplas dependências
//        IO.println("\n=== Exemplo 2: Workflow Complexo ===\n");
//        WorkflowDefinition complexWorkflow = createComplexWorkflow();
//        IO.println(visualizer.visualizeGraph(complexWorkflow));
//
//        // Exemplo 3: Visualização compacta
//        IO.println("\n=== Visualização Compacta ===\n");
//        IO.println("Simples: " + visualizer.visualizeCompact(simpleWorkflow));
//        IO.println("Complexa: " + visualizer.visualizeCompact(complexWorkflow));
//
//        // Exemplo 4: Diagrama Mermaid
//        IO.println("\n=== Diagrama Mermaid - Workflow Linear ===\n");
//        IO.println(visualizer.visualizeMermaid(simpleWorkflow));
//
//        IO.println("\n=== Diagrama Mermaid - Workflow Complexo ===\n");
//        IO.println(visualizer.visualizeMermaid(complexWorkflow));

        // Exemplo 5: WorkflowRunner - Executando o workflow simples
        IO.println("\n\n╔═══════════════════════════════════════════════════════╗");
        IO.println("║         Exemplo: Execução de Workflow (Runner)         ║");
        IO.println("╚═══════════════════════════════════════════════════════╝\n");
        demonstrateSimpleWorkflowExecution();

        // Exemplo 6: WorkflowRunner - Executando o workflow complexo
        IO.println("\n");
        demonstrateComplexWorkflowExecution();
    }

    private static void demonstrateSimpleWorkflowExecution() {
        WorkflowDefinition workflow = createSimpleWorkflow();
        WorkflowRunner runner = new WorkflowRunner(workflow);

        // Inicia um novo run
        WorkflowRun run = runner.startRun("run-001");
        IO.println("✓ Iniciado: " + run + "\n");

        // Simula a execução do workflow
        IO.println("--- Passo 1: Quais tarefas podem ser executadas? ---");
        Set<String> readyTasks = runner.getReadyTasks("run-001");
        IO.println("Tarefas prontas: " + readyTasks + "\n");

        // Executa task-a
        IO.println("--- Passo 2: Executando task-a ---");
        runner.startTask("run-001", "task-a");
        IO.println("Status de task-a: " + runner.getTaskStatus("run-001", "task-a"));
        IO.println();

        // Completa task-a e vê quais tarefas ficam prontas
        IO.println("--- Passo 3: task-a concluída ---");
        Set<String> nextReady = runner.notifyTaskCompleted("run-001", "task-a");
        IO.println("Próximas tarefas prontas: " + nextReady);
        IO.println();

        // Executa as tarefas em sequência
        for (String taskId : List.of("task-b", "task-c", "task-d")) {
            IO.println(String.format("--- Executando %s ---", taskId));
            runner.startTask("run-001", taskId);
            Set<String> nexts = runner.notifyTaskCompleted("run-001", taskId);
            IO.println(String.format("✓ %s concluída. Próximas: %s\n", taskId, nexts));
        }

        // Resumo final
        IO.println("--- Resumo Final ---");
        IO.println(runner.getRunSummary("run-001"));
        IO.println("Run completo? " + runner.isRunComplete("run-001"));
    }

    private static void demonstrateComplexWorkflowExecution() {
        WorkflowDefinition workflow = createComplexWorkflow();
        WorkflowRunner runner = new WorkflowRunner(workflow);

        // Inicia um novo run
        WorkflowRun run = runner.startRun("run-complex-001");
        IO.println("✓ Iniciado: " + run + "\n");

        // Tarefas iniciais que podem ser executadas em paralelo
        IO.println("--- Passo 1: Tarefas iniciais (podem rodar em paralelo) ---");
        Set<String> ready = runner.getReadyTasks("run-complex-001");
        IO.println("Tarefas prontas: " + ready);
        IO.println();

        // Executa as tarefas raiz
        for (String taskId : ready) {
            runner.startTask("run-complex-001", taskId);
            runner.notifyTaskCompleted("run-complex-001", taskId);
            IO.println(String.format("✓ %s concluída", taskId));
        }
        IO.println();

        // Próxima rodada de tarefas
        IO.println("--- Passo 2: Próximas tarefas disponíveis ---");
        ready = runner.getReadyTasks("run-complex-001");
        IO.println("Tarefas prontas: " + ready);
        IO.println();

        // Executa a próxima rodada
        for (String taskId : ready) {
            runner.startTask("run-complex-001", taskId);
            runner.notifyTaskCompleted("run-complex-001", taskId);
            IO.println(String.format("✓ %s concluída", taskId));
        }
        IO.println();

        // Continua até o final
        IO.println("--- Passo 3: Continuando até o final ---");
        while (!runner.isRunComplete("run-complex-001")) {
            Set<String> nextReady = runner.getReadyTasks("run-complex-001");
            if (nextReady.isEmpty()) break;

            for (String taskId : nextReady) {
                runner.startTask("run-complex-001", taskId);
                runner.notifyTaskCompleted("run-complex-001", taskId);
                IO.println(String.format("✓ %s concluída", taskId));
            }
        }
        IO.println();

        // Resumo final
        IO.println("--- Resumo Final ---");
        IO.println(runner.getRunSummary("run-complex-001"));
        IO.println("Run completo? " + runner.isRunComplete("run-complex-001"));
    }

    private static WorkflowDefinition createSimpleWorkflow() {
        List<TaskDefinition> tasks = List.of(
            TaskDefinition.of("task-a", List.of()),
            TaskDefinition.of("task-b", List.of("task-a")),
            TaskDefinition.of("task-c", List.of("task-b")),
            TaskDefinition.of("task-d", List.of("task-c"))
        );
        return new WorkflowDefinition("billing#v1", tasks);
    }

    private static WorkflowDefinition createComplexWorkflow() {
        List<TaskDefinition> tasks = List.of(
            // Nível 0 - Raízes
            TaskDefinition.of("fetch-data", List.of()),
            TaskDefinition.of("validate-input", List.of()),

            // Nível 1
            TaskDefinition.of("process-a", List.of("fetch-data")),
            TaskDefinition.of("process-b", List.of("fetch-data")),
            TaskDefinition.of("check-rules", List.of("validate-input")),

            // Nível 2
            TaskDefinition.of("merge-results", List.of("process-a", "process-b")),
            TaskDefinition.of("apply-rules", List.of("check-rules", "merge-results")),

            // Nível 3
            TaskDefinition.of("save-output", List.of("apply-rules")),
            TaskDefinition.of("notify-user", List.of("apply-rules"))
        );
        return new WorkflowDefinition("payment-processing#v2", tasks);
    }
}
