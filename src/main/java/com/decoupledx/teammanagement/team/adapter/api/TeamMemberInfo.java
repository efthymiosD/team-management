package com.decoupledx.teammanagement.team.adapter.api;

import java.util.UUID;

/**
 * A team's member as seen by the API: the membership link plus the profile's
 * presentation fields. The claimed flag tells the UI whether the profile is
 * linked to an account; unclaimed profiles expose no account id.
 *
 * <p>Privacy rule (enforced server-side by the team module): phone is only
 * populated for the acting team administrators and the member's own row;
 * birthday is visible to all teammates (owner decision 2026-11-14).
 * viewerIsAdmin reflects the ACTING viewer's admin state for the team —
 * the web layer gates admin affordances on it.</p>
 */
public record TeamMemberInfo(UUID profileId, String fullName, String phone, String birthDate,
        String nationality, String photoPath, boolean admin, boolean claimed, boolean mine,
        boolean viewerIsAdmin) {
}
