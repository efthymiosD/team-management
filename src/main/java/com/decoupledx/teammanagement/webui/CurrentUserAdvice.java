package com.decoupledx.teammanagement.webui;

import com.decoupledx.teammanagement.identity.adapter.api.CurrentAccountApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the signed-in user (display name, account id) and the resolved
 * locale language to every web UI view so templates stay free of security
 * dialects and controllers stay pure delegation. The ONLY presentation
 * consumer of {@link CurrentAccountApi} (ADR-0002).
 */
@ControllerAdvice
@RequiredArgsConstructor
class CurrentUserAdvice {

    private final CurrentAccountApi accounts;

    @ModelAttribute("currentUser")
    CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return CurrentUser.ANONYMOUS;
        }
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return new CurrentUser(true, displayNameOf(authentication),
                accountIdOrNull(), administrator);
    }

    @ModelAttribute("locale")
    String locale() {
        return org.springframework.context.i18n.LocaleContextHolder
                .getLocale().getLanguage();
    }

    private String accountIdOrNull() {
        try {
            return accounts.currentAccountId().value();
        } catch (RuntimeException unresolved) {
            return null;
        }
    }

    private String displayNameOf(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String preferredUsername = oidcUser.getPreferredUsername();
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
            return oidcUser.getName();
        }
        return authentication.getName();
    }
}
