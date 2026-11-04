package com.decoupledx.teammanagement.identity.domain.port;

import com.decoupledx.teammanagement.identity.adapter.api.AccountId;
import com.decoupledx.teammanagement.identity.adapter.api.AccountInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AccountRepository {

    Optional<AccountId> findBySubject(String idpSubject);

    AccountId create(String idpSubject, String displayName);

    /**
     * Display names captured at login time, looked up by internal account id.
     * Accounts without a stored name are absent from the result.
     */
    Map<AccountId, String> displayNames(Collection<AccountId> accountIds);

    /**
     * All provisioned accounts, most-recently created first.
     */
    List<AccountInfo> findAll();

    /**
     * Provisioned accounts with the EXACT stored display name
     * (case-insensitive). A blank name yields an empty list — never an
     * unfiltered directory.
     */
    List<AccountId> byExactName(String displayName);

    /**
     * Seeds/overwrites the display name for an already-provisioned account.
     */
    void updateDisplayName(AccountId accountId, String displayName);

}