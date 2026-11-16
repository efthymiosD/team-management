package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.ClaimPreviewInfo;
import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import java.util.UUID;

/**
 * Profile claiming (decision 4 of the bootstrap report): an admin creates an
 * unclaimed player profile with a claim token; the person later binds THEIR
 * account by presenting the token through the claim page. Single use — after
 * a successful claim the token is cleared, and the unique claim-token/account
 * columns make any race an honest 409 (the DB is the truth). An account that
 * already has a profile MERGES instead: their own profile wins, the
 * placeholder is hard-deleted (owner decision 2026-11-15).
 */
public interface ProfileClaimService {

    /** The claim page's preview: the pending profile + its team identity. */
    ClaimPreviewInfo preview(String claimToken);

    PlayerProfileInfo claim(String claimToken);

    /**
     * Admin-only: mints a FRESH claim token for the given pending member —
     * replacing the previous one (one active link per pending profile).
     * Returns the RAW token (the only exposure; storage keeps hashes).
     */
    PlayerProfileInfo regenerateClaimLink(UUID teamId, UUID profileId);
}
