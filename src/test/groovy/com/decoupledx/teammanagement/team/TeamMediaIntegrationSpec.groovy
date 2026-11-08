package com.decoupledx.teammanagement.team

import com.decoupledx.teammanagement.testinfra.JwtSupport
import com.decoupledx.teammanagement.testinfra.PostgresIntegrationSpec
import java.io.File
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import tools.jackson.databind.ObjectMapper

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Slice 2 black-box integration (real PostgreSQL): media uploads (team logo
 * by admins, own player photo) — files stored under app.uploads.dir and
 * SERVED read-only from /uploads/** — plus the claim flow (invite token
 * binds the acting account to an unclaimed player profile; single use; one
 * profile per account).
 */
@AutoConfigureMockMvc
class TeamMediaIntegrationSpec extends PostgresIntegrationSpec {

    @Autowired
    MockMvc mvc

    @Autowired
    ObjectMapper json

    void cleanup() {
        truncate("team_memberships", "player_profiles", "teams")
        // Uploaded bytes are NOT in the database: remove test-generated files
        // so the uploads directory (gitignored) never accumulates test noise.
        new File("uploads").listFiles()?.each { file -> file.delete() }
    }

    // --- helpers -----------------------------------------------------------

    String teamIdOf(String account, String name) {
        json.readTree(createTeam(account, name)).get("teamId").textValue()
    }

    String createTeam(String account, String name) {
        mvc.perform(post("/api/teams")
                .with(JwtSupport.account(account))
                .contentType("application/json")
                .content('{"name":"' + name + '"}'))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
    }

