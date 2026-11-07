package com.decoupledx.teammanagement.team.adapter.api;

import java.util.List;
import java.util.UUID;

/**
 * A team as seen by the API: identity, profile fields, logo web path, and the
 * members (implicitly ordered: administrators, then players by join order).
 */
public record TeamWithMembersInfo(UUID teamId, String name, String logoPath,
        List<TeamMemberInfo> members) {
}
