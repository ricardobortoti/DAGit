package br.bortoti.core;

import java.util.*;

public final class TaskDefinition {
    private final String taskId;
    private final Set<String> dependsOn;
    private final Map<String, String> meta; // opcional, simples

    public TaskDefinition(String taskId, Collection<String> dependsOn, Map<String, String> meta) {
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.dependsOn = Collections.unmodifiableSet(new HashSet<>(dependsOn == null ? List.of() : dependsOn));
        this.meta = Collections.unmodifiableMap(new HashMap<>(meta == null ? Map.of() : meta));
    }

    public static TaskDefinition of(String taskId, Collection<String> dependsOn) {
        return new TaskDefinition(taskId, dependsOn, Map.of());
    }

    public String taskId() { return taskId; }
    public Set<String> dependsOn() { return dependsOn; }
    public Map<String, String> meta() { return meta; }
}