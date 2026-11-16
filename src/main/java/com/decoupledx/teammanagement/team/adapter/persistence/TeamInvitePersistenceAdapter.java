package com.decoupledx.teammanagement.team.adapter.persistence;

import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TeamInvitePersistenceAdapter implements TeamInviteRepository {

    private final TeamInviteJpaRepository invites;
    private final TransactionRunner tx;

    @Override
    public TeamInviteDataValue save(TeamInviteDataValue invite) {
        return tx.run(() -> invites.save(new TeamInviteEntity(invite.id(), invite.teamId(),
                invite.tokenHash(), invite.createdBy(), invite.createdAt())).toData());
    }

    @Override
    public Optional<TeamInviteDataValue> find(UUID id) {
        return invites.findById(id).map(TeamInviteEntity::toData);
    }

    @Override
    public Optional<TeamInviteDataValue> findByTokenHash(String tokenHash) {
        return invites.findByTokenHash(tokenHash).map(TeamInviteEntity::toData);
    }

    @Override
    public List<TeamInviteDataValue> findActiveByTeam(UUID teamId) {
        return invites.findByTeamIdAndUsedAtIsNullOrderByCreatedAtDesc(teamId).stream()
                .map(TeamInviteEntity::toData)
                .toList();
    }

    @Override
    public boolean consume(String tokenHash, UUID usedByProfileId, Instant now) {
        return tx.run(() -> {
            try {
                return invites.consume(tokenHash, usedByProfileId, now) == 1;
            } catch (ConcurrencyFailureException lostUpdateRace) {
                // 40P01-class race on the guarded UPDATE: the honest
                // already-used translation, resolved INSIDE the work per the
                // SingleUseTokenLedger contract (never rethrown mid-flow).
                return false;
            }
        });
    }

    @Override
    public void delete(UUID id) {
        tx.run(() -> {
            invites.deleteById(id);
            return null;
        });
    }
}
