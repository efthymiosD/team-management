package com.decoupledx.teammanagement.identity.domain.port;

import com.decoupledx.teammanagement.identity.adapter.api.AccountEntry;
import com.decoupledx.teammanagement.identity.adapter.api.AccountId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inbound port for the identity module: account resolution and provisioning.
 */
public interface AccountService {

    AccountId resolveOrProvision(String idpSubject);

    AccountId resolveOrProvision(String idpSubject, String displayName);

    Map<UUID, String> displayNames(Collection<AccountId> accountIds);

    List<AccountEntry> findAll();

    /**
     * Internal ids of the provisioned accounts whose stored display name
     * EQUALS the given name (case-insensitive); several accounts may share
     * a name. An empty or blank name yields an empty list — never an
     * unfiltered directory.
     */
    List<AccountId> byExactName(String displayName);
}
