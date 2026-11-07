package com.decoupledx.teammanagement.team.adapter.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TeamMembershipJpaRepository extends JpaRepository<TeamMembershipEntity, TeamMembershipPk> {

    List<TeamMembershipEntity> findByTeamIdOrderByJoinedAt(UUID teamId);

    List<TeamMembershipEntity> findByProfileId(UUID profileId);

    TeamMembershipEntity findByTeamIdAndProfileId(UUID teamId, UUID profileId);

    /**
     * Locks the team's ADMIN rows (SELECT FOR UPDATE) so the concurrent
     * last-admin race serializes in the database (one demote/remove wins,
     * others see the updated count and reject).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from TeamMembershipEntity m where m.teamId = :teamId and m.role = 'ADMIN'")
    List<TeamMembershipEntity> lockAdminsOfTeam(@Param("teamId") UUID teamId);
}
