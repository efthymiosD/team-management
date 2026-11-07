package com.decoupledx.teammanagement.team.adapter.persistence;

import com.decoupledx.teammanagement.team.domain.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PlayerProfilePersistenceAdapter implements PlayerProfileRepository {

    private final PlayerProfileJpaRepository profiles;

    @Override
    public PlayerProfileDataValue save(PlayerProfileDataValue profile) {
        if (profile.id() != null) {
            PlayerProfileEntity existing = profiles.findById(profile.id()).orElse(null);
            if (existing != null) {
                existing.syncFrom(profile);
                return profiles.saveAndFlush(existing).toData();
            }
        }
        try {
            return profiles.saveAndFlush(new PlayerProfileEntity(profile)).toData();
        } catch (DataIntegrityViolationException conflict) {
            // Parallel first-use provisioning (account-claiming race — the
            // claim-TOKEN race will need its own recovery when the claim
            // flow lands). Resolve INSIDE the work: return the winner's row
            // (library ADR-0002).
            return recoverExistingOrRethrow(profile, conflict);
        } catch (ConcurrencyFailureException deadlock) {
            // 40P01 on the insert path: the winner's row is committed or
            // pending recovery elsewhere — same in-work recovery.
            return recoverExistingOrRethrow(profile, deadlock);
        }
    }

    private PlayerProfileDataValue recoverExistingOrRethrow(PlayerProfileDataValue profile,
            RuntimeException conflict) {
        if (profile.accountId() != null) {
            PlayerProfileDataValue winner = findByAccount(profile.accountId());
            if (winner != null) {
                return winner;
            }
        }
        throw conflict;
    }

    @Override
    public PlayerProfileDataValue find(UUID profileId) {
        return profiles.findById(profileId).map(PlayerProfileEntity::toData).orElse(null);
    }

    @Override
    public PlayerProfileDataValue findByAccount(UUID accountId) {
        PlayerProfileEntity entity = profiles.findByAccountId(accountId);
        return entity != null ? entity.toData() : null;
    }

    @Override
    public List<PlayerProfileDataValue> findAllByIds(Collection<UUID> profileIds) {
        if (profileIds.isEmpty()) {
            return List.of();
        }
        return profiles.findAllByIdIn(profileIds).stream().map(PlayerProfileEntity::toData).toList();
    }
}
