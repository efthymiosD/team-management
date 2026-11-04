package com.decoupledx.teammanagement.testinfra

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer
import spock.lang.Specification

/**
 * Base for every integration spec: ONE shared PostgreSQL Testcontainers
 * container per JVM, fixed clock, empty issuer (dev-key fallback — Keycloak is
 * never required), autowired JdbcTemplate for raw-SQL assertions.
 *
 * Carried over from the Reservation System's PostgresIntegrationTest, ported to
 * Spock. Testcontainers 2.x: @ServiceConnection + PostgreSQLContainer is broken,
 * so the container is started statically and wired via @DynamicPropertySource.
 */
@SpringBootTest(properties = [
        "app.clock.fixed-instant=2026-09-01T10:00:00Z",
        "app.security.issuer-uri="
])
abstract class PostgresIntegrationSpec extends Specification {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")

    static {
        POSTGRES.start()
    }

    @Autowired
    JdbcTemplate jdbc

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES.&getJdbcUrl)
        registry.add("spring.datasource.username", POSTGRES.&getUsername)
        registry.add("spring.datasource.password", POSTGRES.&getPassword)
    }

    /** Truncates the given tables so specs stay order-independent. */
    void truncate(String... tables) {
        if (tables.length > 0) {
            jdbc.update("TRUNCATE " + String.join(", ", tables))
        }
    }
}
