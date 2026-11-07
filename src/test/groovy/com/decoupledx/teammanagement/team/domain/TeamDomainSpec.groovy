package com.decoupledx.teammanagement.team.domain

import java.time.Instant
import java.util.UUID
import spock.lang.Specification

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
        thrown(IllegalArgumentException)
    }

    def "unclaimed profile normalizes blank optional fields"() {
        expect:
        def profile = PlayerProfile.unclaimed("Jan Kowalski", "   ", "  ", "token-123", NOW)
        profile.getContactDetails() == null
        profile.getNationality() == null
    }

    def "unclaimed profile nationality is bounded"() {
        when:
        PlayerProfile.unclaimed("Jan Kowalski", null, "x" * 101, "token-123", NOW)
        then:
        thrown(IllegalArgumentException)
    }

    def "unclaimed profile always carries a claim token and no account"() {
        when:
        def profile = PlayerProfile.unclaimed("Jan Kowalski", "jan@example.com", "PL",
                "token-123", NOW)
        then:
        profile.getAccountId() == null
        profile.getClaimToken() == "token-123"
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
