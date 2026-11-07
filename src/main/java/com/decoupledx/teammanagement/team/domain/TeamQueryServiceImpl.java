package com.decoupledx.teammanagement.team.domain;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.TeamInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
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

    private final TeamPermissions permissions;
    private final TeamRepository teams;
    private final TeamMembershipRepository memberships;
    private final PlayerProfileRepository profiles;

    @Override
    public TeamWithMembersInfo get(UUID teamId) {
        TeamDataValue team = teams.find(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
        List<TeamMembershipDataValue> members = memberships.findByTeam(teamId);
        // Batch the profile lookups — never one query per member (AGENTS.md
        // performance checklist).
        Map<UUID, PlayerProfileDataValue> profilesById = profiles.findAllByIds(
                members.stream().map(TeamMembershipDataValue::profileId).toList()).stream()
                .collect(Collectors.toMap(PlayerProfileDataValue::id, Function.identity()));

        UUID viewerProfileId = permissions.viewerProfileIdOrNull();
        boolean viewerAdmin = members.stream()
                .anyMatch(membership -> membership.profileId().equals(viewerProfileId)
                        && "ADMIN".equals(membership.role()));

        // Privacy rule: a member always sees their own contact details; team
        // admins see everyone's. Other viewers get names/photos only.
        List<TeamMemberInfo> memberInfos = members.stream()
                .sorted(Comparator
                        .comparing((TeamMembershipDataValue m) -> "ADMIN".equals(m.role()) ? 0 : 1)
                        .thenComparing(TeamMembershipDataValue::joinedAt))
                .map(membership -> {
                    PlayerProfileDataValue profile = profilesById.get(membership.profileId());
                    boolean own = membership.profileId().equals(viewerProfileId);
                    String contactDetails = viewerAdmin || own ? profile.contactDetails() : null;
                    return new TeamMemberInfo(profile.id(), profile.fullName(), contactDetails,
                            profile.nationality(), profile.photoPath(), "ADMIN".equals(membership.role()),
                            profile.accountId() != null);
                })
                .toList();
        return new TeamWithMembersInfo(team.id(), team.name(), team.logoPath(), memberInfos);
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
                .filter(team -> team != null) // team deleted while we read
                .map(team -> new TeamInfo(team.id(), team.name(), team.logoPath()))
                .toList();
    }
}
