package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import java.util.UUID;

/**
 * Team-profile changes, admin-only. "me" variant: the acting admin is
 * resolved inside (ADR-0002).
 */
public interface TeamProfileManagementService {

    TeamWithMembersInfo rename(UUID teamId, String name);
}
