package com.decoupledx.teammanagement.team.domain;

import com.decoupledx.teammanagement.identity.adapter.api.AccountDirectoryApi;
import com.decoupledx.teammanagement.identity.adapter.api.AccountId;
import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamMemberInfo;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipDataValue;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * The domain's API-view factory service: the one place that maps port data to the
 * adapter/api shapes shared with the facade. Email is resolved from the
 * identity module's directory (the sanctioned cross-module api edge). NO
 * Spring annotations — instantiated by the domain config (domain purity:
 * the config class is the only framework-annotated domain type).
 */
@RequiredArgsConstructor
class TeamViewsService {

    private final AccountDirectoryApi directory;

    /**
     * A member as seen by a viewer: the phone number is the caller's privacy
     * decision (admins see everyone's, members their own — others get null);
     * the birthday is visible to all teammates. {@code mine} marks the
     * viewer's own row.
     */
    TeamMemberInfo memberInfo(TeamMembershipDataValue membership, PlayerProfileDataValue profile,
            UUID viewerProfileId, boolean viewerIsAdmin) {
        boolean own = membership.profileId().equals(viewerProfileId);
        String phone = viewerIsAdmin || own ? profile.phone() : null;
        return new TeamMemberInfo(profile.id(), profile.fullName(), phone,
                formatBirthDate(profile.birthDate()), profile.nationality(), profile.photoPath(),
                TeamMembershipRole.ADMIN.name().equals(membership.role()),
                profile.accountId() != null, own, viewerIsAdmin);
    }

    /** The profile view — the claim-token HASH is never exposed; the RAW
     *  token rides ONLY the mint responses (member registration and claim
     *  link regeneration — ADR-0003). */
    PlayerProfileInfo profileInfo(PlayerProfileDataValue profile) {
        return new PlayerProfileInfo(profile.id(), profile.accountId(), profile.fullName(),
                emailOf(profile), profile.phone(), formatBirthDate(profile.birthDate()),
                profile.nationality(), profile.photoPath(), null);
    }

    /** The profile view with the RAW claim token injected — the mint
     *  responses' one-time exposure (registration / regeneration). */
    PlayerProfileInfo profileInfo(PlayerProfileDataValue profile, String rawClaimToken) {
        return new PlayerProfileInfo(profile.id(), profile.accountId(), profile.fullName(),
                emailOf(profile), profile.phone(), formatBirthDate(profile.birthDate()),
                profile.nationality(), profile.photoPath(), rawClaimToken);
    }

    private String emailOf(PlayerProfileDataValue profile) {
        if (profile.accountId() == null) {
            return null;
        }
        return directory.emails(List.of(AccountId.of(profile.accountId().toString())))
                .get(profile.accountId());
    }

    private static String formatBirthDate(LocalDate birthDate) {
        return birthDate == null ? null : birthDate.toString();
    }
}
