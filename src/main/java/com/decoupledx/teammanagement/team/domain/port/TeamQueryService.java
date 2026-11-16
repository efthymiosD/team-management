package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.TeamInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import java.util.List;
import java.util.UUID;

/**
 * Team reads. The phone number of members is only exposed to acting team
 * administrators (privacy rule); birthdays are visible to all teammates.
 * Member profile pages are a teammates-only surface. "me" variants
 * throughout (ADR-0002).
 */
public interface TeamQueryService {

    TeamWithMembersInfo get(UUID teamId);

    /**
     * The join page's identity hero (name + logo, no roster): readable by
     * any authenticated visitor — the one team view that is NOT
     * membership-gated, so a non-member can decide to join.
     */
    TeamInfo joinPreview(UUID teamId);

    /** One member's profile in a team context (the member-profile page). */
    TeamMemberInfo memberProfile(UUID teamId, UUID profileId);

    List<TeamInfo> myTeams();
}
