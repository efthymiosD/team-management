package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Registration flows. Self-registration joins the acting account's player
 * profile as a PLAYER. Admin registration creates an UNCLAIMED person (a
 * player profile without an account, with a claim token) and links it to the
 * team — the person later claims the profile to bind their account.
 */
public interface TeamRegistrationService {

    /**
     * The acting account's own player profile (provisioned on first use),
     * as the API view — the "my profile" page reads it here because profile
     * provisioning is this port's seam.
     */
    PlayerProfileInfo myProfile();

    /**
     * Whether the acting account's self-service required info is complete
     * (a saved phone number). The completeness gate reads this instead of
     * the full view — no email lookup, no view mapping.
     */
    boolean myProfileIsComplete();

    PlayerProfileInfo registerPlayer(UUID teamId, String fullName, String phone,
            LocalDate birthDate, String nationality);
}
