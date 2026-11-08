package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;

/**
 * Profile claiming (decision 4 of the bootstrap report): an admin creates an
 * unclaimed player profile with a claim token; the person later binds THEIR
 * account by presenting the token in an authenticated request. Single use:
 * after a successful claim the token is cleared (and the unique
 * claim_token/account columns make any race a 409 — the DB is the truth).
 * One profile per account: an account that already has a player profile
 * (e.g. auto-provisioned by joining) cannot claim another one (409).
 */
public interface ProfileClaimService {

    PlayerProfileInfo claim(String claimToken);
}
