package com.decoupledx.teammanagement.team.adapter.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Composite primary key (team_id, profile_id) of {@link TeamMembershipEntity}. */
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class TeamMembershipPk implements Serializable {

    UUID teamId;
    UUID profileId;

    public TeamMembershipPk(UUID teamId, UUID profileId) {
        this.teamId = teamId;
        this.profileId = profileId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TeamMembershipPk pk
                && Objects.equals(teamId, pk.teamId)
                && Objects.equals(profileId, pk.profileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, profileId);
    }
}
