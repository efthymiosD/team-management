package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;

/**
 * The acting account manages ITS OWN player profile's media. "me" variant:
 * the profile is resolved inside (ADR-0002) and provisioned on first use.
 */
public interface PlayerProfilePhotoService {

    PlayerProfileInfo applyMyPhoto(String photoPath);
}
