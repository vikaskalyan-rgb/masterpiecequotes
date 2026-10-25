package com.masterpiece.quotes.api;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Replays production's situation: tables created by the old app (no Flyway history yet) holding
// real-looking data, then the new app's migrations run against it.
class LegacyMigrationTest {

    private static final String SCHEMA = "legacy_migration_test";

    @Test
    void existingDatabaseIsBaselinedAndUpgradedWithoutLosingData() throws Exception {
        String url = System.getenv().getOrDefault("TEST_DB_URL", "jdbc:postgresql://localhost:5432/quotes_test");
        DriverManagerDataSource admin = new DriverManagerDataSource(url,
                System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"),
                System.getenv().getOrDefault("TEST_DB_PASSWORD", "postgres"));
        JdbcTemplate adminJdbc = new JdbcTemplate(admin);
        adminJdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        adminJdbc.execute("CREATE SCHEMA " + SCHEMA);

        DriverManagerDataSource ds = new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA,
                admin.getUsername(), admin.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        // The old app's tables, created outside Flyway (as Hibernate ddl-auto did).
        String v1 = new ClassPathResource("db/migration/V1__baseline_schema.sql").getContentAsString(StandardCharsets.UTF_8);
        jdbc.execute(v1);
        jdbc.execute("""
            INSERT INTO quotes (customer_name, customer_phone, quote_date, status, subtotal, rounded_total, accessories_amount, created_at, updated_at) VALUES
              ('Ravi',  '+91 99944 45388', '2026-08-01', 'DRAFT', 124499, 124000, 0, now() - interval '2 day', now()),
              ('Priya', '09876543210',     '2026-08-02', 'DRAFT', 100200,  95000, 0, now() - interval '1 day', now());
            INSERT INTO rooms (quote_id, name, sort_order) VALUES (1, 'Kitchen', 0);
            INSERT INTO items (room_id, description, length, width, quantity, unit, rate, amount, sort_order) VALUES
              (1, 'Wardrobe Framed (7X4)', 7, 4, 28, 'SQFT', 1500, 42000, 0),
              (1, 'Odd label (9X9)',       7, 4, 28, 'SQFT', 100,   2800, 1);
            INSERT INTO default_term_items (text, sort_order) VALUES ('Existing custom term', 0);
            """);

        Flyway.configure().dataSource(ds).schemas(SCHEMA).baselineOnMigrate(true).baselineVersion("1").load().migrate();

        List<Map<String, Object>> quotes = jdbc.queryForList(
                "SELECT customer_phone, quote_number, total_overridden FROM quotes ORDER BY id");
        assertThat(quotes.get(0)).containsEntry("customer_phone", "919994445388").containsEntry("quote_number", 1)
                .containsEntry("total_overridden", false);
        assertThat(quotes.get(1)).containsEntry("customer_phone", "919876543210").containsEntry("quote_number", 2)
                .containsEntry("total_overridden", true);
        assertThat(jdbc.queryForList("SELECT description FROM items ORDER BY id", String.class))
                .containsExactly("Wardrobe Framed", "Odd label (9X9)");
        // Customised defaults are kept, not re-seeded.
        assertThat(jdbc.queryForList("SELECT text FROM default_term_items", String.class)).containsExactly("Existing custom term");
        assertThat(jdbc.queryForObject("SELECT name FROM business_profile", String.class)).isEqualTo("Masterpiece Interiors");
        // The next quote continues the numbering.
        Integer next = jdbc.queryForObject("""
            INSERT INTO quotes (customer_name, customer_phone, quote_date, status, created_at, updated_at)
            VALUES ('Next', '919000000000', '2026-10-25', 'DRAFT', now(), now()) RETURNING quote_number
            """, Integer.class);
        assertThat(next).isEqualTo(3);

        adminJdbc.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
    }
}
