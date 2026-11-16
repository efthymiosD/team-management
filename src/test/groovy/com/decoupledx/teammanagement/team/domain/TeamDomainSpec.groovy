package com.decoupledx.teammanagement.team.domain

import com.decoupledx.common.error.BusinessException
import spock.lang.Specification

import java.time.Instant

/**
 * Domain invariants, no Spring and no database: name validation, claim
 * semantics, membership role transitions.
 */
class TeamDomainSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z")

    def "team names are trimmed and bounded"() {
        when:
        def team = Team.create(value, NOW)
        then:
        team.getName() == expected
        where:
        value        || expected
        " FC Dynamo " || "FC Dynamo"
        " foo"        || "foo"
    }

    def "team name rejects blank and oversized values"() {
        when:
        Team.create(value, NOW)
        then:
        thrown(IllegalArgumentException)
        where:
        value          || _
        ""              || _
        "   "           || _
        null            || _
        "x".repeat(201) || _
    }

    def "player profile rejects a blank name"() {
        when:
        PlayerProfile.forAccount(UUID.randomUUID(), "", NOW)
        then:
        thrown(BusinessException)
    }

    def "unclaimed profile keeps optional fields empty"() {
        expect:
        def profile = PlayerProfile.unclaimed("Jan Kowalski", null, null, null,
                "token-123-hash", NOW)
        profile.getPhone() == null
        profile.getNationality() == null
    }

    def "unclaimed profile validates an optional phone and normalizes nationality"() {
        expect:
        def profile = PlayerProfile.unclaimed("Jan Kowalski", "+48 600 000 000", null, "pl",
                "token-123-hash", NOW)
        profile.getPhone() == "+48 600 000 000"
        profile.getNationality() == "PL"
        when:
        PlayerProfile.unclaimed("Jan Kowalski", "600-000-000", null, null,
                "token-123-hash", NOW)
        then:
        thrown(BusinessException)
    }

    def "unclaimed profile nationality is bounded"() {
        when:
        PlayerProfile.unclaimed("Jan Kowalski", null, null, "x" * 101,
                "token-123-hash", NOW)
        then:
        thrown(BusinessException)
    }

    def "self-service profile edit: name editable, phone required, nationality is ISO"() {
        given:
        def profile = PlayerProfile.from(PlayerProfile.unclaimed("Jan Kowalski", null, null, null,
                "token-123-hash", NOW).toData())
        when: "the full self-service edit with the required phone"
        profile.updateDetails("Jan Nowak", "+48 600 000 000",
                java.time.LocalDate.parse("1995-03-14"), "pl", NOW)
        then:
        profile.getFullName() == "Jan Nowak"
        profile.getPhone() == "+48 600 000 000"
        profile.getBirthDate().toString() == "1995-03-14"
        profile.getNationality() == "PL"
        profile.isComplete() == true
        and: "a blank phone is a validation error (required on save)"
        when:
        def profile2 = PlayerProfile.from(PlayerProfile.unclaimed("Jan Kowalski", null, null, null,
                "token-123-hash", NOW).toData())
        profile2.updateDetails("Jan Nowak", "  ", null, null, NOW)
        then:
        thrown(BusinessException)
        and: "a malformed phone is a validation error"
        when:
        def profile3 = PlayerProfile.from(PlayerProfile.unclaimed("Jan Kowalski", null, null, null,
                "token-123-hash", NOW).toData())
        profile3.updateDetails("Jan Nowak", "600-000-000", null, null, NOW)
        then:
        thrown(BusinessException)
        and: "a non-ISO nationality is a validation error"
        when:
        def profile4 = PlayerProfile.from(PlayerProfile.unclaimed("Jan Kowalski", null, null, null,
                "token-123-hash", NOW).toData())
        profile4.updateDetails("Jan Nowak", "+48 600 000 000", null, "Polska", NOW)
        then:
        thrown(BusinessException)
        and: "a future birthday is a validation error"
        when:
        def profile5 = PlayerProfile.from(PlayerProfile.unclaimed("Jan Kowalski", null, null, null,
                "token-123-hash", NOW).toData())
        profile5.updateDetails("Jan Nowak", "+48 600 000 000",
                java.time.LocalDate.parse("2030-01-01"), null, NOW)
        then:
        thrown(BusinessException)
    }

    def "unclaimed profile always carries a claim token hash and no account"() {
        when:
        def profile = PlayerProfile.unclaimed("Jan Kowalski", null, null, "PL",
                "token-123-hash", NOW)
        then:
        profile.getAccountId() == null
        profile.getClaimTokenHash() == "token-123-hash"
        profile.getNationality() == "PL"
    }

    def "membership role transitions flip exactly as requested"() {
        given:
        def membership = TeamMembership.create(UUID.randomUUID(), UUID.randomUUID(),
                initial, NOW)
        expect:
        membership.isAdmin() == initialAdmin
        when:
        membership."${operation}"()
        then:
        membership.isAdmin() == targetAdmin
        where:
        initial              | operation   || initialAdmin | targetAdmin
        TeamMembershipRole.PLAYER | "promote" || false        | true
        TeamMembershipRole.ADMIN  | "demote"  || true         | false
        TeamMembershipRole.PLAYER | "demote"  || false        | false
        TeamMembershipRole.ADMIN  | "promote" || true         | true
    }
}
