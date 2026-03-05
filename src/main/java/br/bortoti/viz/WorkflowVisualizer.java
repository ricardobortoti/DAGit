package br.bortoti.viz;

import br.bortoti.core.TaskDefinition;
import br.bortoti.core.WorkflowDefinition;

import java.util.*;

public class WorkflowVisualizer {
    private Map<String, Integer> levels;
    private Map<Integer, List<String>> tasksByLevel;

    /**
     * Gera uma representação visual ASCII do workflow
     */
    public String visualize(WorkflowDefinition workflow) {
        levels = calculateLevels(workflow);
        tasksByLevel = groupTasksByLevel(levels);

        StringBuilder sb = new StringBuilder();

        // Cabeçalho
        sb.append("╔════════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║  Workflow: %-49s║\n", workflow.workflowType()));
        sb.append(String.format("║  Total de tarefas: %-40d║\n", workflow.taskIds().size()));
        sb.append("╚════════════════════════════════════════════════════════════╝\n\n");

        int maxLevel = levels.values().stream().max(Integer::compareTo).orElse(0);

        // Visualiza cada nível
        for (int level = 0; level <= maxLevel; level++) {
            List<String> tasksAtLevel = tasksByLevel.getOrDefault(level, List.of());

            sb.append(String.format("┌─ Nível %d ─────────────────────────────────────────────┐\n", level));

            for (String taskId : tasksAtLevel) {
                TaskDefinition task = workflow.task(taskId);
                Set<String> deps = task.dependsOn();

                sb.append(String.format("│  ┌─ [%s]\n", taskId));

                if (!deps.isEmpty()) {
                    sb.append(String.format("│  │  ├─ Dependências: %s\n", formatDependencies(deps)));
                }

                Set<String> dependents = workflow.dependentsOf(taskId);
                if (!dependents.isEmpty()) {
                    sb.append(String.format("│  │  └─ Filhos: %s\n", formatDependencies(dependents)));
                }

                sb.append("│  │\n");
            }

            sb.append("│\n");

            // Desenha conexões para o próximo nível
            if (level < maxLevel) {
                List<String> nextLevelTasks = tasksByLevel.getOrDefault(level + 1, List.of());
                if (!nextLevelTasks.isEmpty()) {
                    sb.append("│     ↓\n");
                    sb.append("│\n");
                }
            }
        }

        sb.append("└───────────────────────────────────────────────────────────┘\n");