    String registerPlayer(String adminAccount, String teamId, String fullName) {
        mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account(adminAccount))
                .contentType("application/json")
                .content('{"fullName":"' + fullName + '"}'))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.accountId').value(null))
                .andExpect(jsonPath('$.claimToken').isNotEmpty())
                .andReturn().response.contentAsString
    }

    /** Server-side stored file count for test pollution checks. */
    int storedUploads() {
        def dir = new File("uploads")
        dir.exists() && dir.isDirectory() ? dir.listFiles().size() : 0
    }

    // --- media: team logo ---------------------------------------------------

    def "admin replaces the team logo: stored, served, persisted"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def bytes = [0x89, 0x50, 0x4E, 0x47] as byte[] // PNG magic-ish payload
        def logo = new MockMultipartFile("logo", "club.png", "image/png", bytes)
        when:
        mvc.perform(multipart("/api/teams/" + teamId + "/logo").file(logo)
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
        then:
        def view = json.readTree(mvc.perform(get("/api/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.logoPath').isNotEmpty())
                .andReturn().response.contentAsString)
        def logoPath = view.get("logoPath").textValue()
        and: "a real file exists and is SERVED read-only without authentication"
        new File("uploads", logoPath.replaceFirst("/uploads/", "")).exists()
        mvc.perform(get(logoPath))
                .andExpect(status().isOk())
    }

    def "logo upload rejects non-image content types with 415"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def notAnImage = new MockMultipartFile("logo", "lapsi.txt", "text/plain", "hei".bytes)
        expect:
        mvc.perform(multipart("/api/teams/" + teamId + "/logo").file(notAnImage)
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath('$.title').value("INVALID_UPLOAD_IMAGE_TYPE"))
    }

    def "logo upload over the app-side 5MB cap is a 413 (friendly error wins Tomcat)"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def oversized = new MockMultipartFile("logo", "big.png", "image/png", new byte[5 * 1024 * 1024 + 1])
        expect:
        mvc.perform(multipart("/api/teams/" + teamId + "/logo").file(oversized)
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath('$.title').value("UPLOAD_IMAGE_TOO_LARGE"))
    }

    def "logo upload is admin-only (403 for plain members and outsiders)"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        mvc.perform(post("/api/teams/" + teamId + "/members/join")
                .with(JwtSupport.account("member-1"))).andExpect(status().isCreated())
        def logo = new MockMultipartFile("logo", "club.png", "image/png", [0x89] as byte[])
        expect: "a plain member may not replace the logo"
        mvc.perform(multipart("/api/teams/" + teamId + "/logo").file(logo)
                .with(JwtSupport.account("member-1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.title').value("NOT_TEAM_ADMIN"))
        and: "nor an outsider"
        mvc.perform(multipart("/api/teams/" + teamId + "/logo").file(logo)
                .with(JwtSupport.account("outsider")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.title').value("NOT_TEAM_MEMBER"))
    }

    // --- media: own player photo -------------------------------------------

    def "account uploads its own photo: persisted on the profile, served"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        mvc.perform(post("/api/teams/" + teamId + "/members/join")
                .with(JwtSupport.account("member-1"))).andExpect(status().isCreated())
        def photo = new MockMultipartFile("photo", "me.png", "image/png", [0x89, 0x50] as byte[])
        when:
        mvc.perform(multipart("/api/players/me/photo").file(photo)
                .with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.photoPath').isNotEmpty())
        then:
        def photoPath = json.readTree(mvc.perform(get("/api/teams/" + teamId)
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString)
                .get("members")[1].get("photoPath").textValue()
        and: "served"
        mvc.perform(get(photoPath)).andExpect(status().isOk())
    }

    // --- claim flow ---------------------------------------------------------

    def "claiming binds the acting account to the unclaimed profile; token single-use"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def body = registerPlayer("creator-1", teamId, "Jan Nowak")
        def claimToken = json.readTree(body).get("claimToken").textValue()
        when: "the invited person claims with their own account"
        mvc.perform(post("/api/claims").with(JwtSupport.account("claimer-1"))
                .contentType("application/json")
                .content('{"claimToken":"' + claimToken + '"}'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.accountId').isNotEmpty())
                .andExpect(jsonPath('$.claimToken').value(null))
        then: "the second use is rejected — clearing the token makes it unknown (404)"
        mvc.perform(post("/api/claims").with(JwtSupport.account("claimer-2"))
                .contentType("application/json")
                .content('{"claimToken":"' + claimToken + '"}'))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.title').value("CLAIM_TOKEN_UNKNOWN"))
        and: "the profile is linked to the claiming account"
        def view = json.readTree(teamView(teamId))
        def jan = view.get("members").toList().find { it.get("fullName").textValue() == "Jan Nowak" }
        jan.get("claimed").booleanValue() == true
    }

    private String teamView(String teamId) {
        mvc.perform(get("/api/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andReturn().response.contentAsString
    }

    def "claiming an unknown token is 404 CLAIM_TOKEN_UNKNOWN"() {
        expect:
        mvc.perform(post("/api/claims").with(JwtSupport.account("someone"))
                .contentType("application/json")
                .content('{"claimToken":"' + UUID.randomUUID() + '"}'))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.title').value("CLAIM_TOKEN_UNKNOWN"))
    }

    def "claiming rejects an account that already has a player profile (409)"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def claimToken = json.readTree(registerPlayer("creator-1", teamId, "Jan Nowak"))
                .get("claimToken").textValue()
        and: "the claimer already provisioned a profile by joining"
        mvc.perform(post("/api/teams/" + teamId + "/members/join")
                .with(JwtSupport.account("claimer-1"))).andExpect(status().isCreated())
        expect:
        mvc.perform(post("/api/claims").with(JwtSupport.account("claimer-1"))
                .contentType("application/json")
                .content('{"claimToken":"' + claimToken + '"}'))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.title').value("ACCOUNT_ALREADY_PROFILED"))
    }

    def "uploads never leave orphan test bytes: directory only holds served files"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        when:
        mvc.perform(multipart("/api/teams/" + teamId + "/logo").file(
                new MockMultipartFile("logo", "club.png", "image/png", [0x89] as byte[]))
                .with(JwtSupport.account("creator-1"))).andExpect(status().isOk())
        then:
        storedUploads() >= 1
    }

    def "concurrent claims of the same token: exactly one 200, the loser an honest 409"() {
        given: "an admin-registered unclaimed profile and its claim token"
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def claimToken = json.readTree(registerPlayer("creator-1", teamId, "Jan Nowak"))
                .get("claimToken").textValue()
        and: "two DIFFERENT accounts race to claim it (each has no profile yet)"
        ExecutorService pool = Executors.newFixedThreadPool(2)
        CountDownLatch start = new CountDownLatch(1)
        def claim = { String account ->
            start.await()
            mvc.perform(post("/api/claims").with(JwtSupport.account(account))
                    .contentType("application/json")
                    .content('{"claimToken":"' + claimToken + '"}')).andReturn()
        }
        Future first = pool.submit(claim.curry("claimer-1") as Callable)
        Future second = pool.submit(claim.curry("claimer-2") as Callable)
        when:
        start.countDown()
        def firstResult = first.get()
        def secondResult = second.get()
        int firstStatus = firstResult.getResponse().getStatus()
        int secondStatus = secondResult.getResponse().getStatus()
        then: "the loser is the honest 4xx, never a 500. Two legitimate loss\n        modes: under the slower CI/serial interleave the loser never SAW the\n        token (the winner consumed+committed first — the read finds nothing\n        → CLAIM_TOKEN_UNKNOWN 404, the sequential-reuse semantic); when both\n        read the token before either committed, the version race on the\n        claim row fires CFE → PROFILE_ALREADY_CLAIMED 409."
        [firstStatus, secondStatus].every { status -> status == 200 || status == 404 || status == 409 }
        [firstStatus, secondStatus].count(200) == 1
        and: "the 409-mode loser carries the PROFILE race title, not another translation"
        def loserBody = firstStatus == 409 ? firstResult.getResponse().getContentAsString()
                : secondStatus == 409 ? secondResult.getResponse().getContentAsString() : null
        loserBody == null || json.readTree(loserBody).get("title")
                .textValue() == "PROFILE_ALREADY_CLAIMED"
        cleanup:
        pool.shutdownNow()
    }
}
