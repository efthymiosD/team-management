package com.decoupledx.teammanagement.team.adapter.persistence;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.domain.TeamDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TeamPersistenceAdapter implements TeamRepository {

    private final TeamJpaRepository teams;

    @Override
    public TeamDataValue save(TeamDataValue team) {
        try {
            TeamEntity entity = teams.findById(team.id())
                    .orElseGet(() -> new TeamEntity(team));
            entity.syncFrom(team);
            return teams.saveAndFlush(entity).toData();
        } catch (DataIntegrityViolationException conflict) {
            // teams_name_unique — the only unique constraint on teams
            throw new BusinessException(ErrorCode.TEAM_NAME_TAKEN);
        } catch (ConcurrencyFailureException deadlock) {
            // 40P01: the parallel write wins; the caller gets the honest 409
            // (AGENTS.md — both constraint races translate).
            throw new BusinessException(ErrorCode.TEAM_NAME_TAKEN);
        }
    }

    @Override
    public TeamDataValue find(UUID teamId) {
        return teams.findById(teamId).map(TeamEntity::toData).orElse(null);
    }

    @Override
    public List<TeamDataValue> findAllByIds(Collection<UUID> teamIds) {
        if (teamIds.isEmpty()) {
            return List.of();
        }
        return teams.findAllByIdIn(teamIds).stream().map(TeamEntity::toData).toList();
    }
}
