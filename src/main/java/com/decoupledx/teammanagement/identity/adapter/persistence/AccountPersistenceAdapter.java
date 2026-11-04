package com.decoupledx.teammanagement.identity.adapter.persistence;

import com.decoupledx.teammanagement.identity.adapter.api.AccountId;
import com.decoupledx.teammanagement.identity.adapter.api.AccountInfo;
import com.decoupledx.teammanagement.identity.domain.port.AccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AccountPersistenceAdapter implements AccountRepository {

    private final AccountJpaRepository accounts;

    @Override
    public Optional<AccountId> findBySubject(String idpSubject) {
        return accounts.findByIdpSubject(idpSubject)
                .map(entity -> AccountId.of(entity.getAccountId().toString()));
    }

    @Override
    public Map<AccountId, String> displayNames(
            java.util.Collection<AccountId> accountIds) {
        List<UUID> ids = accountIds.stream()
                .filter(id -> {
                    try {
                        UUID.fromString(id.value());
                        return true;
                    } catch (IllegalArgumentException notAnId) {
                        return false;
                    }
                })
                .map(id -> UUID.fromString(id.value()))
                .toList();
        return accounts
                .findByAccountIdIn(ids)
                .stream()
                .filter(entity -> entity.getDisplayName() != null)
                .collect(java.util.stream.Collectors.toMap(
                        entity -> AccountId.of(entity.getAccountId().toString()),
                        AccountEntity::getDisplayName));
    }

    @Override
    public List<AccountInfo> findAll() {
        return accounts.findAllByOrderByCreatedAtDesc().stream()
                .map(entity -> new AccountInfo(
                        AccountId.of(entity.getAccountId().toString()),
                        entity.getDisplayName()))
                .toList();
    }

    @Override
    public List<AccountId> byExactName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return List.of();
        }
        return accounts.findByDisplayNameIgnoreCase(displayName.trim()).stream()
                .map(entity -> AccountId.of(entity.getAccountId().toString()))
                .toList();
    }

    @Override
    public void updateDisplayName(AccountId accountId, String displayName) {
        accounts.findFirstByAccountId(UUID.fromString(accountId.value()))
                .ifPresent(entity -> {
                    entity.setDisplayName(displayName);
                    accounts.save(entity);
                });
    }

    @Override
    public AccountId create(String idpSubject, String displayName) {
        AccountEntity entity = new AccountEntity(
                UUID.randomUUID(), idpSubject, displayName, Instant.now());
        try {
            accounts.saveAndFlush(entity);
            return AccountId.of(entity.getAccountId().toString());
        } catch (DataIntegrityViolationException conflict) {
            return findBySubject(idpSubject)
                    .orElseThrow(() -> conflict);
        }
    }
}
