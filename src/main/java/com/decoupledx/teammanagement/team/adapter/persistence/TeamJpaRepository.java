package com.decoupledx.teammanagement.team.adapter.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TeamJpaRepository extends JpaRepository<TeamEntity, UUID> {

    List<TeamEntity> findAllByIdIn(Collection<UUID> ids);
}
