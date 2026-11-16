package com.decoupledx.teammanagement.identity.domain.port;

import com.decoupledx.teammanagement.identity.adapter.api.AccountId;
import com.decoupledx.teammanagement.identity.adapter.api.AccountInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AccountRepository {

    Optional<AccountId> findBySubject(String idpSubject);

    AccountId create(String idpSubject, String displayName, String email);

    /**
     * Display names captured at login time, looked up by internal account id.
     * Accounts without a stored name are absent from the result.
     */
    Map<AccountId, String> displayNames(Collection<AccountId> accountIds);

    /**
     * E-mail addresses (mirrored from the IdP) keyed by internal account id.
     * Accounts without a stored address are absent from the result.
     */
    Map<AccountId, String> emails(Collection<AccountId> accountIds);

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

    /** Mirrors the IdP e-mail address onto an already-provisioned account. */
    void updateEmail(AccountId accountId, String email);

}