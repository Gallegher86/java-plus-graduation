package ru.practicum.exception;

import java.util.List;

public class ConditionsNotMetException extends RuntimeException {

    private final List<String> errors;

    public ConditionsNotMetException(String message) {
        super(message);
        this.errors = List.of();
    }

    public ConditionsNotMetException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}

