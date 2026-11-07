package com.decoupledx.teammanagement.team.adapter.api;

import java.util.UUID;

public record PlayerProfileInfo(UUID profileId, UUID accountId, String fullName, String contactDetails,
        String nationality, String photoPath, String claimToken) {
}
