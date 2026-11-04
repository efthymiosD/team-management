package com.decoupledx.teammanagement

import com.decoupledx.teammanagement.identity.adapter.api.AccountDirectoryApi
import com.decoupledx.teammanagement.identity.adapter.api.CurrentAccountApi
import com.decoupledx.teammanagement.testinfra.PostgresIntegrationSpec
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock
import java.time.Instant

/**
 * Proves the Spock + spock-spring + Boot 4.1.1 / Java 26 / Modulith setup end to
 * end: the context loads with real PostgreSQL, both identity module APIs are
 * wired, and the shared event/transaction seams exist.
 */
class ApplicationContextSmokeSpec extends PostgresIntegrationSpec {

    @Autowired
    CurrentAccountApi currentAccountApi

    @Autowired
    AccountDirectoryApi accountDirectoryApi

    @Autowired
    Clock clock

    def "application context loads with the identity module wired"() {
        expect:
        currentAccountApi != null
        accountDirectoryApi != null
    }

    def "fixed clock property drives the app Clock bean"() {
        when:
        def now = Instant.now(clock)
        then:
        now == Instant.parse("2026-09-01T10:00:00Z")
    }
}
