package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.domain.PlayerProfileDataValue;

/**
 * Seam between the account identity and a player profile: every account gets
 * one player profile on first team action, named after the account's captured
 * IdP display name. Persistence races are tolerated via the profile/account
 * unique index (the 409 translation lives in the adapter).
 */
public interface CurrentPlayerProfile {

    /**
     * The current viewer's player profile, created on first use if needed.
     */
    PlayerProfileDataValue current();
}
