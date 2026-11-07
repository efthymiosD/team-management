package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import java.util.UUID;

/**
 * Registration flows. Self-registration joins the acting account's player
 * profile as a PLAYER. Admin registration creates an UNCLAIMED person (a
 * player profile without an account, with a claim token) and links it to the
 * team — the person later claims the profile to bind their account.
 */
public interface TeamRegistrationService {

    TeamMemberInfo join(UUID teamId);

    PlayerProfileInfo registerPlayer(UUID teamId, String fullName, String contactDetails, String nationality);
}
