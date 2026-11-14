package com.decoupledx.teammanagement.webui;

import jakarta.annotation.Nullable;

/**
 * Presentation-only view of the signed-in user for the navigation bar.
 * displayName is the human-friendly claim (preferred_username) when available,
 * falling back to the opaque sub; accountId carries the internal identity
 * (provisioned on first team action); administrator comes from the session
 * authorities (ROLE_ADMIN), never from client-side input.
 */
record CurrentUser(
        boolean authenticated,
        String displayName,
        @Nullable String accountId,
        boolean administrator) {

    public static final CurrentUser ANONYMOUS = new CurrentUser(false, null, null, false);

    public static CurrentUser signedInAs(String displayName) {
        return new CurrentUser(true, displayName, null, false);
    }

    public static CurrentUser administrator(String displayName) {
        return new CurrentUser(true, displayName, null, true);
    }
}
