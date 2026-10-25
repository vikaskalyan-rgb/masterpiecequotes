package com.masterpiece.quotes.service;

import com.masterpiece.quotes.exception.InvalidInputException;

// Phone numbers are stored as digits with country code ("919994445388"): that is what WhatsApp
// links need, and it makes search work however the number was typed.
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    // Bare 10-digit numbers and 0-prefixed numbers are Indian; anything else must already carry
    // a country code (11-15 digits, per E.164).
    public static String normalize(String raw) {
        String d = digitsOnly(raw);
        if (d.length() == 11 && d.startsWith("0")) {
            d = d.substring(1);
        }
        if (d.length() == 10) {
            return "91" + d;
        }
        if (d.length() >= 11 && d.length() <= 15 && !d.startsWith("0")) {
            return d;
        }
        throw new InvalidInputException("customerPhone",
                "Enter a 10-digit mobile number, or a full number with country code");
    }
}
