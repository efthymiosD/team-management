package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.domain.TeamDataValue;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for team persistence. Uniqueness races (the team-name unique
 * index) convert to the appropriate 409 code inside the adapter.
 */
public interface TeamRepository {

    TeamDataValue save(TeamDataValue team);

    TeamDataValue find(UUID teamId);

    /** Batch lookup for team list views (no per-team queries). */
    List<TeamDataValue> findAllByIds(Collection<UUID> teamIds);
}
