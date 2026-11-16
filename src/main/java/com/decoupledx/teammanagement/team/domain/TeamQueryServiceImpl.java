package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.common.error.BusinessException;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.TeamInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TeamQueryServiceImpl implements TeamQueryService {

    private final TeamPermissionsService permissions;
    private final TeamRepository teams;
    private final TeamMembershipRepository memberships;
    private final PlayerProfileRepository profiles;
    private final TeamViewsService views;

    @Override
    public TeamWithMembersInfo get(UUID teamId) {
        TeamDataValue team = teams.find(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
        List<TeamMembershipDataValue> members = memberships.findByTeam(teamId);
        // Membership gate (owner feedback 2026-11-15): team pages are a
        // teammates-only surface — a non-member (including a removed former
        // member) gets the honest 403, never the roster.
        UUID viewerProfileId = permissions.viewerProfileIdOrNull();
        boolean viewerMember = viewerProfileId != null && members.stream()
                .anyMatch(membership -> membership.profileId().equals(viewerProfileId));
        if (!viewerMember) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }
        // Batch the profile lookups — never one query per member (AGENTS.md
        Map<UUID, PlayerProfileDataValue> profilesById = profiles.findAllByIds(
                members.stream().map(TeamMembershipDataValue::profileId).toList()).stream()
                .collect(Collectors.toMap(PlayerProfileDataValue::id, Function.identity()));
        boolean viewerAdmin = isAdmin(members, viewerProfileId);

        // only. The birthday is visible to all teammates (owner decision).
        List<TeamMemberInfo> memberInfos = members.stream()
                .sorted(Comparator
                        .comparing((TeamMembershipDataValue m) -> TeamMembershipRole.ADMIN.name().equals(m.role()) ? 0 : 1)
                        .thenComparing(TeamMembershipDataValue::joinedAt))
                .map(membership -> views.memberInfo(membership,
                        profilesById.get(membership.profileId()), viewerProfileId, viewerAdmin))
                .toList();
        return new TeamWithMembersInfo(team.id(), team.name(), team.logoPath(), team.createdAt(),
                viewerAdmin, memberInfos);
    }

    @Override
    public TeamInfo joinPreview(UUID teamId) {
        TeamDataValue team = teams.find(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
        return new TeamInfo(team.id(), team.name(), team.logoPath());
    }

    @Override
    public TeamMemberInfo memberProfile(UUID teamId, UUID profileId) {        TeamDataValue team = teams.find(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
        List<TeamMembershipDataValue> members = memberships.findByTeam(teamId);
        UUID viewerProfileId = permissions.viewerProfileIdOrNull();
        if (viewerProfileId == null || members.stream()
                .noneMatch(membership -> membership.profileId().equals(viewerProfileId))) {
            // Member profiles are a teammates-only surface (owner decision).
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }
        TeamMembershipDataValue membership = members.stream()
                .filter(candidate -> candidate.profileId().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        PlayerProfileDataValue profile = profiles.find(profileId);
        return views.memberInfo(membership, profile, viewerProfileId,
                isAdmin(members, viewerProfileId));
    }

    @Override
    public List<TeamInfo> myTeams() {
        UUID viewerProfileId = permissions.viewerProfileIdOrNull();
        if (viewerProfileId == null) {
            return List.of();
        }
        List<TeamMembershipDataValue> myMemberships = memberships.findByProfile(viewerProfileId);
        Map<UUID, TeamDataValue> teamsById = teams
                .findAllByIds(myMemberships.stream().map(TeamMembershipDataValue::teamId).toList())
                .stream()
                .collect(Collectors.toMap(TeamDataValue::id, Function.identity()));
        return myMemberships.stream()
                .map(membership -> teamsById.get(membership.teamId()))
                .filter(team -> team != null)
                .map(team -> new TeamInfo(team.id(), team.name(), team.logoPath()))
                .toList();
    }

    private static boolean isAdmin(List<TeamMembershipDataValue> members, UUID viewerProfileId) {
        return viewerProfileId != null && members.stream()
                .anyMatch(membership -> membership.profileId().equals(viewerProfileId)
                        && TeamMembershipRole.ADMIN.name().equals(membership.role()));
    }
}
