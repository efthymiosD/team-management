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

    /**
     * Resolution with the IdP-captured identity: display name and the
     * (registration-required) e-mail address, mirrored onto the account.
     */
    AccountId resolveOrProvision(String idpSubject, String displayName, String email);

    Map<UUID, String> displayNames(Collection<AccountId> accountIds);

    /** E-mail addresses keyed by internal account id (absent = not captured). */
    Map<UUID, String> emails(Collection<AccountId> accountIds);

    List<AccountEntry> findAll();

    /**
     * Internal ids of the provisioned accounts whose stored display name
     * EQUALS the given name (case-insensitive); several accounts may share
     * a name. An empty or blank name yields an empty list — never an
     * unfiltered directory.
     */
    List<AccountId> byExactName(String displayName);
}
