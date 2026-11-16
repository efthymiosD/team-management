package com.decoupledx.teammanagement.team.adapter.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PlayerProfileJpaRepository extends JpaRepository<PlayerProfileEntity, UUID> {

    Optional<PlayerProfileEntity> findById(UUID id);

    PlayerProfileEntity findByAccountId(UUID accountId);

    PlayerProfileEntity findByClaimTokenHash(String claimTokenHash);

    List<PlayerProfileEntity> findAllByIdIn(@Param("ids") Collection<UUID> ids);

    /**
     * THE single atomic guarded claim-token consume: exactly one parallel
     * caller matches a row; the loser matches 0 and surfaces the honest
     * already-claimed conflict (decoupledx-common ADR-0003 semantics).
     */
    @Modifying
    @Query("update PlayerProfileEntity p set p.claimTokenHash = null "
            + "where p.id = :id and p.claimTokenHash = :tokenHash and p.accountId is null")
    int consumeClaimToken(@Param("id") UUID id, @Param("tokenHash") String tokenHash);
}
