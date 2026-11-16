package com.decoupledx.teammanagement.webui

import com.decoupledx.teammanagement.testinfra.JwtSupport
import com.decoupledx.teammanagement.testinfra.PostgresIntegrationSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

/**
 * Web UI home page (black-box over real PostgreSQL): anonymous login CTA,
 * authenticated "my teams" list, create-team form through the team module
 * API (flash attributes), and the value-free i18n contract (html lang
 * follows ?lang= for shipped locales only, out-of-set values silently fall
 * back, no unresolved-message markers, Polish copy for the Polish locale).
 */
@AutoConfigureMockMvc
class HomeWebUiIntegrationSpec extends PostgresIntegrationSpec {

    @Autowired
    MockMvc mvc

    def cleanup() {
        truncate("team_invites", "team_memberships", "player_profiles", "teams")
    }

    /**
     * The profile-completeness gate (post-registration flow) redirects
     * incomplete profiles away from non-team pages — every authenticated
     * scenario completes the required info first.
     */
    void completeProfile(String account) {
        mvc.perform(post("/players/me/profile").with(JwtSupport.account(account))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("fullName", "Player")
                .param("phone", "+48 600 000 000"))
                .andExpect(status().is3xxRedirection())
    }

    def "anonymous home shows the login call to action"() {
        expect:
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("currentUser"))
                .andReturn().response.contentAsString.contains("oauth2/authorization/keycloak")
    }

    def "authenticated home lists the member's teams"() {
        given:
        completeProfile("creator-1")
        mvc.perform(post("/api/teams").with(JwtSupport.account("creator-1"))
                .contentType("application/json")
                .content('{"name":"FC Dynamo"}')).andExpect(status().isCreated())
        when:
        def page = mvc.perform(get("/").with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andReturn().response.contentAsString
        then:
        page.contains("FC Dynamo")
        !page.contains("??")
    }

    def "create-team form posts through the team module API (flash message on the redirect)"() {
        given:
        completeProfile("creator-1")
        when:
        mvc.perform(post("/teams").with(JwtSupport.account("creator-1"))
                .param("name", "Form United"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("message"))
        then:
        def page = mvc.perform(get("/").with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
        and: "the created team shows up in the listing"
        page.andReturn().response.contentAsString.contains("Form United")
    }

    def "duplicate team name surfaces the translated business error (PL flash copy)"() {
        given:
        completeProfile("creator-1")
        mvc.perform(post("/teams").with(JwtSupport.account("creator-1"))
                .param("name", "Form United"))
        and: "player input is always preserved — the typed name renders back"
        def post = mvc.perform(post("/teams").with(JwtSupport.account("creator-1"))
                .param("name", "Form United")
                .locale(java.util.Locale.forLanguageTag("pl")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "Drużyna o tej nazwie już istnieje"))
                .andReturn()
        // Flash maps travel via the HTTP session (like the real browser's
        // cookie) — MockMvc must carry the same session across the redirect.
        def page = mvc.perform(get("/").with(JwtSupport.account("creator-1"))
                .session(post.request.session))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains('value="Form United"')
    }

    def "home honors the ?lang= switch (Polish locale contract)"() {
        given:
        completeProfile("creator-1")
        expect:
        def page = mvc.perform(get("/").queryParam("lang", "pl")
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains('lang="pl"')
        page.contains("Moje drużyny")
        !page.contains("??")
    }

    def "out-of-set locales are silently dropped (default language, no lang=xx leak)"() {
        given:
        completeProfile("creator-1")
        expect:
        def page = mvc.perform(get("/").queryParam("lang", "xx")
                .with(JwtSupport.account("creator-1")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains('lang="en"')
        !page.contains('lang="xx"')
        !page.contains("??")
    }

    def "logged-out page renders (permitAll)"() {
        expect:
        mvc.perform(get("/logged-out"))
                .andExpect(status().isOk())
                .andExpect(view().name("logged-out"))
    }

    def "static stylesheet is served without authentication"() {
        expect:
        mvc.perform(get("/css/team.css")).andExpect(status().isOk())
    }
}
