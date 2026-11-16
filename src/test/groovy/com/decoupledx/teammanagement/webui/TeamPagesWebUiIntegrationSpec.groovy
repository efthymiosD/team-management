package com.decoupledx.teammanagement.webui

import com.decoupledx.teammanagement.testinfra.JwtSupport
import com.decoupledx.teammanagement.testinfra.PostgresIntegrationSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

/**
 * The team management UI flows per the APPROVED mockups (§1–§6, black-box
 * over real PostgreSQL): team details page (identity header + players grid +
 * invite link), the member-profile page with the phone-privacy rule, the
 * join hero, my-profile full editing (name/phone/birthday/nationality), and
 * the last-admin protection surfacing as a translated flash.
 */
@AutoConfigureMockMvc
class TeamPagesWebUiIntegrationSpec extends PostgresIntegrationSpec {

    @Autowired
    MockMvc mvc

    @Autowired
    tools.jackson.databind.ObjectMapper json

    def cleanup() {
        truncate("team_invites", "team_memberships", "player_profiles", "teams")
    }

    // --- helpers -----------------------------------------------------------

    String createTeam(String account, String name) {
        mvc.perform(post("/api/teams").with(JwtSupport.account(account))
                .contentType("application/json")
                .content('{"name":"' + name + '"}'))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
    }

    String teamIdOf(String account, String name) {
        json.readTree(createTeam(account, name)).get("teamId").textValue()
    }

