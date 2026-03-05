package br.bortoti.core;

import java.util.*;

public final class WorkflowDefinition {
    private final String workflowType;
    private final Map<String, TaskDefinition> tasks; // taskId -> def
    private final Map<String, Set<String>> dependentsIndex;

    public WorkflowDefinition(String workflowType, Collection<TaskDefinition> taskDefinitions) {
        this.workflowType = Objects.requireNonNull(workflowType, "workflowType");
        Objects.requireNonNull(taskDefinitions, "taskDefinitions");

        Map<String, TaskDefinition> map = new HashMap<>();
        for (TaskDefinition t : taskDefinitions) {
            if (map.putIfAbsent(t.taskId(), t) != null) {
                throw new IllegalArgumentException("Task duplicada: " + t.taskId());
            }
        }
        this.tasks = Collections.unmodifiableMap(map);

        validateAllDepsExist();
        this.dependentsIndex = Collections.unmodifiableMap(buildDependentsIndex());
        validateAcyclic();
    }

    public String workflowType() { return workflowType; }

    public Set<String> taskIds() { return tasks.keySet(); }

    public TaskDefinition task(String taskId) {
        TaskDefinition t = tasks.get(taskId);
        if (t == null) throw new IllegalArgumentException("Task não existe: " + taskId);
        return t;
    }

    public Map<String, Set<String>> toDependsOnMap() {
        Map<String, Set<String>> m = new HashMap<>();
        for (var e : tasks.entrySet()) {
            m.put(e.getKey(), new HashSet<>(e.getValue().dependsOn()));
        }
        return m;
    }

    public Set<String> roots() {
        Set<String> roots = new HashSet<>();
        for (TaskDefinition t : tasks.values()) {
            if (t.dependsOn().isEmpty()) roots.add(t.taskId());
        }
        return Collections.unmodifiableSet(roots);
    }

    public Set<String> dependenciesOf(String taskId) {
        return task(taskId).dependsOn();
    }

    public Set<String> dependentsOf(String taskId) {
        if (!tasks.containsKey(taskId)) {
            throw new IllegalArgumentException("Task não existe: " + taskId);
        }
        return dependentsIndex.getOrDefault(taskId, Set.of());
    }

    public Set<String> childrenOf(String taskId) {
        return dependentsOf(taskId);
    }

    //indice invertido para facilitar consulta de dependentes (filhos)
    private Map<String, Set<String>> buildDependentsIndex() {
        Map<String, Set<String>> idx = new HashMap<>();

        for (String t : tasks.keySet()) {
            idx.put(t, new HashSet<>());
        }

        for (TaskDefinition t : tasks.values()) {
            for (String dep : t.dependsOn()) {
                idx.get(dep).add(t.taskId());
            }
        }

        Map<String, Set<String>> result = new HashMap<>();
        for (var e : idx.entrySet()) {
            result.put(e.getKey(), Collections.unmodifiableSet(e.getValue()));
        }

        return result;
    }

    private void validateAllDepsExist() {
        for (TaskDefinition t : tasks.values()) {
            for (String dep : t.dependsOn()) {
                if (!tasks.containsKey(dep)) {
                    throw new IllegalArgumentException(
                            "Dependência '" + dep + "' referenciada por '" + t.taskId() + "' não existe no workflow."
                    );
                }
            }
        }
    }

    // usa topological sort para validar que não há ciclos(referencia circular entre os nós). Complexidade O(V+E)
    private void validateAcyclic() {
        // Kahn topological sort
        Map<String, Integer> indeg = new HashMap<>();
        Map<String, Set<String>> adj = new HashMap<>();

        for (String id : tasks.keySet()) {
            indeg.put(id, 0);
            adj.put(id, new HashSet<>());
        }

        for (TaskDefinition t : tasks.values()) {
            for (String dep : t.dependsOn()) {
                adj.get(dep).add(t.taskId());
                indeg.put(t.taskId(), indeg.get(t.taskId()) + 1);
            }
        }

        ArrayDeque<String> q = new ArrayDeque<>();
        for (var e : indeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());

        int visited = 0;
        while (!q.isEmpty()) {
            String n = q.remove();
            visited++;
            for (String nxt : adj.get(n)) {
                indeg.put(nxt, indeg.get(nxt) - 1);
                if (indeg.get(nxt) == 0) q.add(nxt);
            }
        }

        if (visited != tasks.size()) {
            throw new IllegalArgumentException("Workflow inválido: há ciclo nas dependências.");
        }
    }
}