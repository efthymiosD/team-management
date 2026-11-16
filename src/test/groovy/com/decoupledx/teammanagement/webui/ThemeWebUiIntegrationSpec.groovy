package com.decoupledx.teammanagement.webui

import com.decoupledx.teammanagement.testinfra.PostgresIntegrationSpec
import org.hamcrest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

/**
 * The manual theme switch (approved mockup §11, black-box): the "theme"
 * cookie decides the server-rendered data-theme attribute on <html> —
 * first paint is correct with no JS; AUTO (the default) renders NO
 * attribute so prefers-color-scheme stays the fallback; out-of-set values
 * are silently dropped.
 */
@AutoConfigureMockMvc
class ThemeWebUiIntegrationSpec extends PostgresIntegrationSpec {

    @Autowired
    MockMvc mvc

    def cleanup() {
        truncate("team_invites", "team_memberships", "player_profiles", "teams")
    }

    private String home() {
        // Anonymous home is permitAll — the switch works logged-out too.
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andReturn().response.contentAsString
    }

    def "default is AUTO: no data-theme attribute, the media query decides"() {
        expect:
        def page = home()
        !page.contains('data-theme="light"')
        !page.contains('data-theme="dark"')
        page.contains('class="ts-theme"')
        page.contains('aria-checked="true"') // the Auto segment is selected
    }

    def "choosing dark persists via the cookie and renders data-theme=dark"() {
        when: "the no-JS form POST selects Dark"
        def chosen = mvc.perform(post("/theme").with(csrf()).param("style", "dark")
                .header("Referer", "http://localhost/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(cookie().value("theme", "dark"))
                .andExpect(cookie().maxAge("theme", 31536000))
                .andReturn()
        then: "the next request (carrying the cookie) paints dark server-side"
        def page = mvc.perform(get("/").cookie(chosen.response.getCookie("theme")))
                .andExpect(status().isOk())
                .andReturn().response.contentAsString
        page.contains('data-theme="dark"')
        !page.contains('data-theme="light"')
    }

    def "choosing light persists and AUTO clears the override"() {
        when:
        def chosen = mvc.perform(post("/theme").with(csrf()).param("style", "light"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
        then:
        mvc.perform(get("/").cookie(chosen.response.getCookie("theme")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers
                        .containsString('data-theme="light"')))
        when: "back to Auto deletes the cookie — the media query decides again"
        def cleared = mvc.perform(post("/theme").with(csrf()).param("style", "auto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(cookie().maxAge("theme", 0))
                .andReturn()
        then:
        cleared.response.getCookie("theme").getMaxAge() == 0
        def page = home()
        !page.contains('data-theme="light"')
        !page.contains('data-theme="dark"')
    }

    def "out-of-set values are silently dropped (cookie untouched)"() {
        when:
        mvc.perform(post("/theme").with(csrf()).param("style", "neon")
                .header("Referer", "http://localhost/"))
                .andExpect(status().is3xxRedirection())
        then: "no theme cookie is issued and no attribute renders"
        def result = mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.allOf(
                        Matchers.not(Matchers
                                .containsString('data-theme="neon"')),
                        Matchers.not(Matchers
                                .containsString('data-theme="dark"')))))
        result.andReturn().response.getCookie("theme") == null
    }
}
