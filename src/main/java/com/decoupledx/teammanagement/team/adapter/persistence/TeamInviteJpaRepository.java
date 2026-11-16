package com.decoupledx.teammanagement.team.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TeamInviteJpaRepository extends JpaRepository<TeamInviteEntity, UUID> {

    Optional<TeamInviteEntity> findByTokenHash(String tokenHash);

    List<TeamInviteEntity> findByTeamIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID teamId);

    /**
     * THE single atomic guarded consume (decoupledx-common ADR-0003): exactly
     * one parallel caller matches a row; the loser matches 0 and surfaces the
     * honest already-used conflict.
     */
    @Modifying
    @Query("update TeamInviteEntity i set i.usedAt = :now, i.usedBy = :profileId "
            + "where i.tokenHash = :tokenHash and i.usedAt is null")
    int consume(@Param("tokenHash") String tokenHash, @Param("profileId") UUID profileId,
            @Param("now") java.time.Instant now);
}
