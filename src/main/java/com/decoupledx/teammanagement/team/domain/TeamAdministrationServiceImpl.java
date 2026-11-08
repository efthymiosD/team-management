package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamAdministrationService;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TeamAdministrationServiceImpl implements TeamAdministrationService {

    private final TeamPermissions permissions;
    private final PlayerProfileRepository profiles;
    private final TeamMembershipRepository memberships;
    private final TransactionRunner tx;

    @Override
    public TeamMemberInfo promote(UUID teamId, UUID profileId) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            TeamMembershipDataValue membership = membership(teamId, profileId);
            // Idempotent: promoting a member who is already an admin is a
            // no-op that returns the current state.
            if ("PLAYER".equals(membership.role())) {
                TeamMembership changed = TeamMembership.from(membership);
                changed.promote();
                membership = memberships.save(changed.toData());
            }
            // The profile FK (team_memberships.profile_id → player_profiles)
            // guarantees the row exists — no speculative 404 surface here.
            return TeamRegistrationServiceImpl.memberInfo(membership, profiles.find(profileId));
        });
    }

    @Override
    public TeamMemberInfo demote(UUID teamId, UUID profileId) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            TeamMembershipDataValue membership = membership(teamId, profileId);
            if ("ADMIN".equals(membership.role()) && memberships.lockAdmins(teamId).size() <= 1) {
                // PESSIMISTIC_WRITE on the team's ADMIN rows serializes the
                // concurrent last-admin race: the loser re-reads the shrunken
                // count and rejects.
                throw new BusinessException(ErrorCode.LAST_ADMIN);
            }
            TeamMembership changed = TeamMembership.from(membership);
            changed.demote();
            TeamMembershipDataValue saved = memberships.save(changed.toData());
            return TeamRegistrationServiceImpl.memberInfo(saved, profiles.find(profileId));
        });
    }

    @Override
    public void removeMember(UUID teamId, UUID profileId) {
        tx.run(() -> {
            permissions.requireAdmin(teamId);
            TeamMembershipDataValue membership = membership(teamId, profileId);
            if ("ADMIN".equals(membership.role()) && memberships.lockAdmins(teamId).size() <= 1) {
                throw new BusinessException(ErrorCode.LAST_ADMIN);
            }
            memberships.delete(membership);
            return null;
        });
    }

    private TeamMembershipDataValue membership(UUID teamId, UUID profileId) {
        TeamMembershipDataValue membership = memberships.find(teamId, profileId);
        if (membership == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return membership;
    }
}
