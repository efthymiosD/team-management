package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.identity.adapter.api.CurrentAccountApi;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.ProfileClaimService;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;

@RequiredArgsConstructor
class ProfileClaimServiceImpl implements ProfileClaimService {

    private final CurrentAccountApi accounts;
    private final PlayerProfileRepository profiles;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public PlayerProfileInfo claim(String claimToken) {
        // Resolution (no provisioning!) happens here so that a claim NEVER
        // ends up linking/creating a second profile for the same account.
        UUID accountId = UUID.fromString(accounts.currentAccountId().value());
        return tx.run(() -> {
            PlayerProfileDataValue claimable = profiles.findByClaimToken(claimToken);
            if (claimable == null) {
                // Unknown token or already used (claiming CLEARS it).
                throw new BusinessException(ErrorCode.CLAIM_TOKEN_UNKNOWN);
            }
            if (claimable.accountId() != null) {
                throw new BusinessException(ErrorCode.PROFILE_ALREADY_CLAIMED);
            }
            if (profiles.findByAccount(accountId) != null) {
                throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_PROFILED);
            }
            PlayerProfile claimed = PlayerProfile.from(claimable);
            claimed.linkAccount(accountId, clock.instant());
            claimed.clearClaimToken(clock.instant());
            try {
                profiles.save(claimed.toData());
            } catch (ConcurrencyFailureException versionRace) {
                // The claim UPDATE version-matched 0 rows: a concurrent
                // claimant consumed this token first. NO re-read here — the
                // failed flush aborted the Postgres tx, any further SQL in
                // the catch is doomed (aborted-tx rule). The loss mode maps
                // deterministically by exception type.
                throw new BusinessException(ErrorCode.PROFILE_ALREADY_CLAIMED);
            } catch (DataIntegrityViolationException accountUniqueRace) {
                // account-unique DIVE from the UPDATE: THIS account got
                // another profile concurrently (provision/claim race) — the
                // claim row itself is untouched → ACCOUNT_ALREADY_PROFILED.
                throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_PROFILED);
            }
            return TeamRegistrationServiceImpl.profileInfo(claimed.toData());
        });
    }
}
