package com.decoupledx.teammanagement.identity.adapter.web;

import com.decoupledx.teammanagement.identity.adapter.api.AccountId;
import com.decoupledx.teammanagement.identity.adapter.api.CurrentAccountApi;
import com.decoupledx.teammanagement.identity.domain.port.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Maps the authenticated principal's IdP subject to the app-owned internal
 * {@link AccountId}. Accepts both principal types: {@link JwtAuthenticationToken}
 * (REST API bearer tokens) and an {@link OidcUser} principal inside an
 * OAuth2 session token (browser sessions via oauth2Login, where the user-name
 * attribute is configured as 'sub'). There is deliberately no preferred_username
 * fallback.
 */
@Service
@RequiredArgsConstructor
public class CurrentAccountResolver implements CurrentAccountApi {

    private final AccountService accounts;

    public AccountId currentAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return resolve(jwt.getName(), jwt.getToken().getClaimAsString("preferred_username"),
                    "JWT is missing the 'sub' claim required for account identity");
        }
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return resolve(oidcUser.getName(), oidcUser.getPreferredUsername(),
                    "OIDC principal is missing the 'sub' claim required for account identity");
        }
        throw new IllegalStateException("No authenticated account principal present");
    }

    @Override
    public boolean isAdministrator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private AccountId resolve(String subject, String displayName, String missingSubjectMessage) {
        if (subject != null && !subject.isBlank()) {
            return accounts.resolveOrProvision(subject, displayName);
        }
        throw new IllegalStateException(missingSubjectMessage);
    }
}
