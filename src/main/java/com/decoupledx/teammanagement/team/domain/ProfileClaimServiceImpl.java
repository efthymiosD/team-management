package com.decoupledx.teammanagement.team.domain;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.tokens.OneTimeTokens;
import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.identity.adapter.api.CurrentAccountApi;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.adapter.api.ClaimPreviewInfo;
import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.ProfileClaimService;
import com.decoupledx.teammanagement.team.domain.port.TeamDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;

@RequiredArgsConstructor
class ProfileClaimServiceImpl implements ProfileClaimService {

    private final CurrentAccountApi accounts;
    private final PlayerProfileRepository profiles;
    private final TeamMembershipRepository memberships;
    private final TeamRepository teams;
    private final TeamPermissionsService permissions;
    private final TeamViewsService views;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public ClaimPreviewInfo preview(String claimToken) {
        PlayerProfileDataValue claimable = profiles
                .findByClaimTokenHash(OneTimeTokens.hash(claimToken));
        if (claimable == null || claimable.accountId() != null) {
            throw new BusinessException(ErrorCode.CLAIM_TOKEN_UNKNOWN);
        }
        // A pending member belongs to exactly one team (their only membership).
        TeamMembershipDataValue membership = memberships.findByProfile(claimable.id()).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CLAIM_TOKEN_UNKNOWN));
        TeamDataValue team = teams.find(membership.teamId());
        if (team == null) {
            throw new BusinessException(ErrorCode.CLAIM_TOKEN_UNKNOWN);
        }
        return new ClaimPreviewInfo(claimable.id(), team.id(), team.name(), team.logoPath(),
                claimable.fullName(), claimable.phone());
    }

    @Override
    public PlayerProfileInfo regenerateClaimLink(UUID teamId, UUID profileId) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            PlayerProfileDataValue pending = profiles.find(profileId);
            if (pending == null || memberships.find(teamId, profileId) == null) {
                throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
            }
            if (pending.accountId() != null) {
                // Claimed profiles have no pending link to regenerate.
                throw new BusinessException(ErrorCode.PROFILE_ALREADY_CLAIMED);
            }
            String rawToken = OneTimeTokens.generate();
            PlayerProfile refreshed = PlayerProfile.from(pending);
            refreshed.setClaimTokenHash(OneTimeTokens.hash(rawToken));
            try {
                profiles.save(refreshed.toData());
            } catch (ConcurrencyFailureException claimRace) {
                // The pending member CLAIMED the profile mid-flight (the
                // optimistic version consumed their window) — the honest
                // already-claimed conflict; the admin can re-check and mint
                // again if a link is still wanted.
                throw new BusinessException(ErrorCode.PROFILE_ALREADY_CLAIMED);
            }
            // The RAW token appears exactly once — in THIS response (ADR-0003).
            return views.profileInfo(refreshed.toData(), rawToken);
        });
    }

    @Override
    public PlayerProfileInfo claim(String claimToken) {
        // Resolution (no provisioning!) happens here so that a claim NEVER
        // ends up linking/creating a second profile for the same account.
        UUID accountId = UUID.fromString(accounts.currentAccountId().value());
        String tokenHash = OneTimeTokens.hash(claimToken);
        // Cheap reads for a friendly error; the authoritative single-use
        // guard is the atomic consume INSIDE the transaction (below) —
        // without it the merge branch would double-spend under race.
        PlayerProfileDataValue claimable = profiles.findByClaimTokenHash(tokenHash);
        if (claimable == null) {
            throw new BusinessException(ErrorCode.CLAIM_TOKEN_UNKNOWN);
        }
        if (claimable.accountId() != null) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_CLAIMED);
        }
        PlayerProfileDataValue existing = profiles.findByAccount(accountId);
        return tx.run(() -> {
            // THE guarded single-use consume: exactly one parallel caller
            // wins; the loser surfaces the honest already-claimed conflict.
            if (!profiles.consumeClaimToken(claimable.id(), tokenHash)) {
                throw new BusinessException(ErrorCode.PROFILE_ALREADY_CLAIMED);
            }
            if (existing != null) {
                // Owner decision 2026-11-15 (claim of a pending member by an
                // account that ALREADY has a profile): the player's own
                // profile wins — memberships re-point, placeholder deleted.
                return views.profileInfo(mergeIntoExisting(claimable, existing));
            }
            PlayerProfile claimed = PlayerProfile.from(claimable);
            claimed.linkAccount(accountId, clock.instant());
            claimed.clearClaimTokenHash(clock.instant());
            try {
                profiles.save(claimed.toData());
            } catch (ConcurrencyFailureException versionRace) {
                // The guarded consume already made this branch single-entry;
                // a CFE here is an unrelated lock race — fail CLOSED with the
                // honest already-claimed conflict (aborted-tx rule: no SQL in
                // the catch; the loss mode maps by exception type).
                throw new BusinessException(ErrorCode.PROFILE_ALREADY_CLAIMED);
            } catch (DataIntegrityViolationException accountUniqueRace) {
                // account-unique DIVE from the UPDATE: THIS account got
                // another profile concurrently (provision/claim race) — the
                // claim row itself is untouched → ACCOUNT_ALREADY_PROFILED.
                throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_PROFILED);
            }
            return views.profileInfo(claimed.toData());
        });
    }

    /**
     * The existing-account merge: every team the PENDING placeholder joined
     * moves to the player's existing profile (duplicate memberships for a
     * team the player is already in are simply discarded), then the
     * placeholder row is hard-deleted. One unit — the membership re-points
     * and the row removal commit or roll back together (aborted-tx rule).
     */
    private PlayerProfileDataValue mergeIntoExisting(PlayerProfileDataValue claimable,
                                                     PlayerProfileDataValue existing) {
        for (TeamMembershipDataValue pending : memberships.findByProfile(claimable.id())) {
            if (memberships.find(pending.teamId(), existing.id()) != null) {
                memberships.delete(pending);
            } else {
                memberships.save(TeamMembership
                        .create(pending.teamId(), existing.id(),
                                TeamMembershipRole.PLAYER, pending.joinedAt())
                        .toData());
                memberships.delete(pending);
            }
        }
        profiles.deleteById(claimable.id());
        return existing;
    }
}
