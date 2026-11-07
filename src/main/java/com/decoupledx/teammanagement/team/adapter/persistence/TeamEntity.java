package com.decoupledx.teammanagement.team.adapter.persistence;

import com.decoupledx.teammanagement.team.domain.TeamDataValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TeamEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @jakarta.persistence.Version
    private long version;

    TeamEntity(TeamDataValue data) {
        this.id = data.id();
        this.name = data.name();
        this.logoPath = data.logoPath();
        this.createdAt = data.createdAt();
        this.updatedAt = data.updatedAt();
    }

    void syncFrom(TeamDataValue data) {
        this.name = data.name();
        this.logoPath = data.logoPath();
        this.updatedAt = data.updatedAt();
    }

    TeamDataValue toData() {
        return TeamDataValue.builder()
                .id(id)
                .name(name)
                .logoPath(logoPath)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(version)
                .build();
    }
}
