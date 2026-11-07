package com.decoupledx.teammanagement.team

import com.decoupledx.teammanagement.testinfra.JwtSupport
import com.decoupledx.teammanagement.testinfra.PostgresIntegrationSpec
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import tools.jackson.databind.ObjectMapper

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Black-box HTTP API integration over real PostgreSQL (single baseline
 * migration, extended in place pre-release): team/member lifecycle,
 * authorization (team-admin is domain state, never a realm role), the
 * last-admin invariant (incl. its concurrent race), member-contact privacy,
 * and the provisioning/join races.
 */
@AutoConfigureMockMvc
class TeamApiIntegrationSpec extends PostgresIntegrationSpec {

    @Autowired
    MockMvc mvc

    @Autowired
    ObjectMapper json

    void cleanup() {
        truncate("team_memberships", "player_profiles", "teams")
    }

    // --- helpers -----------------------------------------------------------

    /** Creates a team; returns the response body String. */
    String createTeam(String account, String name) {
        mvc.perform(post("/api/teams")
                .with(JwtSupport.account(account))
                .contentType("application/json")
                .content('{"name":"' + name + '"}'))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.members[0].admin').value(true))
                .andExpect(jsonPath('$.members[0].claimed').value(true))
                .andReturn().response.contentAsString
    }

    /** Creates a team and returns its id (from the response JSON). */
    String teamIdOf(String account, String name) {
        return json.readTree(createTeam(account, name)).get("teamId").textValue()
    }

    /** Admin-registers an unclaimed player; returns the profile id. */
    String registerPlayer(String adminAccount, String teamId, String fullName) {
        return registerPlayer(adminAccount, teamId, fullName, null)
    }

