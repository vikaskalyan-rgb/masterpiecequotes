package com.masterpiece.quotes.api;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuoteApiTest extends ApiTestBase {

    @Test
    void serverComputesEveryNumberAndIgnoresClientArithmetic() throws Exception {
        JsonNode q = read(postJson("/api/quotes", quoteJson("Ravi", "99944 45388", null))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/quotes/"))));

        JsonNode base = q.at("/rooms/0/items/0");
        assertThat(base.get("quantity").decimalValue()).isEqualByComparingTo("28");      // 7 x 4, not 999
        assertThat(base.get("amount").decimalValue()).isEqualByComparingTo("42000");
        assertThat(q.at("/rooms/0/total").decimalValue()).isEqualByComparingTo("43700");
        assertThat(q.get("subtotal").decimalValue()).isEqualByComparingTo("46200");      // + accessories
        assertThat(q.get("roundedTotal").decimalValue()).isEqualByComparingTo("46000"); // nearest 1,000
        assertThat(q.get("totalOverridden").asBoolean()).isFalse();
        assertThat(q.get("customerPhone").asString()).isEqualTo("919994445388");
        assertThat(q.get("quoteNumber").asInt()).isEqualTo(1);
        assertThat(q.get("validUntil").asString()).isEqualTo("2026-11-19");              // + 30 days
        assertThat(q.get("status").asString()).isEqualTo("DRAFT");
    }

    @Test
    void autoTotalFollowsTheItemsOnEveryEdit() throws Exception {
        JsonNode q = read(postJson("/api/quotes", quoteJson("Ravi", "9994445388", null)));
        String edited = quoteJson("Ravi", "9994445388", "\"version\": " + q.get("version").asLong())
                .replace("\"rate\": 1500", "\"rate\": 2000");
        JsonNode after = read(putJson("/api/quotes/" + q.get("id").asLong(), edited).andExpect(status().isOk()));
        assertThat(after.get("subtotal").decimalValue()).isEqualByComparingTo("60200");
        assertThat(after.get("roundedTotal").decimalValue()).isEqualByComparingTo("60000");
    }

    @Test
    void sqftItemWithOnlyATypedAreaKeepsItsAreaOnSave() throws Exception {
        String body = quoteJson("Ravi", "9994445388", null).replace(
                "{\"description\": \"Cutlery basket\", \"unit\": \"NOS\", \"quantity\": 2, \"rate\": 850}",
                "{\"description\": \"Utility box (2X2)(1X7)\", \"unit\": \"SQFT\", \"quantity\": 11, \"rate\": 1000}");
        JsonNode q = read(postJson("/api/quotes", body).andExpect(status().isCreated()));
        JsonNode item = q.at("/rooms/0/items/1");
        assertThat(item.get("description").asString()).isEqualTo("Utility box (2X2)(1X7)");
        assertThat(item.get("quantity").decimalValue()).isEqualByComparingTo("11");
        assertThat(item.get("amount").decimalValue()).isEqualByComparingTo("11000");
        assertThat(item.get("length").isNull()).isTrue();
    }

    @Test
    void adjustedBilledAreaSurvivesASave() throws Exception {
        String body = quoteJson("Ravi", "9994445388", null).replace(
                "{\"description\": \"Base cabinet\", \"unit\": \"SQFT\", \"length\": 7, \"width\": 4, \"quantity\": 999, \"rate\": 1500}",
                "{\"description\": \"Foyer Box\", \"unit\": \"SQFT\", \"length\": 2.5, \"width\": 5.5, \"quantity\": 14, \"rate\": 1250, \"areaAdjusted\": true}");
        JsonNode item = read(postJson("/api/quotes", body).andExpect(status().isCreated())).at("/rooms/0/items/0");
        assertThat(item.get("length").decimalValue()).isEqualByComparingTo("2.5");
        assertThat(item.get("quantity").decimalValue()).isEqualByComparingTo("14");
        assertThat(item.get("amount").decimalValue()).isEqualByComparingTo("17500");
    }

    @Test
    void typedTotalIsKeptExactlyWhenOverridden() throws Exception {
        JsonNode q = read(postJson("/api/quotes",
                quoteJson("Ravi", "9994445388", "\"totalOverridden\": true, \"roundedTotal\": 44000")));
        assertThat(q.get("roundedTotal").decimalValue()).isEqualByComparingTo("44000");
        assertThat(q.get("suggestedTotal").decimalValue()).isEqualByComparingTo("46000");
    }

    @Test
    void staleEditFromAnotherDeviceIsRejected() throws Exception {
        JsonNode q = read(postJson("/api/quotes", quoteJson("Ravi", "9994445388", null)));
        long id = q.get("id").asLong();
        long v = q.get("version").asLong();
        putJson("/api/quotes/" + id, quoteJson("Ravi K", "9994445388", "\"version\": " + v)).andExpect(status().isOk());
        putJson("/api/quotes/" + id, quoteJson("Ravi Old", "9994445388", "\"version\": " + v))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentVersion").value(v + 1));
    }

    @Test
    void retriedCreateWithSameRequestIdNeverDuplicates() throws Exception {
        String body = quoteJson("Ravi", "9994445388", "\"clientRequestId\": \"abc-123\"");
        long first = read(postJson("/api/quotes", body)).get("id").asLong();
        long second = read(postJson("/api/quotes", body)).get("id").asLong();
        assertThat(second).isEqualTo(first);
        call(get("/api/quotes")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rejectsNegativeAndOversizedValuesWith400() throws Exception {
        postJson("/api/quotes", quoteJson("Ravi", "9994445388", null).replace("\"rate\": 850", "\"rate\": -5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields", aMapWithSize(1)));
        postJson("/api/quotes", quoteJson("x".repeat(300), "9994445388", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.customerName").exists());
        postJson("/api/quotes", quoteJson("Ravi", "12345", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.customerPhone").exists());
    }

    @Test
    void malformedRequestsAre400NotServerErrors() throws Exception {
        postJson("/api/quotes", "{not json").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request was malformed"));
        call(get("/api/quotes").param("status", "BOGUS")).andExpect(status().isBadRequest());
        call(get("/api/quotes/abc")).andExpect(status().isBadRequest());
        call(get("/api/quotes/9999")).andExpect(status().isNotFound());
    }

    @Test
    void sharingRecordsWhatWasSentAndDetectsLaterEdits() throws Exception {
        JsonNode q = read(postJson("/api/quotes", quoteJson("Ravi", "9994445388", null)));
        long id = q.get("id").asLong();

        JsonNode share = read(call(post("/api/quotes/" + id + "/share")).andExpect(status().isOk()));
        assertThat(share.get("newVersion").asBoolean()).isTrue();
        assertThat(share.at("/quote/status").asString()).isEqualTo("SENT");
        assertThat(share.at("/quote/changedSinceShared").asBoolean()).isFalse();

        // Same content again: no new version.
        assertThat(read(call(post("/api/quotes/" + id + "/share"))).get("newVersion").asBoolean()).isFalse();

        long version = share.at("/quote/version").asLong();
        String edited = quoteJson("Ravi", "9994445388", "\"version\": " + version).replace("\"rate\": 1500", "\"rate\": 1800");
        JsonNode after = read(putJson("/api/quotes/" + id, edited));
        assertThat(after.get("changedSinceShared").asBoolean()).isTrue();
        assertThat(after.get("status").asString()).isEqualTo("SENT");

        call(post("/api/quotes/" + id + "/share"));
        JsonNode versions = read(call(get("/api/quotes/" + id + "/snapshots")));
        assertThat(versions).hasSize(2);

        // The first shared version still holds the original price.
        long oldest = versions.get(1).get("id").asLong();
        JsonNode payload = read(call(get("/api/quotes/" + id + "/snapshots/" + oldest)));
        assertThat(payload.at("/quote/rooms/0/items/0/rate").decimalValue()).isEqualByComparingTo("1500");
        assertThat(payload.at("/business/name").asString()).isEqualTo("Masterpiece Interiors");
    }

    @Test
    void deleteIsSoftAndRestorable() throws Exception {
        long id = read(postJson("/api/quotes", quoteJson("Ravi", "9994445388", null))).get("id").asLong();
        call(delete("/api/quotes/" + id)).andExpect(status().isNoContent());
        call(get("/api/quotes")).andExpect(jsonPath("$", hasSize(0)));
        call(get("/api/quotes/" + id)).andExpect(status().isNotFound());
        call(get("/api/quotes").param("deleted", "true")).andExpect(jsonPath("$[0].id").value(id));
        call(post("/api/quotes/" + id + "/restore")).andExpect(status().isOk());
        call(get("/api/quotes")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void searchMatchesNamePhoneDigitsAndQuoteNumber() throws Exception {
        postJson("/api/quotes", quoteJson("Ravi Kumar", "+91 99944 45388", null));
        postJson("/api/quotes", quoteJson("Priya", "9876543210", null));
        call(get("/api/quotes").param("search", "ravi")).andExpect(jsonPath("$", hasSize(1)));
        call(get("/api/quotes").param("search", "99944 45388")).andExpect(jsonPath("$[0].customerName").value("Ravi Kumar"));
        call(get("/api/quotes").param("search", "MP-0002")).andExpect(jsonPath("$[0].customerName").value("Priya"));
        call(get("/api/quotes").param("search", "%")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void suggestionsAreCleanAndCarryTheUnit() throws Exception {
        postJson("/api/quotes", quoteJson("Ravi", "9994445388", null));
        call(get("/api/items/suggestions").param("q", "base"))
                .andExpect(jsonPath("$[0].description").value("Base cabinet"))
                .andExpect(jsonPath("$[0].unit").value("SQFT"));
        call(get("/api/items/suggestions").param("q", "_")).andExpect(jsonPath("$", hasSize(0)));
        call(get("/api/items/suggestions")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        call(get("/api/customers/suggestions").param("q", "4445"))
                .andExpect(jsonPath("$[0].name").value("Ravi"))
                .andExpect(jsonPath("$[0].address").value("12 Lake View Road"));
    }

    @Test
    void analyticsCountOnlySentQuotesAndGroupCustomersByPhone() throws Exception {
        long a = read(postJson("/api/quotes", quoteJson("Ravi", "9994445388", null))).get("id").asLong();
        long b = read(postJson("/api/quotes", quoteJson("Ravi Kumar", "+91 99944 45388", null))).get("id").asLong();
        postJson("/api/quotes", quoteJson("Draft Only", "9876543210", null));
        call(post("/api/quotes/" + a + "/share"));
        call(patch("/api/quotes/" + b + "/status").contentType("application/json").content("{\"status\":\"ACCEPTED\"}"));

        JsonNode stats = read(call(get("/api/analytics")).andExpect(status().isOk()));
        assertThat(stats.get("quotedCount").asLong()).isEqualTo(2);
        assertThat(stats.get("quotedValue").decimalValue()).isEqualByComparingTo("92000");
        assertThat(stats.get("wonValue").decimalValue()).isEqualByComparingTo("46000");
        assertThat(stats.get("conversionRate").asDouble()).isEqualTo(1.0);
        assertThat(stats.get("topCustomers")).hasSize(1);
        assertThat(stats.at("/topCustomers/0/quotes").asLong()).isEqualTo(2);
        assertThat(stats.at("/statusCounts/DRAFT").asLong()).isEqualTo(1);
    }

    @Test
    void businessProfileDrivesRoundingAndValidates() throws Exception {
        String profile = """
            {"name":"Masterpiece Interiors","tagline":"Inspired Interiors, Perfected",
             "address":"16/10, Gopalakrishnan Street, Guindy, Chennai – 600032","phonePrimary":"+91 99944 45388",
             "phoneSecondary":"+91 73044 20600","signatory":"N. Kalyan","gstin":"",
             "roundingStep":100,"roundingMode":"UP","validityDays":15}
            """;
        putJson("/api/settings/business", profile).andExpect(status().isOk());
        JsonNode q = read(postJson("/api/quotes", quoteJson("Ravi", "9994445388", null)));
        assertThat(q.get("roundedTotal").decimalValue()).isEqualByComparingTo("46200");
        assertThat(q.get("validUntil").asString()).isEqualTo("2026-11-04");

        putJson("/api/settings/business", profile.replace("\"roundingStep\":100", "\"roundingStep\":7"))
                .andExpect(status().isBadRequest());
        putJson("/api/settings/business", profile.replace("\"gstin\":\"\"", "\"gstin\":\"NOT-A-GSTIN\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void backupRestoresQuotesWithTheirNumbersAndHistory() throws Exception {
        long id = read(postJson("/api/quotes", quoteJson("Ravi", "9994445388",
                "\"totalOverridden\": true, \"roundedTotal\": 44000"))).get("id").asLong();
        postJson("/api/quotes", quoteJson("Priya", "9876543210", null));
        call(post("/api/quotes/" + id + "/share"));
        String backup = call(get("/api/backup")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        cleanDatabase();
        JsonNode result = read(postJson("/api/backup/restore", backup).andExpect(status().isOk()));
        assertThat(result.get("quotesRestored").asInt()).isEqualTo(2);
        assertThat(result.get("snapshotsRestored").asInt()).isEqualTo(1);

        call(get("/api/quotes").param("search", "MP-0001"))
                .andExpect(jsonPath("$[0].customerName").value("Ravi"))
                .andExpect(jsonPath("$[0].roundedTotal").value(44000.0))
                .andExpect(jsonPath("$[0].status").value("SENT"));

        // Restoring again skips everything instead of duplicating.
        assertThat(read(postJson("/api/backup/restore", backup)).get("quotesSkipped").asInt()).isEqualTo(2);

        // New quotes continue numbering after the restored ones.
        assertThat(read(postJson("/api/quotes", quoteJson("New", "9000000000", null))).get("quoteNumber").asInt()).isEqualTo(3);

        postJson("/api/backup/restore", "{\"format\":\"something-else\",\"quotes\":[]}").andExpect(status().isBadRequest());
    }

    @Test
    void defaultsRoundTrip() throws Exception {
        call(get("/api/settings/defaults")).andExpect(jsonPath("$.termItems", hasSize(greaterThan(0))));
        putJson("/api/settings/defaults", "{\"materialSpecItems\":[{\"itemLabel\":\"Ply\"}],\"termItems\":[{\"text\":\"T1\"}]}")
                .andExpect(jsonPath("$.termItems[0].text").value("T1"));
    }
}
