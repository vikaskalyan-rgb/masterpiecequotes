package com.masterpiece.quotes.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class SecurityConfigAccess {

    private SecurityConfigAccess() {
    }

    public static Object users(String password) {
        return new SecurityConfig().users("owner", password, new BCryptPasswordEncoder());
    }
}
