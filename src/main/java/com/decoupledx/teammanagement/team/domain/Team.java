package com.decoupledx.teammanagement.team.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A group that organizes matches. Names are globally unique (DB-enforced),
 * 1..200 characters after trimming; the optional logo is a web path into the
 * uploads directory (the bytes live on disk — ADR-0002).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Team {

    static final int NAME_MAX_LENGTH = 200;

    private UUID id;
    private String name;
    private String logoPath;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    private Team(UUID id, String name, String logoPath, Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.name = name;
        this.logoPath = logoPath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    static Team create(String name, Instant now) {
        String validName = validateName(name);
        return new Team(UUID.randomUUID(), validName, null, now, now, 0L);
    }

    static Team from(TeamDataValue data) {
        return new Team(data.id(), data.name(), data.logoPath(), data.createdAt(), data.updatedAt(),
                data.version());
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

    void rename(String name, Instant now) {
        this.name = validateName(name);
        this.updatedAt = now;
    }

    void applyLogo(String logoPath, Instant now) {
        Objects.requireNonNull(logoPath, "logo path must not be null");
        this.logoPath = logoPath;
        this.updatedAt = now;
    }

    private static String validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("team name must not be null");
        }
        String trimmed = name.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("team name must not be blank");
        }
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("team name must be at most " + NAME_MAX_LENGTH + " characters");
        }
        return trimmed;
    }
}
