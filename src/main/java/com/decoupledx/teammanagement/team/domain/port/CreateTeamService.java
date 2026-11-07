package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;

/**
 * Creates a team; the creator becomes the first administrator (their player
 * profile is provisioned on first use). "me" variant — the acting account is
 * resolved inside (ADR-0002).
 */
public interface CreateTeamService {

    TeamWithMembersInfo create(String name);
}
