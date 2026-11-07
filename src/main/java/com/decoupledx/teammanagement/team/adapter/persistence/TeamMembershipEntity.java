package com.decoupledx.teammanagement.team.adapter.persistence;

import com.decoupledx.teammanagement.team.domain.TeamMembershipDataValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "team_memberships")
@IdClass(TeamMembershipPk.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TeamMembershipEntity {

    @Id
    @Column(name = "team_id")
    private UUID teamId;

    @Id
    @Column(name = "profile_id")
    private UUID profileId;

    @Column(nullable = false)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @jakarta.persistence.Version
    private long version;

    TeamMembershipEntity(TeamMembershipDataValue data) {
        this.teamId = data.teamId();
        this.profileId = data.profileId();
        this.role = data.role();
        this.joinedAt = data.joinedAt();
    }

    void syncFrom(TeamMembershipDataValue data) {
        this.role = data.role();
    }

    TeamMembershipDataValue toData() {
        return TeamMembershipDataValue.builder()
                .teamId(teamId)
                .profileId(profileId)
                .role(role)
                .joinedAt(joinedAt)
                .version(version)
                .build();
    }
}
