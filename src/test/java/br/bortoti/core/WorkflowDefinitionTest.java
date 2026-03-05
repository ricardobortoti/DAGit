package br.bortoti.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowDefinitionTest {
    @Test
    void shouldCreateValidWorkflow() {
        WorkflowDefinition def = new WorkflowDefinition(
                "billing#v1",
                List.of(
                        TaskDefinition.of("A", List.of()),
                        TaskDefinition.of("B", List.of("A")),
                        TaskDefinition.of("C", List.of("A")),
                        TaskDefinition.of("D", List.of("B", "C"))
                )
        );

        assertEquals("billing#v1", def.workflowType());

        assertTrue(def.taskIds().containsAll(Set.of("A", "B", "C", "D")));

        Map<String, Set<String>> map = def.toDependsOnMap();
        assertEquals(Set.of(), map.get("A"));
        assertEquals(Set.of("A"), map.get("B"));
        assertEquals(Set.of("A"), map.get("C"));
        assertEquals(Set.of("B", "C"), map.get("D"));
    }


    @Test
    void shouldFailIfDependencyDoesNotExist() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                new WorkflowDefinition(
                        "invalid",
                        List.of(
                                TaskDefinition.of("A", List.of("X")) // X não existe
                        )
                )
        );

        assertTrue(ex.getMessage().contains("não existe"));
    }

    @Test
    void shouldFailIfCycleExists() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                new WorkflowDefinition(
                        "cyclic",
                        List.of(
                                TaskDefinition.of("A", List.of("B")),
                                TaskDefinition.of("B", List.of("A"))
                        ) //referencia circular
                )
        );

        assertTrue(ex.getMessage().contains("ciclo"));
    }

    @Test
    void shouldAllowIndependentRoots() {
        WorkflowDefinition def = new WorkflowDefinition(
                "multi-root",
                List.of(
                        TaskDefinition.of("A", List.of()),
                        TaskDefinition.of("B", List.of())
                )
        );

        assertEquals(Set.of("A", "B"), def.taskIds());
    }

    @Test
    void rootsShouldReturnTasksWithNoDependencies() {
        WorkflowDefinition def = new WorkflowDefinition(
                "billing#v1",
                List.of(
                        TaskDefinition.of("A", List.of()),
                        TaskDefinition.of("B", List.of("A")),
                        TaskDefinition.of("C", List.of("A")),
                        TaskDefinition.of("D", List.of("B", "C"))
                )
        );

        assertEquals(Set.of("A"), def.roots());
    }

    @Test
    void rootsShouldReturnMultipleRootsWhenTheyExist() {
        WorkflowDefinition def = new WorkflowDefinition(
                "multi-root",
                List.of(
                        TaskDefinition.of("A", List.of()),
                        TaskDefinition.of("B", List.of()),
                        TaskDefinition.of("C", List.of("A"))
                )
        );

        assertEquals(Set.of("A", "B"), def.roots());
    }

    @Test
    void dependenciesOfShouldReturnDeps() {
        WorkflowDefinition def = new WorkflowDefinition(
                "billing#v1",
                List.of(
                        TaskDefinition.of("A", List.of()),
                        TaskDefinition.of("B", List.of("A")),
                        TaskDefinition.of("C", List.of("A")),
                        TaskDefinition.of("D", List.of("B", "C"))
                )
        );

        assertEquals(Set.of(), def.dependenciesOf("A"));
        assertEquals(Set.of("A"), def.dependenciesOf("B"));
        assertEquals(Set.of("B", "C"), def.dependenciesOf("D"));
    }

    @Test
    void dependentsOfShouldReturnChildren() {
        WorkflowDefinition def = new WorkflowDefinition(
                "billing#v1",
                List.of(
                        TaskDefinition.of("A", List.of()),
                        TaskDefinition.of("B", List.of("A")),
                        TaskDefinition.of("C", List.of("A")),
                        TaskDefinition.of("D", List.of("B", "C"))
                )
        );

        assertEquals(Set.of("B", "C"), def.dependentsOf("A"));
        assertEquals(Set.of("D"), def.dependentsOf("B"));
        assertEquals(Set.of("D"), def.dependentsOf("C"));
        assertEquals(Set.of(), def.dependentsOf("D"));
    }

    @Test
    void childrenOfIsAliasOfDependentsOf() {
        WorkflowDefinition def = new WorkflowDefinition(
                "billing#v1",
                List.of(
                        TaskDefinition.of("A", List.of()),
                        TaskDefinition.of("B", List.of("A"))
                )
        );

        assertEquals(def.dependentsOf("A"), def.childrenOf("A"));
    }

    @Test
    void dependentsOfShouldThrowIfTaskDoesNotExist() {
        WorkflowDefinition def = new WorkflowDefinition(
                "billing#v1",
                List.of(
                        TaskDefinition.of("A", List.of())
                )
        );

        assertThrows(IllegalArgumentException.class, () -> def.dependentsOf("X"));
    }

    @Test
    void dependenciesOfShouldThrowIfTaskDoesNotExist() {
        WorkflowDefinition def = new WorkflowDefinition(
                "billing#v1",
                List.of(
                        TaskDefinition.of("A", List.of())
                )
        );

        assertThrows(IllegalArgumentException.class, () -> def.dependenciesOf("X"));
    }
}