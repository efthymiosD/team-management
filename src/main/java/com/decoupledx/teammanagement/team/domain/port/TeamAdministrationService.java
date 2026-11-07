package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import java.util.UUID;

/**
 * Administrator-side membership operations, all admin-only. The
 * at-least-one-admin invariant blocks removing or demoting the last admin;
 * promote/demote of a member already in the target role are idempotent
 * no-ops.
 */
public interface TeamAdministrationService {

    TeamMemberInfo promote(UUID teamId, UUID profileId);

    TeamMemberInfo demote(UUID teamId, UUID profileId);

    void removeMember(UUID teamId, UUID profileId);
}
