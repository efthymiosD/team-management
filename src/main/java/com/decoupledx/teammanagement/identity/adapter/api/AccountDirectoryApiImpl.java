package com.decoupledx.teammanagement.identity.adapter.api;

import com.decoupledx.teammanagement.identity.domain.port.AccountService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Module external-facing facade implementing {@link AccountDirectoryApi} on top of
 * the identity inbound port. The only bean other modules use for this module.
 */
@RequiredArgsConstructor
class AccountDirectoryApiImpl implements AccountDirectoryApi {

    private final AccountService accountService;

    @Override
    public Map<UUID, String> displayNames(Collection<AccountId> accounts) {
        return accountService.displayNames(accounts);
    }

    @Override
    public List<AccountEntry> findAll() {
        return accountService.findAll();
    }

    @Override
    public List<AccountId> idsByExactName(String displayName) {
        return accountService.byExactName(displayName);
    }
}
