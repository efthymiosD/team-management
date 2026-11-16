package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamAdministrationService;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TeamAdministrationServiceImpl implements TeamAdministrationService {

    private final TeamPermissionsService permissions;
    private final PlayerProfileRepository profiles;
    private final TeamMembershipRepository memberships;
    private final TeamViewsService views;
    private final TransactionRunner tx;

    @Override
    public TeamMemberInfo promote(UUID teamId, UUID profileId) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            TeamMembershipDataValue membership = membership(teamId, profileId);
            // Owner decision 2026-11-16: a PENDING member (no linked account)
            // cannot be promoted — they claim first, then become eligible.
            PlayerProfileDataValue candidate = profiles.find(profileId);
            if (candidate != null && candidate.accountId() == null) {
                throw new BusinessException(ErrorCode.MEMBER_PENDING);
            }
            if (TeamMembershipRole.PLAYER.name().equals(membership.role())) {
                TeamMembership changed = TeamMembership.from(membership);
                changed.promote();
                membership = memberships.save(changed.toData());
            }
            return views.memberInfo(membership, profiles.find(profileId),
                    permissions.viewerProfileIdOrNull(), true);
        });
    }

    @Override
    public TeamMemberInfo demote(UUID teamId, UUID profileId) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            TeamMembershipDataValue membership = membership(teamId, profileId);
            if (TeamMembershipRole.ADMIN.name().equals(membership.role()) && memberships.lockAdmins(teamId).size() <= 1) {
                // concurrent last-admin race: the loser re-reads the shrunken
                throw new BusinessException(ErrorCode.LAST_ADMIN);
            }
            TeamMembership changed = TeamMembership.from(membership);
            changed.demote();
            TeamMembershipDataValue saved = memberships.save(changed.toData());
            return views.memberInfo(saved, profiles.find(profileId),
                    permissions.viewerProfileIdOrNull(), true);
        });
    }

    @Override
    public void removeMember(UUID teamId, UUID profileId) {
        tx.run(() -> {
            permissions.requireAdmin(teamId);
            TeamMembershipDataValue membership = membership(teamId, profileId);
            if (TeamMembershipRole.ADMIN.name().equals(membership.role()) && memberships.lockAdmins(teamId).size() <= 1) {
                throw new BusinessException(ErrorCode.LAST_ADMIN);
            }
            memberships.delete(membership);
            // Owner decision 2026-11-15: removing a PENDING member (unclaimed
            if (memberships.findByProfile(profileId).isEmpty()) {
                PlayerProfileDataValue profile = profiles.find(profileId);
                if (profile != null && profile.accountId() == null) {
                    profiles.deleteById(profileId);
                }
            }
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