        return sb.toString();
    }

    /**
     * Gera uma representação compacta do workflow em uma única linha
     */
    public String visualizeCompact(WorkflowDefinition workflow) {
        levels = calculateLevels(workflow);
        tasksByLevel = groupTasksByLevel(levels);

        StringBuilder sb = new StringBuilder();
        int maxLevel = levels.values().stream().max(Integer::compareTo).orElse(0);

        for (int level = 0; level <= maxLevel; level++) {
            List<String> tasksAtLevel = tasksByLevel.getOrDefault(level, List.of());
            sb.append("[").append(String.join(" | ", tasksAtLevel)).append("]");
            if (level < maxLevel) {
                sb.append(" → ");
            }
        }

        return sb.toString();
    }

    /**
     * Gera uma visualização em grafo ASCII mostrando as conexões entre tarefas
     */
    public String visualizeGraph(WorkflowDefinition workflow) {
        levels = calculateLevels(workflow);
        tasksByLevel = groupTasksByLevel(levels);

        StringBuilder sb = new StringBuilder();

        // Cabeçalho
        sb.append("╔════════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║  Grafo do Workflow: %-41s║\n", workflow.workflowType()));
        sb.append(String.format("║  Total de tarefas: %-40d║\n", workflow.taskIds().size()));
        sb.append("╚════════════════════════════════════════════════════════════╝\n\n");

        int maxLevel = levels.values().stream().max(Integer::compareTo).orElse(0);
        Map<String, Integer> positionInLevel = new HashMap<>();

        // Calcula posição de cada tarefa dentro de seu nível
        for (int level = 0; level <= maxLevel; level++) {
            List<String> tasksAtLevel = tasksByLevel.getOrDefault(level, List.of());
            for (int i = 0; i < tasksAtLevel.size(); i++) {
                positionInLevel.put(tasksAtLevel.get(i), i);
            }
        }

        // Desenha o grafo
        for (int level = 0; level <= maxLevel; level++) {
            List<String> tasksAtLevel = tasksByLevel.getOrDefault(level, List.of());

            // Desenha as tarefas no nível atual
            sb.append("    ");
            for (int i = 0; i < tasksAtLevel.size(); i++) {
                if (i > 0) sb.append("        ");
                sb.append(String.format("┌────────────┐"));
            }
            sb.append("\n");

            sb.append("    ");
            for (int i = 0; i < tasksAtLevel.size(); i++) {
                if (i > 0) sb.append("  ");
                String taskId = tasksAtLevel.get(i);
                String displayName = truncate(taskId, 10);
                sb.append(String.format("│ %-10s │", displayName));
            }
            sb.append("\n");

            sb.append("    ");
            for (int i = 0; i < tasksAtLevel.size(); i++) {
                if (i > 0) sb.append("        ");
                sb.append(String.format("└────────────┘"));
            }
            sb.append("\n");

            // Desenha as conexões para o próximo nível
            if (level < maxLevel) {
                sb.append(drawConnections(workflow, tasksAtLevel, tasksByLevel.getOrDefault(level + 1, List.of()), levels, positionInLevel));
            }
        }

        return sb.toString();
    }

    /**
     * Desenha as linhas de conexão entre níveis
     */
    private String drawConnections(WorkflowDefinition workflow, List<String> currentLevel,
                                   List<String> nextLevel, Map<String, Integer> levels,
                                   Map<String, Integer> positionInLevel) {
        StringBuilder sb = new StringBuilder();

        // Cria mapa de conexões
        Map<String, Set<String>> connections = new HashMap<>();
        for (String task : currentLevel) {
            Set<String> dependents = workflow.dependentsOf(task);
            connections.put(task, dependents);
        }

        // Desenha linhas verticais e diagonais
        sb.append("\n");

        // Linhas verticais iniciais
        sb.append("    ");
        for (int i = 0; i < currentLevel.size(); i++) {
            if (i > 0) sb.append("        ");
            String task = currentLevel.get(i);
            if (!connections.get(task).isEmpty()) {
                sb.append("    │        ");
            } else {
                sb.append("             ");
            }
        }
        sb.append("\n");

        // Conexões diagonais/horizontais
        for (int i = 0; i < currentLevel.size(); i++) {
            String currentTask = currentLevel.get(i);
            Set<String> dependents = connections.get(currentTask);

            if (dependents.isEmpty()) continue;

            sb.append("    ");

            for (int j = 0; j < currentLevel.size(); j++) {
                if (j > 0) sb.append("        ");

                if (j == i) {
                    // Raiz da conexão
                    sb.append("    ├─");
                } else if (j > i && currentLevel.get(j - 1).equals(currentTask)) {
                    sb.append("────┤");
                } else if (j < i) {
                    sb.append("        ");
                } else {
                    sb.append("    │   ");
                }
            }
            sb.append("──┐\n");
        }

        // Linhas verticais para baixo
        sb.append("    ");
        for (int i = 0; i < nextLevel.size(); i++) {
            if (i > 0) sb.append("        ");
            sb.append("    │        ");
        }
        sb.append("\n\n");

        return sb.toString();
    }

    /**
     * Trunca uma string para um tamanho máximo
     */
    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 1) + "…";
    }

    /**
     * Calcula o nível (profundidade) de cada tarefa no DAG
     */
    private Map<String, Integer> calculateLevels(WorkflowDefinition workflow) {
        Map<String, Integer> levels = new HashMap<>();

        // Tarefas raiz estão no nível 0
        for (String root : workflow.roots()) {
            levels.put(root, 0);
        }

        // Calcula o nível de cada tarefa baseado em suas dependências
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String taskId : workflow.taskIds()) {
                if (!levels.containsKey(taskId)) {
                    Set<String> dependencies = workflow.dependenciesOf(taskId);
                    if (dependencies.stream().allMatch(levels::containsKey)) {
                        int maxDepLevel = dependencies.stream()
                            .map(levels::get)
                            .max(Integer::compareTo)
                            .orElse(-1);
                        levels.put(taskId, maxDepLevel + 1);
                        changed = true;
                    }
                }
            }
        }

        return levels;
    }

    /**
     * Agrupa tarefas por nível
     */
    private Map<Integer, List<String>> groupTasksByLevel(Map<String, Integer> levels) {
        Map<Integer, List<String>> result = new TreeMap<>();
        levels.forEach((taskId, level) ->
            result.computeIfAbsent(level, k -> new ArrayList<>()).add(taskId)
        );
        return result;
    }

    /**
     * Formata um conjunto de dependências em uma string legível
     */
    private String formatDependencies(Collection<String> deps) {
        return deps.isEmpty() ? "(nenhuma)" : String.join(", ", deps);
    }

    /**
     * Gera um diagrama em formato Mermaid para visualização
     */
    public String visualizeMermaid(WorkflowDefinition workflow) {
        StringBuilder sb = new StringBuilder();

        // Cabeçalho Mermaid
        sb.append("```mermaid\n");
        sb.append("graph TD\n");

        // Define estilo por nível
        sb.append("    classDef level0 fill:#e1f5ff,stroke:#01579b,stroke-width:2px;\n");
        sb.append("    classDef level1 fill:#f3e5f5,stroke:#4a148c,stroke-width:2px;\n");
        sb.append("    classDef level2 fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px;\n");
        sb.append("    classDef level3 fill:#fff3e0,stroke:#e65100,stroke-width:2px;\n");
        sb.append("    classDef level4 fill:#fce4ec,stroke:#880e4f,stroke-width:2px;\n");

        // Calcula níveis se não estiver já calculado
        if (levels == null) {
            levels = calculateLevels(workflow);
        }

        // Cria todos os nós
        for (String taskId : workflow.taskIds()) {
            int level = levels.getOrDefault(taskId, 0);
            String levelClass = "level" + Math.min(level, 4); // Máx 5 cores diferentes
            sb.append(String.format("    %s[\"%s\"]:::%s\n", sanitizeId(taskId), taskId, levelClass));
        }

        sb.append("\n");

        // Cria todas as arestas (conexões)
        for (String taskId : workflow.taskIds()) {
            Set<String> dependents = workflow.dependentsOf(taskId);
            for (String dependent : dependents) {
                sb.append(String.format("    %s --> %s\n", sanitizeId(taskId), sanitizeId(dependent)));
            }
        }

        sb.append("```\n");

        return sb.toString();
    }

    /**
     * Sanitiza o ID da tarefa para ser válido em Mermaid
     * Remove caracteres especiais e substitui por underscores
     */
    private String sanitizeId(String taskId) {
        return taskId.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}

