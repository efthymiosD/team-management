package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.teammanagement.identity.adapter.api.AccountDirectoryApi;
import com.decoupledx.teammanagement.identity.adapter.api.AccountId;
import com.decoupledx.teammanagement.identity.adapter.api.CurrentAccountApi;
import com.decoupledx.teammanagement.team.domain.port.CurrentPlayerProfile;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Account → player profile linking on first team action (decision: any
 * authenticated account may act; the profile is provisioned and named after
 * the account's captured IdP display name).
 */
@RequiredArgsConstructor
class CurrentPlayerProfileImpl implements CurrentPlayerProfile {

    private static final String FALLBACK_NAME = "Player";

    private final CurrentAccountApi accounts;
    private final AccountDirectoryApi directory;
    private final PlayerProfileRepository profiles;
    private final Clock clock;

    @Override
    public PlayerProfileDataValue current() {
        UUID accountId = UUID.fromString(accounts.currentAccountId().value());
        PlayerProfileDataValue existing = profiles.findByAccount(accountId);
        if (existing != null) {
            return existing;
        }
        PlayerProfile profile = PlayerProfile.forAccount(accountId, displayName(accountId),
                clock.instant());
        return profiles.save(profile.toData());
    }

    private String displayName(UUID accountId) {
        return directory.displayNames(List.of(AccountId.of(accountId.toString()))).values()
                .stream()
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(FALLBACK_NAME);
    }
}
