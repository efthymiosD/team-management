package com.decoupledx.teammanagement.identity.adapter.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Module API of the identity module: display names captured from the IdP at
 * login time, keyed by internal account id. Absent entries mean the account
 * has no stored name yet.
 */
public interface AccountDirectoryApi {

    Map<UUID, String> displayNames(Collection<AccountId> accounts);

    /**
     * All provisioned accounts (internal id + display name), most-recently
     * created first. Used by admin UIs to pick an account (e.g. for
     * profile linking).
     */
    List<AccountEntry> findAll();

    /**
     * Internal ids of all provisioned accounts whose stored display name
     * EQUALS the given name (case-insensitive); several accounts may share
     * a name. An empty or blank name yields an empty list — never an
     * unfiltered directory. Used by admin flows to translate a picked
     * account name into ids.
     */
    List<AccountId> idsByExactName(String displayName);
}