package br.bortoti.core;

import java.util.*;

/**
 * Orquestrador de execução de workflows
 * Gerencia múltiplas execuções (runs) de um mesmo workflow
 * e controla o estado de cada tarefa
 */
public class WorkflowRunner {
    private final WorkflowDefinition workflow;
    private final Map<String, WorkflowRun> runs; // runId -> WorkflowRun

    /**
     * Cria um novo executor para um workflow específico
     *
     * @param workflow Definição do workflow a executar
     */
    public WorkflowRunner(WorkflowDefinition workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
        this.runs = new HashMap<>();
    }

    /**
     * Inicia uma nova execução do workflow
     *
     * @param runId ID único para esta execução
     * @return A nova WorkflowRun criada
     * @throws IllegalArgumentException se o runId já existe
     */
    public WorkflowRun startRun(String runId) {
        if (runs.containsKey(runId)) {
            throw new IllegalArgumentException("Já existe um run com ID: " + runId);
        }

        WorkflowRun run = new WorkflowRun(runId, workflow);
        runs.put(runId, run);
        return run;
    }

    /**
     * Obtém uma execução existente
     *
     * @param runId ID da execução
     * @return A WorkflowRun associada
     * @throws IllegalArgumentException se o runId não existe
     */
    public WorkflowRun getRun(String runId) {
        WorkflowRun run = runs.get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Run não encontrado: " + runId);
        }
        return run;
    }

    /**
     * Lista todas as execuções existentes
     */
    public Collection<WorkflowRun> getAllRuns() {
        return Collections.unmodifiableCollection(runs.values());
    }

    /**
     * Notifica que uma tarefa foi concluída com sucesso
     * Retorna a lista de tarefas que agora estão prontas para execução
     *
     * @param runId ID da execução
     * @param taskId ID da tarefa concluída
     * @return Set de IDs de tarefas que agora podem ser iniciadas
     */
    public Set<String> notifyTaskCompleted(String runId, String taskId) {
        WorkflowRun run = getRun(runId);

        // Valida que a tarefa está em execução
        if (run.taskStatus(taskId) != TaskStatus.RUNNING) {
            throw new IllegalStateException(
                String.format("Tarefa '%s' não está em execução (status: %s)",
                    taskId, run.taskStatus(taskId))
            );
        }

        // Marca como concluída
        run.markDone(taskId);

        // Retorna as tarefas que agora podem ser iniciadas
        return run.getReadyTasks();
    }

    /**
     * Notifica que uma tarefa falhou
     *
     * @param runId ID da execução
     * @param taskId ID da tarefa que falhou
     */
    public void notifyTaskFailed(String runId, String taskId) {
        WorkflowRun run = getRun(runId);

        // Valida que a tarefa está em execução
        if (run.taskStatus(taskId) != TaskStatus.RUNNING) {
            throw new IllegalStateException(
                String.format("Tarefa '%s' não está em execução (status: %s)",
                    taskId, run.taskStatus(taskId))
            );
        }

        // Marca como falha
        run.markFailed(taskId);
    }

    /**
     * Obtém as tarefas prontas para execução em um run específico
     *
     * @param runId ID da execução
     * @return Set de IDs de tarefas que podem ser iniciadas
     */
    public Set<String> getReadyTasks(String runId) {
        return getRun(runId).getReadyTasks();
    }

    /**
     * Marca uma tarefa como em execução
     *
     * @param runId ID da execução
     * @param taskId ID da tarefa
     */
    public void startTask(String runId, String taskId) {
        getRun(runId).markRunning(taskId);
    }

    /**
     * Obtém o status atual de uma tarefa
     *
     * @param runId ID da execução
     * @param taskId ID da tarefa
     * @return Status da tarefa
     */
    public TaskStatus getTaskStatus(String runId, String taskId) {
        return getRun(runId).taskStatus(taskId);
    }

    /**
     * Obtém o status de todas as tarefas em um run
     *
     * @param runId ID da execução
     * @return Mapa de taskId -> status
     */
    public Map<String, TaskStatus> getAllTaskStatuses(String runId) {
        return getRun(runId).allTaskStatuses();
    }

    /**
     * Verifica se um run foi completado com sucesso
     *
     * @param runId ID da execução
     * @return true se todas as tarefas estão DONE
     */
    public boolean isRunComplete(String runId) {
        return getRun(runId).allTasksDone();
    }

    /**
     * Verifica se um run teve alguma falha
     *
     * @param runId ID da execução
     * @return true se alguma tarefa está FAILED
     */
    public boolean hasRunFailed(String runId) {
        return getRun(runId).hasFailed();
    }

    /**
     * Retorna o workflow que este runner executa
     */
    public WorkflowDefinition getWorkflow() {
        return workflow;
    }

    /**
     * Retorna informações resumidas de um run
     *
     * @param runId ID da execução
     * @return String com informações do run
     */
    public String getRunSummary(String runId) {
        WorkflowRun run = getRun(runId);
        return String.format(
            "Run: %s | Total: %d | Done: %d | Failed: %d | Running: %d | Waiting: %d | Duration: %dms",
            run.runId(),
            workflow.taskIds().size(),
            run.getTasksWithStatus(TaskStatus.DONE).size(),
            run.getTasksWithStatus(TaskStatus.FAILED).size(),
            run.getTasksWithStatus(TaskStatus.RUNNING).size(),
            run.getTasksWithStatus(TaskStatus.WAITING).size(),
            run.getDurationMs()
        );
    }
}

