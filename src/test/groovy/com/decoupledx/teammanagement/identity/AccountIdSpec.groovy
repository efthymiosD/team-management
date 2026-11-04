package com.decoupledx.teammanagement.identity

import com.decoupledx.teammanagement.identity.adapter.api.AccountId
import spock.lang.Specification

/**
 * Plain Spock domain unit test (no Spring, no database) — the fastest layer,
 * proving the Spock toolchain itself alongside the AccountId value type.
 */
class AccountIdSpec extends Specification {

    def "rejects null and blank values"() {
        when:
        AccountId.of(value)
        then:
        thrown(RuntimeException)
        where:
        value << [null, '', '   ']
    }

    def "of preserves the given value"() {
        expect:
        AccountId.of("account-1").value() == "account-1"
    }

    def "random produces unique non-blank ids"() {
        when:
        def first = AccountId.random()
        def second = AccountId.random()
        then:
        first.value()
        second.value()
        first != second
    }
}
