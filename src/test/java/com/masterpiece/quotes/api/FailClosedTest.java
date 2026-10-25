package com.masterpiece.quotes.api;

import com.masterpiece.quotes.security.SecurityConfigAccess;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Without a proper APP_PASSWORD the API must refuse to start, never start unprotected.
class FailClosedTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short", "elevenchars"})
    void refusesToStartWithoutAStrongPassword(String password) {
        assertThatThrownBy(() -> SecurityConfigAccess.users(password))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_PASSWORD");
    }
}