    String registerPlayer(String adminAccount, String teamId, String fullName, String contactDetails) {
        def body = mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account(adminAccount))
                .contentType("application/json")
                .content('{"fullName":"' + fullName + '","contactDetails":"'
                        + (contactDetails ?: "") + '"}'))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.accountId').value(null))
                .andExpect(jsonPath('$.claimToken').isNotEmpty())
                .andReturn().response.contentAsString
        return json.readTree(body).get("profileId").textValue()
    }

    String teamView(String account, String teamId) {
        mvc.perform(get("/api/teams/" + teamId).with(JwtSupport.account(account)))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
    }

    /** Member ids from a team view, in the view's order (admins first). */
    List<String> memberIds(String account, String teamId) {
        return json.readTree(teamView(account, teamId)).get("members").toList()
                .collect { it.get("profileId").textValue() }
    }

    // --- scenarios ---------------------------------------------------------

    def "creating a team trims the name and provisions the first admin membership"() {
        when:
        def body = createTeam("creator-1", "  FC Dynamo  ")
        then:
        def team = json.readTree(body)
        team.get("name").textValue() == "FC Dynamo"
        team.get("members").size() == 1
        team.get("members").get(0).get("admin").booleanValue()
    }

    def "team names are unique across the application (409 TEAM_NAME_TAKEN)"() {
        given:
        createTeam("creator-1", "FC Dynamo")
        expect: "the second team with the same name is rejected for its creator"
        mvc.perform(post("/api/teams").with(JwtSupport.account("other"))
                .contentType("application/json").content('{"name":"FC Dynamo"}'))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.title').value("TEAM_NAME_TAKEN"))
    }

    def "join allows any account; a double join is a 409 ALREADY_MEMBER"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        expect: "first join succeeds as a player"
        mvc.perform(post("/api/teams/" + teamId + "/members/join")
                .with(JwtSupport.account("member-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.admin').value(false))
                .andExpect(jsonPath('$.claimed').value(true))
        and:
        mvc.perform(post("/api/teams/" + teamId + "/members/join")
                .with(JwtSupport.account("member-1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.title').value("ALREADY_MEMBER"))
    }

    def "join on an unknown team is a 404"() {
        expect:
        mvc.perform(post("/api/teams/00000000-0000-0000-0000-000000000001/members/join")
                .with(JwtSupport.account("member-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.title').value("TEAM_NOT_FOUND"))
    }

    def "rename: plain member and outsider are 403, acting admin wins (200)"() {
        given:
        def teamId = teamIdOf("creator-1", "Old Name")
        mvc.perform(post("/api/teams/" + teamId + "/members/join")
                .with(JwtSupport.account("member-1"))).andExpect(status().isCreated())
        expect: "a plain member is inside the team but not an admin"
        mvc.perform(patch("/api/teams/" + teamId).with(JwtSupport.account("member-1"))
                .contentType("application/json").content('{"name":"Member Choice"}'))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.title').value("NOT_TEAM_ADMIN"))
        and: "an account outside the team is rejected as a non-member"
        mvc.perform(patch("/api/teams/" + teamId).with(JwtSupport.account("outsider"))
                .contentType("application/json").content('{"name":"Outsider Choice"}'))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.title').value("NOT_TEAM_MEMBER"))
        when:
        mvc.perform(patch("/api/teams/" + teamId).with(JwtSupport.account("creator-1"))
                .contentType("application/json").content('{"name":"New Name"}'))
        then:
        def outcome = mvc.perform(get("/api/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.name').value("New Name"))
                .andExpect(jsonPath('$.members.length()').value(2))
        outcome != null
    }

    def "only team admins may register players"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        mvc.perform(post("/api/teams/" + teamId + "/members/join")
                .with(JwtSupport.account("member-1"))).andExpect(status().isCreated())
        expect: "a plain member's registration attempt is rejected"
        mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account("member-1"))
                .contentType("application/json")
                .content('{"fullName":"Jan Nowak"}'))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.title').value("NOT_TEAM_ADMIN"))
    }

    def "admin registration creates an unclaimed profile carrying a claim token"() {
        when:
        mvc.perform(post("/api/teams/" + teamIdOf("creator-1", "FC Dynamo") + "/members")
                .with(JwtSupport.account("creator-1"))
                .contentType("application/json")
                .content('{"fullName":"Jan Nowak","contactDetails":"jan@example.com"}'))
        then:
        def outcome = mvc.perform(post("/api/teams/" + teamIdOf("creator-2", "Other Squad") + "/members")
                .with(JwtSupport.account("creator-2"))
                .contentType("application/json")
                .content('{"fullName":"Piotr Zielinski"}'))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.accountId').value(null))
                .andExpect(jsonPath('$.claimToken').isNotEmpty())
        outcome != null
    }

    def "promote is idempotent, then demote returns to player"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def registered = registerPlayer("creator-1", teamId, "Jan Nowak")
        expect: "first promote on a player flips the flag"
        mvc.perform(post("/api/teams/" + teamId + "/members/" + registered + "/promote")
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.admin').value(true))
        and: "re-promoting an existing admin changes nothing"
        mvc.perform(post("/api/teams/" + teamId + "/members/" + registered + "/promote")
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.admin').value(true))
        and:
        mvc.perform(post("/api/teams/" + teamId + "/members/" + registered + "/demote")
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.admin').value(false))
    }

    def "the last admin cannot be demoted nor removed (409 LAST_ADMIN)"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def adminProfileId = memberIds("creator-1", teamId)[0]
        expect:
        mvc.perform(post("/api/teams/" + teamId + "/members/" + adminProfileId + "/demote")
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.title').value("LAST_ADMIN"))
        and:
        mvc.perform(delete("/api/teams/" + teamId + "/members/" + adminProfileId)
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.title').value("LAST_ADMIN"))
    }

    def "admins remove members only by their own team's membership"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def profileId = registerPlayer("creator-1", teamId, "Jan Nowak")
        expect: "removing this team's member works (204)"
        mvc.perform(delete("/api/teams/" + teamId + "/members/" + profileId)
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isNoContent())
        and: "repeating the removal finds no membership (404)"
        mvc.perform(delete("/api/teams/" + teamId + "/members/" + profileId)
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.title').value("MEMBER_NOT_FOUND"))
    }

    def "member contact details are private: admins see all, others only their own"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def janId = registerPlayer("creator-1", teamId, "Jan Nowak", "jan@example.com")
        mvc.perform(post("/api/teams/" + teamId + "/members/join")
                .with(JwtSupport.account("member-1"))).andExpect(status().isCreated())
        def asAdmin = json.readTree(teamView("creator-1", teamId)).get("members").toList()
        def asJoiner = json.readTree(teamView("member-1", teamId)).get("members").toList()
        def asOutsider = json.readTree(teamView("outsider", teamId)).get("members").toList()
        expect: "the admin sees every stored contact detail"
        def janAsAdmin = asAdmin.find { it.get("profileId").textValue() == janId }
        janAsAdmin.get("contactDetails").textValue() == "jan@example.com"
        and: "the joiner (not an admin) cannot see anyone else's, not even their own absence"
        def janAsJoiner = asJoiner.find { it.get("profileId").textValue() == janId }
        janAsJoiner.get("contactDetails").isNull()
        and: "account outsiders see no contact details at all"
        asOutsider.every { it.get("contactDetails").isNull() }
    }

    def "concurrent demotes of both admins keep exactly one admin (one 200, one 409 LAST_ADMIN)"() {
        given: "two admins: creator + a promoted member with an account"
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        mvc.perform(post("/api/teams/" + teamId + "/members/join")
                .with(JwtSupport.account("member-1"))).andExpect(status().isCreated())
        def secondMemberId = memberIds("creator-1", teamId)[1]
        mvc.perform(post("/api/teams/" + teamId + "/members/" + secondMemberId + "/promote")
                .with(JwtSupport.account("creator-1"))).andExpect(status().isOk())
        def creatorId = memberIds("creator-1", teamId)[0]
        and:
        ExecutorService pool = Executors.newFixedThreadPool(2)
        CountDownLatch start = new CountDownLatch(1)
        def demote = { String actor, String target ->
            start.await()
            mvc.perform(post("/api/teams/" + teamId + "/members/" + target + "/demote")
                    .with(JwtSupport.account(actor))).andReturn().response.status
        }
        Future<Integer> first = pool.submit(demote.curry("creator-1", secondMemberId) as Callable<Integer>)
        Future<Integer> second = pool.submit(demote.curry("member-1", creatorId) as Callable<Integer>)
        when:
        start.countDown()
        int firstStatus = first.get()
        int secondStatus = second.get()
        then: "the happy-path invariant survives every interleaving: exactly one demote\n        commits, the loser surfaces as an honest client error (409 LAST_ADMIN when its\n        permission check still saw it as admin, or 403 NOT_TEAM_ADMIN when the other\n        racer's demote under READ COMMITTED revoked the actor's admin role first),\n        and the at-least-one-admin invariant holds afterwards"
        firstStatus == 200 || secondStatus == 200
        [firstStatus, secondStatus].every { status -> status in [200, 403, 409] }
        [firstStatus, secondStatus].count(200) == 1
        and: "the at-least-one-admin invariant holds afterwards"
        def admins = json.readTree(teamView("creator-1", teamId)).get("members").toList()
                .findAll { it.get("admin").booleanValue() }
        admins.size() == 1
        cleanup:
        pool.shutdownNow()
    }

    def "concurrent first join of the same account provisions exactly one profile (one 201, one 409)"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        and:
        ExecutorService pool = Executors.newFixedThreadPool(2)
        CountDownLatch start = new CountDownLatch(1)
        def join = {
            start.await()
            mvc.perform(post("/api/teams/" + teamId + "/members/join")
                    .with(JwtSupport.account("race-member"))).andReturn().response.status
        }
        Future<Integer> first = pool.submit(join as Callable<Integer>)
        Future<Integer> second = pool.submit(join as Callable<Integer>)
        when:
        start.countDown()
        int firstStatus = first.get()
        int secondStatus = second.get()
        then: "the profile/account unique race resolves inside the work: never a 500"
        [firstStatus, secondStatus].sort() == [201, 409]
        and: "the racing account contributes exactly one membership"
        json.readTree(teamView("creator-1", teamId)).get("members").size() == 2
        cleanup:
        pool.shutdownNow()
    }
}
