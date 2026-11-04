package com.decoupledx.teammanagement.identity.domain;

import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.identity.adapter.api.AccountEntry;
import com.decoupledx.teammanagement.identity.adapter.api.AccountId;
import com.decoupledx.teammanagement.identity.domain.port.AccountRepository;
import com.decoupledx.teammanagement.identity.domain.port.AccountService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class AccountServiceImpl implements AccountService {

    private final AccountRepository accounts;
    private final TransactionRunner tx;

    @Override
    public AccountId resolveOrProvision(String idpSubject) {
        return resolveOrProvision(idpSubject, null);
    }

    @Override
    public AccountId resolveOrProvision(String idpSubject, String displayName) {
        return tx.run(() -> {
            AccountId existing = accounts.findBySubject(idpSubject)
                    .orElseGet(() -> accounts.create(idpSubject, displayName));
            if (displayName != null
                    && !accounts.displayNames(java.util.List.of(existing))
                    .containsKey(existing)) {
                accounts.updateDisplayName(existing, displayName);
            }
            return existing;
        });
    }

    @Override
    public Map<UUID, String> displayNames(Collection<AccountId> accountIds) {
        return accounts.displayNames(accountIds).entrySet().stream()
                .filter(entry -> isUuid(entry.getKey().value()))
                .collect(Collectors.toMap(
                        entry -> UUID.fromString(entry.getKey().value()),
                        Map.Entry::getValue));
    }

    @Override
    public List<AccountEntry> findAll() {
        return accounts.findAll().stream()
                .filter(entry -> isUuid(entry.accountId().value()))
                .map(entry -> new AccountEntry(
                        UUID.fromString(entry.accountId().value()),
                        entry.displayName()))
                .toList();
    }

    @Override
    public List<AccountId> byExactName(String displayName) {
        return accounts.byExactName(displayName);
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException notAnId) {
            return false;
        }
    }
}
