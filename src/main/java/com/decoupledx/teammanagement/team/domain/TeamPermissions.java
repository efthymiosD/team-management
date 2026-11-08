package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.common.error.BusinessException;
import com.decoupledx.teammanagement.identity.adapter.api.CurrentAccountApi;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipDataValue;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * The acting-viewer facts the write flows share: who is the viewer's player
 * profile (if any), and are they an administrator of the given team. Both are
 * resolved INSIDE feature-domain code (ADR-0002) — adapters never thread the
 * account. The last-admin invariant itself lives in the membership service.
 */
@RequiredArgsConstructor
class TeamPermissions {

    private final CurrentAccountApi accounts;
    private final PlayerProfileRepository profiles;
    private final TeamMembershipRepository memberships;

    /** Rejection when the acting viewer has no team membership — 403, not 401. */
    UUID requireMember(UUID teamId) {
        UUID viewer = viewerProfileId();
        TeamMembershipDataValue mine = memberships.find(teamId, viewer);
        if (mine == null) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }
        return viewer;
    }

    void requireAdmin(UUID teamId) {
        UUID viewer = viewerProfileId();
        TeamMembershipDataValue mine = memberships.find(teamId, viewer);
        if (mine == null) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }
        if (!"ADMIN".equals(mine.role())) {
            throw new BusinessException(ErrorCode.NOT_TEAM_ADMIN);
        }
    }

    /** Viewer's profile id, or null when the account has no profile yet. */
    UUID viewerProfileIdOrNull() {
        PlayerProfileDataValue profile = profiles.findByAccount(accountId());
        return profile != null ? profile.id() : null;
    }

    private UUID viewerProfileId() {
        UUID profileId = viewerProfileIdOrNull();
        if (profileId == null) {
            throw new BusinessException(ErrorCode.NOT_TEAM_MEMBER);
        }
        return profileId;
    }

    private UUID accountId() {
        return UUID.fromString(accounts.currentAccountId().value());
    }
}
