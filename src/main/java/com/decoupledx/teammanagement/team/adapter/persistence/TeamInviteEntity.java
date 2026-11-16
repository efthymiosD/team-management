package com.decoupledx.teammanagement.team.adapter.persistence;

import com.decoupledx.teammanagement.team.domain.port.TeamInviteDataValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One-time team-join invite. Storage carries the SHA-256 HASH only — the raw
 * token appeared exactly once, in the minted link (decoupledx-common ADR-0003).
 */
@Entity
@Table(name = "team_invites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TeamInviteEntity {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_by")
    private UUID usedBy;

    TeamInviteEntity(UUID id, UUID teamId, String tokenHash, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.teamId = teamId;
        this.tokenHash = tokenHash;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    TeamInviteDataValue toData() {
        return TeamInviteDataValue.builder()
                .id(id)
                .teamId(teamId)
                .tokenHash(tokenHash)
                .createdBy(createdBy)
                .createdAt(createdAt)
                .usedAt(usedAt)
                .usedBy(usedBy)
                .build();
    }
}
