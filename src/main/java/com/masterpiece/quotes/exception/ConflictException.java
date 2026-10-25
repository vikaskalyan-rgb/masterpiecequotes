package com.masterpiece.quotes.exception;

// 409: the quote was changed elsewhere since this copy was loaded.
public class ConflictException extends RuntimeException {
    private final Long currentVersion;

    public ConflictException(String message, Long currentVersion) {
        super(message);
        this.currentVersion = currentVersion;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }
}
