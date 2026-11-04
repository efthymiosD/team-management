package com.decoupledx.teammanagement.identity.adapter.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByIdpSubject(String idpSubject);

    List<AccountEntity> findByAccountIdIn(Collection<UUID> accountIds);

    List<AccountEntity> findAllByOrderByCreatedAtDesc();

    List<AccountEntity> findByDisplayNameIgnoreCase(String displayName);

    Optional<AccountEntity> findFirstByAccountId(UUID accountId);
}
