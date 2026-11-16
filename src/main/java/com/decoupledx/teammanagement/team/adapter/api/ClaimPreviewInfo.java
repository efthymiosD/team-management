package com.decoupledx.teammanagement.team.adapter.api;

import java.util.UUID;

/**
 * The claim page's preview: the team identity plus the identity hint the
 * admin entered (name + phone). E-mail comes from the account the claimer
 * signs in with — never asked here. profileId lets the deeplink validate
 * that the path points at the token's own profile.
 */
public record ClaimPreviewInfo(UUID profileId, UUID teamId, String teamName, String teamLogoPath,
        String fullName, String phone) {
}
