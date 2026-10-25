package com.masterpiece.quotes.dto;

public record RestoreResultDto(int quotesRestored, int quotesSkipped, int snapshotsRestored, boolean settingsRestored) {
}
