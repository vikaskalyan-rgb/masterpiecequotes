package com.masterpiece.quotes.service;

import com.masterpiece.quotes.exception.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumbersTest {

    @ParameterizedTest
    @CsvSource({
            "9994445388,          919994445388",
            "'+91 99944 45388',   919994445388",
            "09994445388,         919994445388",
            "'91-99944-45388',    919994445388",
            "'+971 50 123 4567',  971501234567",
    })
    void normalizesToDigitsWithCountryCode(String raw, String expected) {
        assertThat(PhoneNumbers.normalize(raw)).isEqualTo(expected);
    }

    @Test
    void rejectsNumbersThatCantBeDialled() {
        assertThatThrownBy(() -> PhoneNumbers.normalize("12345")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize("abc")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize("1234567890123456")).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void parsesQuoteNumbersInEveryFormatPeopleType() {
        assertThat(QuoteService.parseQuoteNumber("42")).isEqualTo(42);
        assertThat(QuoteService.parseQuoteNumber("MP-0042")).isEqualTo(42);
        assertThat(QuoteService.parseQuoteNumber("mp42")).isEqualTo(42);
        assertThat(QuoteService.parseQuoteNumber("#42")).isEqualTo(42);
        assertThat(QuoteService.parseQuoteNumber("Ravi")).isNull();
        assertThat(QuoteService.parseQuoteNumber("9994445388")).isNull();
    }

    @Test
    void escapesLikeWildcards() {
        assertThat(QuoteService.escapeLike("50%_off\\")).isEqualTo("50\\%\\_off\\\\");
    }
}
