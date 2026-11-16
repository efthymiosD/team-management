package com.decoupledx.teammanagement.webui;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.WebUtils;

/**
 * The manual theme switch (approved mockup §11): the choice persists in a
 * server-read "theme" cookie so the first paint already carries
 * data-theme="light|dark" on &lt;html&gt; — no wrong-theme flash, correct
 * without JS. AUTO renders no attribute: the prefers-color-scheme media
 * query stays the untouchable fallback. The JS enhancement swaps the
 * attribute + cookie in place (no reload); this POST is the no-JS path.
 */
@Controller
class ThemeController {

    static final String COOKIE_NAME = "theme";
    static final String AUTO = "auto";
    static final String LIGHT = "light";
    static final String DARK = "dark";
    static final Set<String> VALID = Set.of(AUTO, LIGHT, DARK);

    private static final int COOKIE_MAX_AGE_SECONDS =
            Math.toIntExact(Duration.ofDays(365).toSeconds());

    @PostMapping("/theme")
    String choose(@RequestParam(name = "style", defaultValue = AUTO) String style,
            HttpServletRequest request, HttpServletResponse response) {
        if (!VALID.contains(style)) {
            return redirectTo(request);
        }
        Cookie cookie = new Cookie(ThemeController.COOKIE_NAME, style);
        cookie.setPath("/");
        cookie.setMaxAge(AUTO.equals(style) ? 0 : COOKIE_MAX_AGE_SECONDS);
        cookie.setHttpOnly(false);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return redirectTo(request);
    }

    /** Back to the page the switch was pressed on (same-origin path only). */
    private static String redirectTo(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                java.net.URI uri = java.net.URI.create(referer);
                if (uri.getPath() != null && uri.getPath().startsWith("/")) {
                    return "redirect:" + uri.getPath()
                            + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
                }
            } catch (IllegalArgumentException malformedReferer) {
            }
        }
        return "redirect:/";
    }

    /** The acting request's theme choice (null = AUTO — media query decides). */
    static String currentTheme(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, ThemeController.COOKIE_NAME);
        if (cookie == null || !VALID.contains(cookie.getValue())
                || AUTO.equals(cookie.getValue())) {
            return null;
        }
        return cookie.getValue();
    }
}
