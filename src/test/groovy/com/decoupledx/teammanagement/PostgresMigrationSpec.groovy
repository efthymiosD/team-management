package com.decoupledx.teammanagement

import com.decoupledx.teammanagement.testinfra.PostgresIntegrationSpec
import org.springframework.dao.DuplicateKeyException
import java.util.UUID

/**
 * Proves the migration convention (Flyway + flyway-database-postgresql) against
 * a real PostgreSQL instance and the schema's declared invariants.
 */
class PostgresMigrationSpec extends PostgresIntegrationSpec {

    void cleanup() {
        truncate("accounts")
    }

    def "flyway applied the baseline schema successfully"() {
        expect:
        jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Integer.class) == 1
        jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'accounts'",
                Integer.class) == 1
    }

    def "accounts enforces unique idp subjects at the database level"() {
        when:
        jdbc.update("INSERT INTO accounts (account_id, idp_subject) VALUES (?, ?)",
                UUID.randomUUID(), "sub-duplicate")
        jdbc.update("INSERT INTO accounts (account_id, idp_subject) VALUES (?, ?)",
                UUID.randomUUID(), "sub-duplicate")
        then:
        thrown(DuplicateKeyException)
    }
}
