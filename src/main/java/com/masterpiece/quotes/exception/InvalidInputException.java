package com.masterpiece.quotes.exception;

import java.util.Map;

// 400 for business-rule validation that annotations can't express (e.g. phone number format).
public class InvalidInputException extends RuntimeException {
    private final Map<String, String> fields;

    public InvalidInputException(String field, String message) {
        super(message);
        this.fields = Map.of(field, message);
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
