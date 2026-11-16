package com.decoupledx.teammanagement.team.domain;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.tokens.OneTimeTokens;
import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.TeamInviteInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.domain.port.CurrentPlayerProfile;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteService;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TeamInviteServiceImpl implements TeamInviteService {

    private final TeamPermissionsService permissions;
    private final CurrentPlayerProfile currentPlayer;
    private final TeamRepository teams;
    private final TeamMembershipRepository memberships;
    private final TeamInviteRepository invites;
    private final TeamViewsService views;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public TeamInviteInfo mint(UUID teamId) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            requireTeam(teamId);
            String rawToken = OneTimeTokens.generate();
            TeamInviteDataValue saved = invites.save(TeamInviteDataValue.builder()
                    .id(UUID.randomUUID())
                    .teamId(teamId)
                    .tokenHash(OneTimeTokens.hash(rawToken))
                    .createdBy(currentPlayer.current().id())
                    .createdAt(clock.instant())
                    .build());
            return TeamInviteInfo.builder()
                    .inviteId(saved.id())
                    .teamId(saved.teamId())
                    .createdAt(saved.createdAt())
                    .rawToken(rawToken)
                    .build();
        });
    }

    @Override
    public void revoke(UUID teamId, UUID inviteId) {
        tx.run(() -> {
            permissions.requireAdmin(teamId);
            TeamInviteDataValue invite = invites.find(inviteId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_UNKNOWN));
            if (!invite.teamId().equals(teamId)) {
                throw new BusinessException(ErrorCode.INVITE_UNKNOWN);
            }
            invites.delete(inviteId);
            return null;
        });
    }

    @Override
    public List<TeamInviteInfo> activeInvites(UUID teamId) {
        permissions.requireAdmin(teamId);
        return invites.findActiveByTeam(teamId).stream()
                .map(invite -> TeamInviteInfo.builder()
                        .inviteId(invite.id())
                        .teamId(invite.teamId())
                        .createdAt(invite.createdAt())
                        .build()) // rawToken deliberately absent — hash-only storage
                .toList();
    }

    @Override
    public boolean inviteActive(UUID teamId, String rawToken) {
        return invites.findByTokenHash(OneTimeTokens.hash(rawToken))
                .filter(invite -> invite.teamId().equals(teamId))
                .filter(invite -> invite.usedAt() == null)
                .isPresent();
    }

    @Override
    public TeamMemberInfo joinByInvite(UUID teamId, String rawToken) {
        // Cheap reads first: a doomed request must not provision a profile.
        requireTeam(teamId);
        TeamInviteDataValue invite = invites
                .findByTokenHash(OneTimeTokens.hash(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_UNKNOWN));
        if (!invite.teamId().equals(teamId)) {
            throw new BusinessException(ErrorCode.INVITE_UNKNOWN);
        }
        PlayerProfileDataValue profile = currentPlayer.current();
        return tx.run(() -> {
            TeamMembershipDataValue existing = memberships.find(teamId, profile.id());
            if (existing != null) {
                throw new BusinessException(ErrorCode.ALREADY_MEMBER);
            }
            // THE guarded consume: exactly one parallel caller matches a row;
            // the loser surfaces the honest already-used conflict (ADR-0003).
            if (!invites.consume(invite.tokenHash(), profile.id(), clock.instant())) {
                throw new BusinessException(ErrorCode.INVITE_ALREADY_USED);
            }
            TeamMembershipDataValue membership = memberships.save(TeamMembership
                    .create(teamId, profile.id(), TeamMembershipRole.PLAYER, clock.instant()).toData());
            return views.memberInfo(membership, profile, profile.id(), false);
        });
    }

    private void requireTeam(UUID teamId) {
        TeamDataValue team = teams.find(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
    }
}
