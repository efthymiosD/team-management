package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.domain.PlayerProfileDataValue;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for player-profile persistence. The profile/account unique
 * index and the claim-token unique index race as 409s inside the adapter.
 */
public interface PlayerProfileRepository {

    PlayerProfileDataValue save(PlayerProfileDataValue profile);

    PlayerProfileDataValue find(UUID profileId);

    PlayerProfileDataValue findByAccount(UUID accountId);

    /** Batch lookup for assembling member views (no per-member queries). */
    List<PlayerProfileDataValue> findAllByIds(Collection<UUID> profileIds);
}
