package com.masterpiece.quotes.api;

import com.masterpiece.quotes.security.SecurityConfigAccess;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Without an APP_PASSWORD (4+ characters) the API must refuse to start, never start unprotected.
class FailClosedTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1", "123"})
    void refusesToStartWithoutAPassword(String password) {
        assertThatThrownBy(() -> SecurityConfigAccess.users(password))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_PASSWORD");
    }

    @org.junit.jupiter.api.Test
    void acceptsTheOwnersFourDigitCode() {
        org.assertj.core.api.Assertions.assertThatCode(() -> SecurityConfigAccess.users("4821")).doesNotThrowAnyException();
    }
}
