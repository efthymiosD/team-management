package com.decoupledx.teammanagement.team.adapter.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeamWithMembersInfo(UUID teamId, String name, String logoPath, Instant createdAt,
        boolean viewerIsAdmin, List<TeamMemberInfo> members) {
}
