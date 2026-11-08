package com.decoupledx.teammanagement.team.adapter.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

interface PlayerProfileJpaRepository extends JpaRepository<PlayerProfileEntity, UUID> {

    PlayerProfileEntity findByAccountId(UUID accountId);

    PlayerProfileEntity findByClaimToken(String claimToken);

    List<PlayerProfileEntity> findAllByIdIn(@Param("ids") Collection<UUID> ids);
}
