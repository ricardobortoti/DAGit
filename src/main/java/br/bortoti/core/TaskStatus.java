package br.bortoti.core;

/**
 * Enum que representa os possíveis estados de uma tarefa em execução
 */
public enum TaskStatus {
    WAITING("Aguardando execução"),
    RUNNING("Em execução"),
    DONE("Concluída com sucesso"),
    FAILED("Falhou");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}

