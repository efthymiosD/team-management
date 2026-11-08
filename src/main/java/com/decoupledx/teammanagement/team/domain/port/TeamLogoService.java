package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import java.util.UUID;

/**
 * Team-profile media changes, admin-only. "me" variant: the acting admin is
 * resolved inside (ADR-0002). The web path points into /uploads/**.
 */
public interface TeamLogoService {

    TeamWithMembersInfo applyLogo(UUID teamId, String logoPath);
}
