package com.masterpiece.quotes.api;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

// Runs the real application against a real Postgres (TEST_DB_URL), migrated by Flyway.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestBase {

    static final String PASSWORD = "test-password-123";
    static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString(("owner:" + PASSWORD).getBytes(StandardCharsets.UTF_8));

    @Autowired
    MockMvc mvc;
    @Autowired
    JsonMapper json;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE quote_snapshots, items, rooms, material_spec_items, term_items, quotes RESTART IDENTITY CASCADE");
        jdbc.execute("SELECT setval('quote_number_seq', 1, false)");
        jdbc.update("UPDATE business_profile SET rounding_step = 1000, rounding_mode = 'NEAREST', validity_days = 30");
    }

    ResultActions call(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, AUTH));
    }

    ResultActions postJson(String url, String body) throws Exception {
        return call(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    ResultActions putJson(String url, String body) throws Exception {
        return call(put(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    static String quoteJson(String name, String phone, String extra) {
        return """
            {
              "customerName": "%s",
              "customerPhone": "%s",
              "customerAddress": "12 Lake View Road",
              "quoteDate": "2026-10-20",
              "accessoriesDescription": "Tandem box",
              "accessoriesAmount": 2500,
              "rooms": [
                {"name": "Kitchen", "items": [
                  {"description": "Base cabinet", "unit": "SQFT", "length": 7, "width": 4, "quantity": 999, "rate": 1500},
                  {"description": "Cutlery basket", "unit": "NOS", "quantity": 2, "rate": 850}
                ]}
              ],
              "materialSpecItems": [{"itemLabel": "Plywood", "detail": "BWP 710", "brand": "GAMA"}],
              "termItems": [{"text": "50%% advance"}]
              %s
            }
            """.formatted(name, phone, extra == null ? "" : "," + extra);
    }
}
