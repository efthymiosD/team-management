package com.decoupledx.teammanagement.team.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The relationship between a team and a player profile, with the member's
 * role. Removal DELETES the row — participation and results (later phases)
 * reference the profile, so history survives removal. The at-least-one-admin
 * invariant is enforced in the membership service under a locking read.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TeamMembership {

    private UUID teamId;
    private UUID profileId;
    private TeamMembershipRole role;
    private Instant joinedAt;
    private long version;

    private TeamMembership(UUID teamId, UUID profileId, TeamMembershipRole role, Instant joinedAt, long version) {
        this.teamId = teamId;
        this.profileId = profileId;
        this.role = role;
        this.joinedAt = joinedAt;
        this.version = version;
    }

    static TeamMembership create(UUID teamId, UUID profileId, TeamMembershipRole role, Instant joinedAt) {
        Objects.requireNonNull(teamId, "team id must not be null");
        Objects.requireNonNull(profileId, "profile id must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(joinedAt, "joinedAt must not be null");
        return new TeamMembership(teamId, profileId, role, joinedAt, 0L);
    }

    static TeamMembership from(TeamMembershipDataValue data) {
        return new TeamMembership(data.teamId(), data.profileId(), TeamMembershipRole.valueOf(data.role()),
                data.joinedAt(), data.version());
    }

    TeamMembershipDataValue toData() {
        return TeamMembershipDataValue.builder()
                .teamId(teamId)
                .profileId(profileId)
                .role(role.name())
                .joinedAt(joinedAt)
                .version(version)
                .build();
    }

    void promote() {
        this.role = TeamMembershipRole.ADMIN;
    }

    void demote() {
        this.role = TeamMembershipRole.PLAYER;
    }

    boolean isAdmin() {
        return role == TeamMembershipRole.ADMIN;
    }
}
