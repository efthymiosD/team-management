package com.decoupledx.teammanagement.team.adapter.persistence;
import com.decoupledx.common.error.BusinessException;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TeamMembershipPersistenceAdapter implements TeamMembershipRepository {

    private final TeamMembershipJpaRepository memberships;

    @Override
    public TeamMembershipDataValue save(TeamMembershipDataValue membership) {
        try {
            TeamMembershipEntity entity = memberships.findById(
                    new TeamMembershipPk(membership.teamId(), membership.profileId()))
                    .orElseGet(() -> new TeamMembershipEntity(membership));
            entity.syncFrom(membership);
            return memberships.saveAndFlush(entity).toData();
        } catch (DataIntegrityViolationException conflict) {
            // team_memberships unique (team_id, profile_id) — joining twice in
            // parallel loses the race as ALREADY_MEMBER. (FK races cannot
            // occur: members and teams are never deleted.)
            throw new BusinessException(ErrorCode.ALREADY_MEMBER);
        } catch (ConcurrencyFailureException deadlock) {
            // 40P01 (e.g. deadlock with the ADMIN row locking in the
            // demote/remove path): the honest 409 translation, per AGENTS.md.
            throw new BusinessException(ErrorCode.ALREADY_MEMBER);
        }
    }

    @Override
    public TeamMembershipDataValue find(UUID teamId, UUID profileId) {
        return memberships.findById(new TeamMembershipPk(teamId, profileId))
                .map(TeamMembershipEntity::toData).orElse(null);
    }

    @Override
    public List<TeamMembershipDataValue> findByTeam(UUID teamId) {
        return memberships.findByTeamIdOrderByJoinedAt(teamId).stream()
                .map(TeamMembershipEntity::toData).toList();
    }

    @Override
    public List<TeamMembershipDataValue> findByProfile(UUID profileId) {
        return memberships.findByProfileId(profileId).stream()
                .map(TeamMembershipEntity::toData).toList();
    }

    @Override
    public List<TeamMembershipDataValue> lockAdmins(UUID teamId) {
        return memberships.lockAdminsOfTeam(teamId).stream()
                .map(TeamMembershipEntity::toData).toList();
    }

    @Override
    public void delete(TeamMembershipDataValue membership) {
        memberships.deleteById(new TeamMembershipPk(membership.teamId(), membership.profileId()));
    }
}