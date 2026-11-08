package com.decoupledx.teammanagement.team.domain;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import com.decoupledx.teammanagement.team.domain.port.TeamDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamLogoService;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TeamLogoServiceImpl implements TeamLogoService {

    private final TeamPermissions permissions;
    private final TeamRepository teams;
    private final TeamQueryService teamQuery;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public TeamWithMembersInfo applyLogo(UUID teamId, String logoPath) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            TeamDataValue existing = teams.find(teamId);
            if (existing == null) {
                throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
            }
            Team team = Team.from(existing);
            team.applyLogo(logoPath, clock.instant());
            // team row exists — save is a no-race update (no unique columns change)
            teams.save(team.toData());
            return teamQuery.get(teamId);
        });
    }
}
