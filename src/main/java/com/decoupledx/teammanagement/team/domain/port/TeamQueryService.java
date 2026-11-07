package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.TeamInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import java.util.List;
import java.util.UUID;

/**
 * Team reads. Contact details of members are only exposed to acting team
 * administrators (privacy rule — see §9 of the plan); other viewers receive
 * the name/photo shape only. "me" variants throughout (ADR-0002).
 */
public interface TeamQueryService {

    TeamWithMembersInfo get(UUID teamId);

    List<TeamInfo> myTeams();
}
