package com.decoupledx.teammanagement.webui;

import com.decoupledx.teammanagement.team.adapter.api.TeamApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * The post-registration flow (owner feedback #6, approved mockup §7): a
 * signed-in user whose profile lacks the required info (a saved phone
 * number) is redirected to "My profile" before using the rest of the UI —
 * registration lands on the profile to complete it first.
 *
 * <p>EXEMPT: the profile page itself (and its POSTs), the whole team
 * surface (/teams/**) — so a player who registered FROM a join link still
 * lands on their new team's page and can join without being bounced — plus
 * static assets and sign-out. Navigating anywhere else (home, …) catches
 * the incomplete profile. REST API paths are exempt: the gate is a UI
 * concern.</p>
 */
@Component
@RequiredArgsConstructor
class ProfileCompletionGate implements HandlerInterceptor {

    private final TeamApi teamApi;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }
        if (isExempt(request.getRequestURI())) {
            return true;
        }
        if (!teamApi.myProfileIsComplete()) {
            response.sendRedirect("/players/me");
            return false;
        }
        return true;
    }

    private boolean isExempt(String path) {
        return path.equals("/players/me")
                || path.startsWith("/players/me/")
                || path.startsWith("/teams/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/uploads/")
                || path.startsWith("/login/")
                || path.equals("/favicon.ico")
                || path.equals("/error")
                || path.equals("/theme")
                || path.equals("/logout")
                || path.equals("/logged-out");
    }
}
