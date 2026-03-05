package br.bortoti.core;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Representa uma execução específica de um workflow
 * Rastreia o status de cada tarefa e mantém histórico de eventos
 */
public class WorkflowRun {
    private final String runId;
    private final WorkflowDefinition workflow;
    private final LocalDateTime startTime;
    private final Map<String, TaskStatus> taskStatuses;
    private final Map<String, LocalDateTime> taskCompletionTimes;
    private LocalDateTime endTime;

    /**
     * Cria uma nova execução de workflow
     *
     * @param runId ID único para esta execução
     * @param workflow Definição do workflow a executar
     */
    public WorkflowRun(String runId, WorkflowDefinition workflow) {
        this.runId = Objects.requireNonNull(runId, "runId");
        this.workflow = Objects.requireNonNull(workflow, "workflow");
        this.startTime = LocalDateTime.now();
        this.taskStatuses = new HashMap<>();
        this.taskCompletionTimes = new HashMap<>();

        // Inicializa todas as tarefas como WAITING
        for (String taskId : workflow.taskIds()) {
            taskStatuses.put(taskId, TaskStatus.WAITING);
        }
    }

    // ========== GETTERS ==========

    public String runId() {
        return runId;
    }

    public WorkflowDefinition workflow() {
        return workflow;
    }

    public LocalDateTime startTime() {
        return startTime;
    }

    public LocalDateTime endTime() {
        return endTime;
    }

    public TaskStatus taskStatus(String taskId) {
        validateTaskExists(taskId);
        return taskStatuses.get(taskId);
    }

    public Map<String, TaskStatus> allTaskStatuses() {
        return Collections.unmodifiableMap(new HashMap<>(taskStatuses));
    }

    // ========== MUTADORES DE STATUS ==========

    /**
     * Marca uma tarefa como em execução
     *
     * @param taskId ID da tarefa
     * @throws IllegalArgumentException se a tarefa não existe ou não está em WAITING
     */
    public void markRunning(String taskId) {
        validateTaskExists(taskId);
        TaskStatus current = taskStatuses.get(taskId);
        if (current != TaskStatus.WAITING) {
            throw new IllegalArgumentException(
                String.format("Tarefa '%s' está em estado %s, não pode ser marcada como RUNNING", taskId, current)
            );
        }
        taskStatuses.put(taskId, TaskStatus.RUNNING);
    }

    /**
     * Marca uma tarefa como concluída com sucesso
     *
     * @param taskId ID da tarefa
     * @throws IllegalArgumentException se a tarefa não existe ou não está em RUNNING
     */
    public void markDone(String taskId) {
        validateTaskExists(taskId);
        TaskStatus current = taskStatuses.get(taskId);
        if (current != TaskStatus.RUNNING) {
            throw new IllegalArgumentException(
                String.format("Tarefa '%s' está em estado %s, não pode ser marcada como DONE", taskId, current)
            );
        }
        taskStatuses.put(taskId, TaskStatus.DONE);
        taskCompletionTimes.put(taskId, LocalDateTime.now());

        // Se todas as tarefas terminaram, marca o run como finalizado
        if (allTasksDone()) {
            this.endTime = LocalDateTime.now();
        }
    }

    /**
     * Marca uma tarefa como falha
     *
     * @param taskId ID da tarefa
     * @throws IllegalArgumentException se a tarefa não existe ou não está em RUNNING
     */
    public void markFailed(String taskId) {
        validateTaskExists(taskId);
        TaskStatus current = taskStatuses.get(taskId);
        if (current != TaskStatus.RUNNING) {
            throw new IllegalArgumentException(
                String.format("Tarefa '%s' está em estado %s, não pode ser marcada como FAILED", taskId, current)
            );
        }
        taskStatuses.put(taskId, TaskStatus.FAILED);
        taskCompletionTimes.put(taskId, LocalDateTime.now());
    }

    // ========== CONSULTAS DE STATUS ==========

    /**
     * Retorna todas as tarefas que podem ser executadas agora
     * (todas as suas dependências estão DONE)
     *
     * @return Set de IDs de tarefas que podem ser iniciadas
     */
    public Set<String> getReadyTasks() {
        Set<String> readyTasks = new HashSet<>();

        for (String taskId : workflow.taskIds()) {
            TaskStatus status = taskStatuses.get(taskId);

            // Só tarefas em WAITING podem ser marcadas como prontas
            if (status != TaskStatus.WAITING) {
                continue;
            }

            // Verifica se todas as dependências estão DONE
            Set<String> dependencies = workflow.dependenciesOf(taskId);
            boolean allDepsDone = dependencies.stream()
                .allMatch(dep -> taskStatuses.get(dep) == TaskStatus.DONE);

            if (allDepsDone) {
                readyTasks.add(taskId);
            }
        }

        return readyTasks;
    }

    /**
     * Verifica se todas as tarefas foram concluídas com sucesso
     */
    public boolean allTasksDone() {
        return workflow.taskIds().stream()
            .allMatch(taskId -> taskStatuses.get(taskId) == TaskStatus.DONE);
    }

    /**
     * Verifica se alguma tarefa falhou
     */
    public boolean hasFailed() {
        return workflow.taskIds().stream()
            .anyMatch(taskId -> taskStatuses.get(taskId) == TaskStatus.FAILED);
    }

    /**
     * Retorna todos os IDs de tarefas com um status específico
     */
    public Set<String> getTasksWithStatus(TaskStatus status) {
        Set<String> result = new HashSet<>();
        for (var entry : taskStatuses.entrySet()) {
            if (entry.getValue() == status) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Retorna o tempo de execução de uma tarefa (se concluída)
     */
    public LocalDateTime completionTime(String taskId) {
        validateTaskExists(taskId);
        return taskCompletionTimes.get(taskId);
    }

    /**
     * Calcula a duração total do run em milissegundos
     */
    public long getDurationMs() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.temporal.ChronoUnit.MILLIS.between(startTime, end);
    }

    // ========== HELPER METHODS ==========

    private void validateTaskExists(String taskId) {
        if (!workflow.taskIds().contains(taskId)) {
            throw new IllegalArgumentException("Tarefa não existe: " + taskId);
        }
    }

    @Override
    public String toString() {
        return String.format("WorkflowRun{id=%s, workflow=%s, tasks=%d, done=%d, failed=%d}",
            runId,
            workflow.workflowType(),
            workflow.taskIds().size(),
            getTasksWithStatus(TaskStatus.DONE).size(),
            getTasksWithStatus(TaskStatus.FAILED).size()
        );
    }
}

