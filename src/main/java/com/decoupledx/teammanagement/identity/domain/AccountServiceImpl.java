package com.decoupledx.teammanagement.identity.domain;

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

    @Override
    public AccountId resolveOrProvision(String idpSubject) {
        return resolveOrProvision(idpSubject, null, null);
    }

    @Override
    public AccountId resolveOrProvision(String idpSubject, String displayName) {
        return resolveOrProvision(idpSubject, displayName, null);
    }

    @Override
    public AccountId resolveOrProvision(String idpSubject, String displayName, String email) {
        // race the loser's INSERT fails and aborts a wrapped transaction
        // ("current transaction is aborted"), so no further statements could
        // row on the race.
        AccountId account = accounts.findBySubject(idpSubject)
                .orElseGet(() -> accounts.create(idpSubject, displayName, email));
        if (displayName != null
                && !accounts.displayNames(List.of(account)).containsKey(account)) {
            accounts.updateDisplayName(account, displayName);
        }
        if (email != null && !email.isBlank()
                && !email.equals(accounts.emails(List.of(account)).get(account))) {
            accounts.updateEmail(account, email.strip());
        }
        return account;
    }

    @Override
    public Map<UUID, String> emails(Collection<AccountId> accountIds) {
        return accounts.emails(accountIds).entrySet().stream()
                .filter(entry -> isUuid(entry.getKey().value()))
                .collect(Collectors.toMap(
                        entry -> UUID.fromString(entry.getKey().value()),
                        Map.Entry::getValue));
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
