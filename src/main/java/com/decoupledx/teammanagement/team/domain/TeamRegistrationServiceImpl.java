package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.domain.port.CurrentPlayerProfile;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamRegistrationService;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TeamRegistrationServiceImpl implements TeamRegistrationService {

    private final TeamPermissions permissions;
    private final CurrentPlayerProfile currentPlayer;
    private final TeamRepository teams;
    private final TeamMembershipRepository memberships;
    private final PlayerProfileRepository profiles;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public TeamMemberInfo join(UUID teamId) {
        // Cheap read first: a doomed request must not provision a profile.
        requireTeam(teamId);
        // Provisioning outside the team tx — see CreateTeamServiceImpl.create().
        PlayerProfileDataValue profile = currentPlayer.current();
        return tx.run(() -> {
            TeamMembershipDataValue existing = memberships.find(teamId, profile.id());
            if (existing != null) {
                throw new BusinessException(ErrorCode.ALREADY_MEMBER);
            }
            TeamMembershipDataValue membership = memberships.save(TeamMembership
                    .create(teamId, profile.id(), TeamMembershipRole.PLAYER, clock.instant()).toData());
            return memberInfo(membership, profile);
        });
    }

    @Override
    public PlayerProfileInfo registerPlayer(UUID teamId, String fullName, String contactDetails,
            String nationality) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            PlayerProfile profile = PlayerProfile.unclaimed(fullName, contactDetails, nationality,
                    java.util.UUID.randomUUID().toString(), clock.instant());
            PlayerProfileDataValue savedProfile = profiles.save(profile.toData());
            memberships.save(TeamMembership
                    .create(teamId, savedProfile.id(), TeamMembershipRole.PLAYER, clock.instant())
                    .toData());
            return profileInfo(savedProfile);
        });
    }

    private void requireTeam(UUID teamId) {
        TeamDataValue team = teams.find(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
    }

    static TeamMemberInfo memberInfo(TeamMembershipDataValue membership, PlayerProfileDataValue profile) {
        return new TeamMemberInfo(profile.id(), profile.fullName(), profile.contactDetails(),
                profile.nationality(), profile.photoPath(), "ADMIN".equals(membership.role()),
                profile.accountId() != null);
    }

    static PlayerProfileInfo profileInfo(PlayerProfileDataValue profile) {
        return new PlayerProfileInfo(profile.id(), profile.accountId(), profile.fullName(),
                profile.contactDetails(), profile.nationality(), profile.photoPath(),
                profile.claimToken());
    }
}