    String registerPlayer(String adminAccount, String teamId, String fullName) {
        mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account(adminAccount))
                .contentType("application/json")
                .content('{"fullName":"' + fullName + '"}'))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
    }

    /** Joins via a fresh one-time invite (API-level setup helper). */
    void joinTeam(String account, String teamId) {
        joinViaToken(mintToken("creator-1", teamId), account, teamId)
    }

    String mintToken(String adminAccount, String teamId) {
        def body = mvc.perform(post("/api/teams/" + teamId + "/invites")
                .with(JwtSupport.account(adminAccount)))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
        return json.readTree(body).get("rawToken").textValue()
    }

    void joinViaToken(String rawToken, String account, String teamId) {
        mvc.perform(post("/api/teams/" + teamId + "/join/" + rawToken)
                .with(JwtSupport.account(account)))
                .andExpect(status().isCreated())
    }

    /** The self-service full-profile edit (phone required). */
    void saveProfile(String account, String fullName, String phone) {
        mvc.perform(post("/players/me/profile").with(JwtSupport.account(account))
                .with(csrf()).param("fullName", fullName).param("phone", phone))
                .andExpect(status().is3xxRedirection())
    }

    /** The team's JSON API view (rendered team pages are HTML, not JSON). */
    String apiTeam(String account, String teamId) {
        return mvc.perform(get("/api/teams/" + teamId).with(JwtSupport.account(account)))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
    }

    /** The profileId of a member by name, read off the team's JSON API. */
    List<String> memberIdsByApi(String account, String teamId) {
        json.readTree(apiTeam(account, teamId)).get("members").toList()
                .collect { it.get("profileId").textValue() }
    }

    // --- scenarios ---------------------------------------------------------

    def "team page: admin sees the identity header and the management grid"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def page = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("team"))
                .andReturn().response.contentAsString
        expect:
        page.contains("FC Dynamo")
        page.contains('data-testid="member-actions"')
        page.contains('class="player-grid"')
    }

    def "member profile page: admin sees the member's phone, others get the lock pill"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def body = mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1"))
                .contentType("application/json")
                .content('{"fullName":"Jan Nowak","phone":"+48 600 000 000"}'))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
        def janId = json.readTree(body).get("profileId").textValue()
        and: "Jan also sets a birthday on his profile (regression: the page must render it)"
        def janToken = json.readTree(body).get("claimToken").textValue()
        mvc.perform(post("/api/claims").with(JwtSupport.account("jan-owner"))
                .contentType("application/json").content('{"claimToken":"' + janToken + '"}'))
                .andExpect(status().isOk())
        mvc.perform(post("/players/me/profile").with(JwtSupport.account("jan-owner"))
                .with(csrf()).param("fullName", "Jan Nowak").param("phone", "+48 600 000 000")
                .param("birthDate", "1995-03-14").param("nationality", "PL"))
                .andExpect(status().is3xxRedirection())
        when: "the team administrator opens the member's profile"
        def adminView = mvc.perform(get("/teams/" + teamId + "/members/" + janId)
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("member"))
                .andReturn().response.contentAsString
        then:
        adminView.contains("Jan Nowak")
        and: "the phone renders readable + clickable (owner feedback)"
        adminView.contains('href="tel:+48600000000"')
        adminView.contains("+48 600 000 000")
        and: "the birthday renders locale-formatted with the current age"
        adminView.contains("1995") // the birthday renders (locale-formatted)
        adminView.contains("31 years old") // fixed clock 2026-09-01 vs 1995-03-14
        adminView.contains("🇵🇱") // the flag renders
        !adminView.contains("Contact details are visible to team administrators only.")
        when: "a non-admin teammate opens the same profile"
        joinTeam("member-1", teamId)
        def teammateView = mvc.perform(get("/teams/" + teamId + "/members/" + janId)
                .with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        then:
        teammateView.contains("Jan Nowak")
        !teammateView.contains("+48 600 000 000")
        teammateView.contains('data-testid="contact-locked"')
    }

    def "team page contact privacy: non-admin member rows carry no phone"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        joinTeam("member-1", teamId)
        saveProfile("member-1", "Member One", "+48 700 000 001")
        when: "the member views the team page"
        def memberPage = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("team"))
                .andReturn().response.contentAsString
        then: "the grid never renders phone numbers; flags render as chips"
        !memberPage.contains("+48 700 000 001")
        memberPage.contains('class="player-grid"')
    }

    
    def "the team logo serves real bytes from its stored URL"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def logo = new MockMultipartFile("logo", "club.png", "image/png", [0x89, 0x50] as byte[])
        mvc.perform(multipart("/teams/" + teamId + "/logo").file(logo)
                .with(JwtSupport.account("creator-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
        when: "the browser fetches the logo URL stored on the team"
        def logoPath = json.readTree(apiTeam("creator-1", teamId)).get("logoPath").textValue()
        def logoResponse = mvc.perform(get(logoPath))
        then:
        logoResponse.andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
    }

    def "unauthenticated visitors are redirected to sign in from a team URL"() {
        expect:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def result = mvc.perform(get("/teams/" + teamId + "/join"))
        result.andExpect(status().is3xxRedirection())
    }

    // --- §12 one-time invite UI flow ----------------------------------------

    def "minting shows the fresh one-time link once; later reloads never show it"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        when: "the admin mints an invite"
        def minted = mvc.perform(post("/teams/" + teamId + "/invites")
                .with(JwtSupport.account("creator-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        then: "the flash carries the FULL absolute link"
        def link = minted.flashMap.get("inviteLink").toString()
        link.startsWith(baseUrl() + "/teams/" + teamId + "/join/")
        and: "a LATER reload (flash consumed) shows the ACTIVE list, never the link"
        def page = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        !page.contains("/join/")
        page.contains("Outstanding invite")
    }

    static String baseUrl() {
        "http://localhost:8080"
    }

    def "the one-time invite page joins the player; reuse lands on the invalid page"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def rawToken = mintToken("creator-1", teamId)
        when: "the invited player opens the link"
        def hero = mvc.perform(get("/teams/" + teamId + "/join/" + rawToken)
                .with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("join"))
                .andReturn().response.contentAsString
        then:
        hero.contains("FC Dynamo")
        !hero.contains("creator-1") // no roster leak on the hero
        when: "the join form posts"
        def joined = mvc.perform(post("/teams/" + teamId + "/join/" + rawToken)
                .with(JwtSupport.account("member-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        then:
        joined.response.redirectedUrl == "/teams/" + teamId
        and: "the SAME token is spent — the page is the friendly invalid one"
        def reused = mvc.perform(get("/teams/" + teamId + "/join/" + rawToken)
                .with(JwtSupport.account("member-2")))
                .andExpect(status().isOk())
                .andExpect(view().name("invalid-link"))
                .andReturn().response.contentAsString
        reused.contains("no longer valid")
        and: "posting the spent token also ends on the invalid page"
        def spent = mvc.perform(post("/teams/" + teamId + "/join/" + rawToken)
                .with(JwtSupport.account("member-2")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("invalid-link"))
        spent != null
    }

    def "revoking the outstanding invite invalidates its link"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def rawToken = mintToken("creator-1", teamId)
        and: "a flash-free reload exposes the revoke affordance"
        def clean = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        def matcher = java.util.regex.Pattern.compile('/teams/' + teamId
                + '/invites/([0-9a-f-]{36})/revoke')
        def inviteMatcher = matcher.matcher(clean)
        inviteMatcher.find()
        def inviteId = inviteMatcher.group(1)
        when: "the admin revokes"
        mvc.perform(post("/teams/" + teamId + "/invites/" + inviteId + "/revoke")
                .with(JwtSupport.account("creator-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
        then: "the link is dead for its holder"
        mvc.perform(get("/teams/" + teamId + "/join/" + rawToken)
                .with(JwtSupport.account("member-1")))
                .andExpect(view().name("invalid-link"))
        mvc.perform(post("/teams/" + teamId + "/join/" + rawToken)
                .with(JwtSupport.account("member-1")).with(csrf()))
                .andExpect(view().name("invalid-link"))
    }

    def "the legacy open join link renders the invalid page (exploit closed)"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        expect:
        def page = mvc.perform(get("/teams/" + teamId + "/join")
                .with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("invalid-link"))
                .andReturn().response.contentAsString
        page.contains("no longer valid")
    }

    // --- §12.1 add-member UI flow -------------------------------------------

    def "admin adds a pending member and shares the claim link"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        when: "the add-member form posts"
        def added = mvc.perform(post("/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1")).with(csrf())
                .param("fullName", "Dimitris Vlachos").param("phone", "+30 694 555 0142")
                .param("nationality", "GR"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        then: "the flash carries the personal claim link (per-member deeplink)"
        def claimLink = added.flashMap.get("claimLink").toString()
        claimLink.startsWith(baseUrl() + "/teams/" + teamId + "/members/")
        and: "a reload shows the claim-link card REPLACING the form"
        def page = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1"))
                .session(added.request.session))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains(claimLink)
        !page.contains('id="memberName"')
    }

    def "the pending member claims their profile through the claim page"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def added = mvc.perform(post("/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1")).with(csrf())
                .param("fullName", "Dimitris Vlachos").param("phone", "+30 694 555 0142"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        def rawToken = added.flashMap.get("claimLink").toString()
                .substring((baseUrl() + "/teams/" + teamId + "/members/").length())
        // strip the /members/{profileId}/claim/ prefix — keep only the token
        rawToken = rawToken.substring(rawToken.lastIndexOf('/claim/') + '/claim/'.length())
        when: "the player opens the claim link (login-first, then the hero)"
        def previewProfileId = json.readTree(apiTeam("creator-1", teamId)).get("members").toList()
                .find { it.get("fullName").textValue() == "Dimitris Vlachos" }
                .get("profileId").textValue()
        def hero = mvc.perform(get("/teams/" + teamId + "/members/" + previewProfileId
                + "/claim/" + rawToken).with(JwtSupport.account("dimitris-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("claim"))
                .andReturn().response.contentAsString
        then: "the identity hint the admin entered renders — no e-mail asked"
        hero.contains("Dimitris Vlachos")
        hero.contains("+30 694 555 014")
        hero.contains("created for you")
        when: "the claim form posts"
        def claimed = mvc.perform(post("/teams/" + teamId + "/members/" + previewProfileId
                + "/claim/" + rawToken).with(JwtSupport.account("dimitris-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        then: "the landing is the profile-completion page (the gate's target)"
        claimed.response.redirectedUrl == "/players/me"
        and: "the pending profile is now the claimer's — visible on the team page"
        def teamJson = json.readTree(apiTeam("creator-1", teamId))
        teamJson.get("members").toList()
                .findAll { it.get("fullName").textValue() == "Dimitris Vlachos" }.size() == 1
        and: "the claim token is spent"
        mvc.perform(get("/teams/" + teamId + "/members/" + previewProfileId
                + "/claim/" + rawToken).with(JwtSupport.account("dimitris-1")))
                .andExpect(view().name("invalid-link"))
    }

    def "an invalid phone on add-member keeps the typed draft"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        when:
        def failed = mvc.perform(post("/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1")).with(csrf())
                .param("fullName", "Dimitris Vlachos").param("phone", "694555")
                .locale(java.util.Locale.forLanguageTag("pl")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "Wartość jednego z pól profilu jest nieprawidłowa"))
                .andReturn()
        then: "the typed draft renders back into the form"
        def page = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1"))
                .session(failed.request.session))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains('value="Dimitris Vlachos"')
        page.contains('value="694555"')
    }

    
    def "promote and demote via the team page forms"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def body = registerPlayer("creator-1", teamId, "Jan Nowak")
        def profileId = json.readTree(body).get("profileId").textValue()
        and: "a PENDING member's Promote is grayed AND disabled (owner tweak)"
        def page = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        def janPromote = page.indexOf('/members/' + profileId + '/promote')
        janPromote > 0
        page.substring(janPromote, janPromote + 500).contains('disabled="disabled"')
        and: "the last-admin Remove is ALSO disabled → two disabled buttons in a 1-admin team with a pending member"
        (page =~ /disabled="disabled"/).count == 2
        when: "Jan claims via the API"
        mvc.perform(post("/api/claims").with(JwtSupport.account("claimer-4"))
                .contentType("application/json")
                .content('{"claimToken":"' + json.readTree(body).get("claimToken").textValue() + '"}'))
                .andExpect(status().isOk())
        then: "once Jan claims, the promote button becomes eligible (no disabled)"
        def page2 = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        def janPromote2 = page2.indexOf('/members/' + profileId + '/promote')
        !page2.substring(janPromote2, janPromote2 + 400).contains('disabled="disabled"')
        and: "the last-admin Remove is the only remaining disabled button"
        (page2 =~ /disabled="disabled"/).count == 1
        when: "promoting Jan via the team page form"
        mvc.perform(post("/teams/" + teamId + "/members/" + profileId + "/promote")
                .with(JwtSupport.account("creator-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Member promoted to administrator."))
        then:
        def jan = json.readTree(apiTeam("creator-1", teamId)).get("members").toList()
                .find { it.get("profileId").textValue() == profileId }
        jan.get("admin").booleanValue() == true
        and: "with two admins every action is eligible again — no disabled buttons"
        def page3 = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        !(page3 =~ /disabled="disabled"/).find()
        when: "demote returns the member to player"
        mvc.perform(post("/teams/" + teamId + "/members/" + profileId + "/demote")
                .with(JwtSupport.account("creator-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Member demoted to player."))
        then:
        json.readTree(apiTeam("creator-1", teamId)).get("members").toList()
                .find { it.get("profileId").textValue() == profileId }
                .get("admin").booleanValue() == false
    }

    // --- §13 pending-member UI ----------------------------------------------

    def "pending members wear the clock badge and pending pill until they claim"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def body = mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1"))
                .contentType("application/json")
                .content('{"fullName":"Jan Nowak","phone":"+48 600 000 000"}'))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
        def rawToken = json.readTree(body).get("claimToken").textValue()
        def janId = json.readTree(body).get("profileId").textValue()
        when: "the admin views the team page"
        def page = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        then:
        page.contains('class="pm-badge"')
        page.contains('class="pm-pill"')
        page.contains('New claim link')
        when: "Jan claims"
        mvc.perform(post("/api/claims").with(JwtSupport.account("claimer-7"))
                .contentType("application/json")
                .content('{"claimToken":"' + rawToken + '"}')).andExpect(status().isOk())
        def after = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        then: "the pending markers are gone for the claimed member"
        !after.contains('class="pm-badge"')
        !after.contains('class="pm-pill"')
        !after.contains('New claim link')
    }

    def "generating a claim link from the grid shows the in-tile minted card once"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def body = mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1"))
                .contentType("application/json")
                .content('{"fullName":"Jan Nowak","phone":"+48 600 000 000"}'))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
        def janId = json.readTree(body).get("profileId").textValue()
        def oldToken = json.readTree(body).get("claimToken").textValue()
        when: "the admin clicks New claim link on Jan's tile"
        def minted = mvc.perform(post("/teams/" + teamId + "/members/" + janId + "/claim-link")
                .with(JwtSupport.account("creator-1")).with(csrf()).param("source", "tile"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        def link = minted.flashMap.get("claimLink").toString()
        then: "the redirected page renders the small minted card INSIDE Jan's tile"
        def page = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1"))
                .session(minted.request.session))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains('class="pm-linkcard"')
        page.contains(link)
        page.contains('pm-linktile')
        and: "the OLD claim token is dead (regeneration replaced it)"
        mvc.perform(post("/api/claims").with(JwtSupport.account("claimer-8"))
                .contentType("application/json")
                .content('{"claimToken":"' + oldToken + '"}'))
                .andExpect(status().isNotFound())
    }

    def "the member-profile page shows the pending chip and the admins-only claim card"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def body = mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1"))
                .contentType("application/json")
                .content('{"fullName":"Jan Nowak","phone":"+48 600 000 000"}'))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
        def janId = json.readTree(body).get("profileId").textValue()
        when: "the admin views the pending member's profile"
        def page = mvc.perform(get("/teams/" + teamId + "/members/" + janId)
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("member"))
                .andReturn().response.contentAsString
        then:
        page.contains('class="pm-chip"')
        page.contains('not linked to an account yet')
        page.contains('New claim link')
        when: "the admin regenerates FROM the profile view"
        def minted = mvc.perform(post("/teams/" + teamId + "/members/" + janId + "/claim-link")
                .with(JwtSupport.account("creator-1")).with(csrf()).param("source", "member"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        then: "the FULL claim-link card renders on the team page (§13.3)"
        def teamPage = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1"))
                .session(minted.request.session))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        teamPage.contains('Claim link ready')
        teamPage.contains(minted.flashMap.get("claimLink").toString())
    }

    def "the pending member claims their profile through the RENDERED claim form"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def added = mvc.perform(post("/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1")).with(csrf())
                .param("fullName", "Dimitris Vlachos").param("phone", "+30 694 555 0142"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        def link = added.flashMap.get("claimLink").toString()
        def rawToken = link.substring(link.lastIndexOf('/claim/') + '/claim/'.length())
        def previewProfileId = json.readTree(apiTeam("creator-1", teamId)).get("members").toList()
                .find { it.get("fullName").textValue() == "Dimitris Vlachos" }
                .get("profileId").textValue()
        when: "the player opens the claim page and the RENDERED form action is used"
        def hero = mvc.perform(get("/teams/" + teamId + "/members/" + previewProfileId
                + "/claim/" + rawToken).with(JwtSupport.account("dimitris-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("claim"))
                .andReturn().response.contentAsString
        def matcher = java.util.regex.Pattern.compile('action="([^"]*/claim/' + rawToken + ')"')
        def formMatcher = matcher.matcher(hero)
        formMatcher.find()
        def formAction = formMatcher.group(1)
        then: "the rendered form points at the deeplink — posting IT claims"
        def claimed = mvc.perform(post(formAction)
                .with(JwtSupport.account("dimitris-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
        claimed.andReturn().response.redirectedUrl == "/players/me"
    }

    def "a claim deeplink whose PATH contradicts the token renders invalid-link"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def body = mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1"))
                .contentType("application/json")
                .content('{"fullName":"Jan Nowak","phone":"+48 600 000 000"}'))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
        def rawToken = json.readTree(body).get("claimToken").textValue()
        def janId = json.readTree(body).get("profileId").textValue()
        def otherId = java.util.UUID.randomUUID()
        expect: "both a wrong PROFILE and a wrong TEAM in the path are rejected"
        mvc.perform(get("/teams/" + teamId + "/members/" + otherId + "/claim/" + rawToken)
                .with(JwtSupport.account("member-1")))
                .andExpect(view().name("invalid-link"))
        mvc.perform(get("/teams/" + otherId + "/members/" + janId + "/claim/" + rawToken)
                .with(JwtSupport.account("member-1")))
                .andExpect(view().name("invalid-link"))
    }

    def "regenerating for a CLAIMED member or a non-team profile is honest"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def body = mvc.perform(post("/api/teams/" + teamId + "/members")
                .with(JwtSupport.account("creator-1"))
                .contentType("application/json")
                .content('{"fullName":"Jan Nowak","phone":"+48 600 000 000"}'))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
        def janId = json.readTree(body).get("profileId").textValue()
        mvc.perform(post("/api/claims").with(JwtSupport.account("claimer-9"))
                .contentType("application/json")
                .content('{"claimToken":"' + json.readTree(body).get("claimToken").textValue() + '"}'))
                .andExpect(status().isOk())
        expect: "a CLAIMED member has no pending link to regenerate"
        mvc.perform(post("/api/teams/" + teamId + "/members/" + janId + "/claim-link")
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.title').value("PROFILE_ALREADY_CLAIMED"))
        and: "a profile that is not a member of this team is not found"
        mvc.perform(post("/api/teams/" + teamId + "/members/" + java.util.UUID.randomUUID()
                + "/claim-link").with(JwtSupport.account("creator-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.title').value("MEMBER_NOT_FOUND"))
    }

    def "team pages are teammates-only: outsiders are redirected home"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        expect:
        def post = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("outsider")))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        post.response.redirectedUrl == "/"
    }

    def "admins can rename the team; the typed draft survives a failed save"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        and: "another team holds the target name (duplicate-name path)"
        teamIdOf("creator-2", "Name Clash")
        when: "the admin saves the header form (rename)"
        mvc.perform(post("/teams/" + teamId + "/details").with(JwtSupport.account("creator-1"))
                .with(csrf()).param("name", "Dynamo Reborn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Team updated."))
        then:
        def page = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains("Dynamo Reborn")
        when: "a duplicate name fails — the draft renders back"
        def failed = mvc.perform(post("/teams/" + teamId + "/details")
                .with(JwtSupport.account("creator-1")).with(csrf())
                .param("name", "Name Clash"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        then:
        def after = mvc.perform(get("/teams/" + teamId)
                .with(JwtSupport.account("creator-1"))
                .session(failed.request.session))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        after.contains('value="Name Clash"')
        after.contains("Dynamo Reborn") // the stored name is unchanged
    }

    def "editing the name AND picking a logo saves BOTH in one submit"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def logo = new MockMultipartFile("logo", "club.png", "image/png", [0x89, 0x50] as byte[])
        when: "the admin edits the name, picks a logo, hits Save once"
        mvc.perform(multipart("/teams/" + teamId + "/details").file(logo)
                .param("name", "Dynamo United")
                .with(JwtSupport.account("creator-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Team updated."))
        then:
        def api = json.readTree(apiTeam("creator-1", teamId))
        api.get("name").textValue() == "Dynamo United"
        api.get("logoPath").textValue().startsWith("/uploads/logo-")
    }

    def "an invalid logo fails the WHOLE header submit (name untouched)"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def notAnImage = new MockMultipartFile("logo", "evil.txt", "text/plain",
                "script".getBytes())
        when: "the submit carries a disallowed file type alongside a valid rename"
        mvc.perform(multipart("/teams/" + teamId + "/details").file(notAnImage)
                .param("name", "Should Not Apply")
                .with(JwtSupport.account("creator-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "Only JPEG, PNG or WebP images may be uploaded"))
        then:
        def api = json.readTree(apiTeam("creator-1", teamId))
        api.get("name").textValue() == "FC Dynamo"
        api.get("logoPath").isNull()
    }

    def "last admin removal surfaces the translated LAST_ADMIN flash"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def adminProfileId = memberIdsByApi("creator-1", teamId)[0]
        expect:
        mvc.perform(post("/teams/" + teamId + "/members/" + adminProfileId + "/remove")
                .with(JwtSupport.account("creator-1")).with(csrf())
                .locale(java.util.Locale.forLanguageTag("pl")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "Nie można usunąć ani zdegradować ostatniego administratora drużyny"))
    }

    def "a member whose BIRTHDAY is today wears the cake badge on the grid card"() {
        given: "a player whose birthday is the fixed clock's date (2026-09-01)"
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def body = registerPlayer("creator-1", teamId, "Jan Nowak")
        def janId = json.readTree(body).get("profileId").textValue()
        def janToken = json.readTree(body).get("claimToken").textValue()
        mvc.perform(post("/api/claims").with(JwtSupport.account("jan-owner"))
                .contentType("application/json").content('{"claimToken":"' + janToken + '"}'))
                .andExpect(status().isOk())
        mvc.perform(post("/players/me/profile").with(JwtSupport.account("jan-owner"))
                .with(csrf()).param("fullName", "Jan Nowak").param("phone", "+48 600 000 000")
                .param("birthDate", "1990-09-01").param("nationality", "PL"))
                .andExpect(status().is3xxRedirection())
        when: "the team page renders the grid"
        def page = mvc.perform(get("/teams/" + teamId).with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        then:
        page.contains('class="cake-badge"')
        page.contains("🎂")
    }

    def "logo upload via the team page form (admin)"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        def logo = new MockMultipartFile("logo", "club.png", "image/png", [0x89, 0x50] as byte[])
        when:
        mvc.perform(multipart("/teams/" + teamId + "/logo").file(logo)
                .with(JwtSupport.account("creator-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Team logo updated."))
        then:
        json.readTree(apiTeam("creator-1", teamId)).get("logoPath")
                .textValue().startsWith("/uploads/logo-")
    }

    def "plain member cannot upload the team logo (403 flash)"() {
        given:
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        joinTeam("member-1", teamId)
        def logo = new MockMultipartFile("logo", "club.png", "image/png", [0x89] as byte[])
        expect:
        mvc.perform(multipart("/teams/" + teamId + "/logo").file(logo)
                .with(JwtSupport.account("member-1")).with(csrf())
                .locale(java.util.Locale.forLanguageTag("pl")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "Tę akcję mogą wykonać tylko administratorzy drużyny"))
    }

    // --- my profile (mockup §6) -------------------------------------------

    def "my profile page: provisioned on first visit, full edit round-trips"() {
        when: "first visit provisions the acting account's profile"
        def page = mvc.perform(get("/players/me").with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn().response.contentAsString
        then:
        page.contains("Player") // auto-provisioned fallback name
        page.contains("member-1@example.com") // email mirrored read-only
        page.contains('id="nationality"') // the searchable country dropdown
        when: "the full profile form posts (name editable, phone required)"
        saveProfile("member-1", "Jan Nowak", "+48 600 000 000")
        then: "the saved values render back"
        def after = mvc.perform(get("/players/me").with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        after.contains("Jan Nowak")
        after.contains("+48 600 000 000")
        !after.contains('flash-warn') // the completeness callout is gone
    }

    def "my profile save surfaces the field bound as a translated flash"() {
        when:
        def post = mvc.perform(post("/players/me/profile").with(JwtSupport.account("member-1"))
                .with(csrf()).param("fullName", "Jan Nowak").param("phone", "600-000-000")
                .locale(java.util.Locale.forLanguageTag("pl")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "Wartość jednego z pól profilu jest nieprawidłowa"))
                .andReturn()
        then: "player input is ALWAYS preserved — the typed draft renders back"
        def page = mvc.perform(get("/players/me").with(JwtSupport.account("member-1"))
                .session(post.request.session))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains('value="Jan Nowak"')
        page.contains('value="600-000-000"')
    }

    def "my profile photo upload via the merged form keeps pending edits"() {
        given: "the account already has details saved"
        saveProfile("member-1", "Jan Nowak", "+48 600 000 000")
        def photo = new MockMultipartFile("photo", "me.png", "image/png", [0x89, 0x50] as byte[])
        when: "a photo is uploaded through the ONE form together with the form's fields"
        mvc.perform(multipart("/players/me/profile").file(photo)
                .param("fullName", "Jan Nowak").param("phone", "+48 600 000 000")
                .with(JwtSupport.account("member-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Profile saved."))
        then:
        def page = mvc.perform(get("/players/me").with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains("/uploads/photo-")
        page.contains("Jan Nowak")
    }

    def "submitting details and a new photo together applies BOTH (no lost edits)"() {
        given:
        saveProfile("member-1", "Old Name", "+48 600 000 000")
        def photo = new MockMultipartFile("photo", "me.png", "image/png", [0x89, 0x50] as byte[])
        when: "the user edits the name, picks a photo, and hits Save once"
        mvc.perform(multipart("/players/me/profile").file(photo)
                .param("fullName", "New Name").param("phone", "+48 600 000 000")
                .with(JwtSupport.account("member-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
        then:
        def page = mvc.perform(get("/players/me").with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains("New Name")
        page.contains("/uploads/photo-")
    }

    def "an invalid upload fails the WHOLE submit (details untouched)"() {
        given:
        saveProfile("member-1", "Jan Nowak", "+48 600 000 000")
        def notAnImage = new MockMultipartFile("photo", "evil.txt", "text/plain",
                "script".getBytes())
        when: "the submit carries a disallowed file type alongside valid fields"
        mvc.perform(multipart("/players/me/profile").file(notAnImage)
                .param("fullName", "Different Name").param("phone", "+48 600 000 000")
                .with(JwtSupport.account("member-1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "Only JPEG, PNG or WebP images may be uploaded"))
        then: "nothing was applied — the stored name is unchanged"
        def page = mvc.perform(get("/players/me").with(JwtSupport.account("member-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains("Jan Nowak")
        !page.contains("Different Name")
        !page.contains("/uploads/photo-")
    }

    // --- post-registration gate (mockup §7) --------------------------------

    def "incomplete profiles are redirected to my profile from gated pages"() {
        given: "a fresh account with no saved phone"
        def teamId = teamIdOf("creator-1", "FC Dynamo")
        expect: "the home page is gated"
        mvc.perform(get("/").with(JwtSupport.account("fresh-1")))
                .andExpect(status().is3xxRedirection())
        and: "the legacy open link is dead for everyone (invalid page, not gated)"
        mvc.perform(get("/teams/" + teamId + "/join").with(JwtSupport.account("fresh-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("invalid-link"))
        and: "after saving the phone, the gate lifts"
        saveProfile("fresh-1", "Fresh One", "+48 900 000 009")
        mvc.perform(get("/").with(JwtSupport.account("fresh-1")))
                .andExpect(status().isOk())
    }
}
