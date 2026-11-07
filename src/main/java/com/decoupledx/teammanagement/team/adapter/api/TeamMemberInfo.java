package com.decoupledx.teammanagement.team.adapter.api;

import java.util.UUID;

/**
 * A team's member as seen by the API: the membership link plus the profile's
 * presentation fields. The claimed flag tells the UI whether the profile is
 * linked to an account; unclaimed profiles expose no account id.
 */
public record TeamMemberInfo(UUID profileId, String fullName, String contactDetails, String nationality,
        String photoPath, boolean admin, boolean claimed) {
}
