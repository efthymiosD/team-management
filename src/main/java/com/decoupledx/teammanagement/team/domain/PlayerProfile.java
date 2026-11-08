package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A person who participates in games — deliberately NOT an account. Profiles
 * may initially exist without an account (admin-created, claimable later via
 * a per-profile token); account linking is optional and unique (one profile
 * per account). Photos live in the uploads directory, referenced by web path
 * (ADR-0002).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PlayerProfile {

    static final int NAME_MAX_LENGTH = 200;
    static final int NATIONALITY_MAX_LENGTH = 100;

    private UUID id;
    private String fullName;
    private String contactDetails;
    private String nationality;
    private String photoPath;

    @Setter
    private UUID accountId;

    @Setter
    private String claimToken;

    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    private PlayerProfile(UUID id, String fullName, String contactDetails, String nationality,
            String photoPath, UUID accountId, String claimToken, Instant createdAt, Instant updatedAt,
            long version) {
        this.id = id;
        this.fullName = fullName;
        this.contactDetails = contactDetails;
        this.nationality = nationality;
        this.photoPath = photoPath;
        this.accountId = accountId;
        this.claimToken = claimToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    static PlayerProfile forAccount(UUID accountId, String displayName, Instant now) {
        return new PlayerProfile(UUID.randomUUID(),
                validateName(displayName, "player name"), null, null, null,
                Objects.requireNonNull(accountId, "account id must not be null"),
                null, now, now, 0L);
    }

    /**
     * Admin-managed registration of an unclaimed person: no account, the
     * claim token lets them link their own account later. Optional fields are
     * normalized: contact/nationality blanks become null.
     */
    static PlayerProfile unclaimed(String fullName, String contactDetails, String nationality,
            String claimToken, Instant now) {
        return new PlayerProfile(UUID.randomUUID(),
                validateName(fullName, "player name"), normalize(contactDetails, 2000),
                normalize(nationality, NATIONALITY_MAX_LENGTH), null,
                null, Objects.requireNonNull(claimToken, "claim token must not be null"),
                now, now, 0L);
    }

    static PlayerProfile from(PlayerProfileDataValue data) {
        return new PlayerProfile(data.id(), data.fullName(), data.contactDetails(), data.nationality(),
                data.photoPath(), data.accountId(), data.claimToken(), data.createdAt(),
                data.updatedAt(), data.version());
    }

    PlayerProfileDataValue toData() {
        return PlayerProfileDataValue.builder()
                .id(id)
                .fullName(fullName)
                .contactDetails(contactDetails)
                .nationality(nationality)
                .photoPath(photoPath)
                .accountId(accountId)
                .claimToken(claimToken)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(version)
                .build();
    }

    void updateContactDetails(String contactDetails, Instant now) {
        this.contactDetails = contactDetails;
        this.updatedAt = now;
    }

    /** Claiming: binds the acting account and consumes the claim token (single use). */
    void linkAccount(UUID accountId, Instant now) {
        this.accountId = Objects.requireNonNull(accountId, "account id must not be null");
        this.updatedAt = now;
    }

    void clearClaimToken(Instant now) {
        this.claimToken = null;
        this.updatedAt = now;
    }

    void applyPhoto(String photoPath, Instant now) {
        Objects.requireNonNull(photoPath, "photo path must not be null");
        this.photoPath = photoPath;
        this.updatedAt = now;
    }

    private static String validateName(String name, String label) {
        if (name == null) {
            throw new IllegalArgumentException(label + " must not be null");
        }
        String trimmed = name.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(label + " must be at most " + NAME_MAX_LENGTH + " characters");
        }
        return trimmed;
    }

    /** Optional-field normalization: strip; a blank value becomes null. */
    private static String normalize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("value must be at most " + maxLength + " characters");
        }
        return trimmed;
    }
}
