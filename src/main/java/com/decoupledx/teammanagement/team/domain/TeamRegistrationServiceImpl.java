package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.common.tokens.OneTimeTokens;
import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.common.tokens.OneTimeTokens;
import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.domain.port.CurrentPlayerProfile;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamRegistrationService;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TeamRegistrationServiceImpl implements TeamRegistrationService {

    private final TeamPermissionsService permissions;
    private final CurrentPlayerProfile currentPlayer;
    private final TeamRepository teams;
    private final TeamMembershipRepository memberships;
    private final PlayerProfileRepository profiles;
    private final TeamViewsService views;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public PlayerProfileInfo myProfile() {
        return views.profileInfo(currentPlayer.current());
    }

    @Override
    public boolean myProfileIsComplete() {
        return PlayerProfile.from(currentPlayer.current()).isComplete();
    }

    /**
     * Admin registration of a pending member: the aggregate stores the SHA-256
     * HASH of the claim token — the RAW token exists exactly ONCE, in the
     * returned view (the claim link), and never at rest (ADR-0003).
     */
    @Override
    public PlayerProfileInfo registerPlayer(UUID teamId, String fullName, String phone,
            LocalDate birthDate, String nationality) {
        return tx.run(() -> {
            permissions.requireAdmin(teamId);
            String rawToken = OneTimeTokens.generate();
            PlayerProfile profile = PlayerProfile.unclaimed(fullName, phone, birthDate,
                    nationality, OneTimeTokens.hash(rawToken), clock.instant());
            PlayerProfileDataValue savedProfile = profiles.save(profile.toData());
            memberships.save(TeamMembership
                    .create(teamId, savedProfile.id(), TeamMembershipRole.PLAYER, clock.instant())
                    .toData());
            return views.profileInfo(savedProfile, rawToken);
        });
    }

}
