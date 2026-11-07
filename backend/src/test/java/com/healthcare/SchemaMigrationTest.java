package com.healthcare;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthcare.support.FakeAiServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs the real Flyway migration (V1__init_schema.sql) on H2 in MySQL mode and lets Hibernate VALIDATE the
 * entities against it. It catches drift between the entities and the SQL schema. It is not a substitute for
 * running against MySQL 8 itself (see docs/testing.md).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:flywaycheck;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("test")
class SchemaMigrationTest {
    @DynamicPropertySource
    static void ai(DynamicPropertyRegistry r) {
        r.add("app.ai.base-url", () -> FakeAiServer.INSTANCE.baseUrl());
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void migrationCreatesAllTablesAndEntitiesValidate() {
        Integer tables = jdbc.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) in "
                        + "('users','patient_profiles','predictions','prediction_symptoms','prediction_results',"
                        + "'doctors','appointments')", Integer.class);
        assertThat(tables).isEqualTo(7);
    }
}
