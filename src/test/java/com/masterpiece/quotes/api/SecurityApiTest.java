package com.masterpiece.quotes.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityApiTest extends ApiTestBase {

    private static String basic(String password) {
        return "Basic " + Base64.getEncoder().encodeToString(("owner:" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void healthChecksArePublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/health/ready")).andExpect(status().isOk());
    }

    @Test
    void everythingElseNeedsThePassword() throws Exception {
        mvc.perform(get("/api/quotes")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/api/settings/defaults")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/settings/defaults").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/backup")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/check").header(HttpHeaders.AUTHORIZATION, basic("wrong-password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/check").header(HttpHeaders.AUTHORIZATION, AUTH)).andExpect(status().isNoContent());
    }

    @Test
    void corsPreflightFromTheAppIsAllowedAndOthersAreNot() throws Exception {
        mvc.perform(options("/api/quotes")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/quotes")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void repeatedWrongPasswordsLockTheCallerOut() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(get("/api/auth/check").header(HttpHeaders.AUTHORIZATION, basic("guess-" + i))
                            .with(r -> { r.setRemoteAddr("203.0.113.9"); return r; }))
                    .andExpect(status().isUnauthorized());
        }
        // Even the right password is refused during the lockout, for that caller only.
        mvc.perform(get("/api/auth/check").header(HttpHeaders.AUTHORIZATION, AUTH)
                        .with(r -> { r.setRemoteAddr("203.0.113.9"); return r; }))
                .andExpect(status().isTooManyRequests());
        mvc.perform(get("/api/auth/check").header(HttpHeaders.AUTHORIZATION, AUTH)
                        .with(r -> { r.setRemoteAddr("198.51.100.1"); return r; }))
                .andExpect(status().isNoContent());
    }

    @Test
    void clientErrorsAreAcceptedFromTheApp() throws Exception {
        mvc.perform(post("/api/client-errors").header(HttpHeaders.AUTHORIZATION, AUTH).contentType("application/json")
                        .content("{\"message\":\"TypeError: x is undefined\",\"url\":\"/quotes/1\"}"))
                .andExpect(status().isAccepted());
    }
}
