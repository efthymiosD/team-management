package com.decoupledx.teammanagement.team.domain;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.teammanagement.shared.ErrorCode;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A person who participates in games — deliberately NOT an account. Profiles
 * may initially exist without an account (admin-created, claimable later via
 * a per-profile token); account linking is optional and unique (one profile
 * per account). Photos live in the uploads directory, referenced by web path
 * (ADR-0002). The contact set is a validated phone number (required on save)
 * plus an optional birthday; nationality is an ISO 3166-1 alpha-2 code.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PlayerProfile {

    static final int NAME_MAX_LENGTH = 200;

    /** Liberal international format (decision 8): leading +, digits/spaces, 7–20 chars. */
    private static final Pattern PHONE = Pattern.compile("^\\+[0-9][0-9 -]{5,18}[0-9]$");

    /** ISO 3166-1 alpha-2: exactly two uppercase letters. */
    private static final Pattern COUNTRY_CODE = Pattern.compile("^[A-Z]{2}$");

    private static final int BIRTH_YEAR_MIN = 1900;

    private UUID id;
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    private String nationality;
    private String photoPath;

    @Setter
    private UUID accountId;

    @Setter
    private String claimTokenHash;

    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    private PlayerProfile(UUID id, String fullName, String phone, LocalDate birthDate,
            String nationality, String photoPath, UUID accountId, String claimTokenHash,
            Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.birthDate = birthDate;
        this.nationality = nationality;
        this.photoPath = photoPath;
        this.accountId = accountId;
        this.claimTokenHash = claimTokenHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    static PlayerProfile forAccount(UUID accountId, String displayName, Instant now) {
        return new PlayerProfile(UUID.randomUUID(),
                validateName(displayName), null, null, null, null,
                Objects.requireNonNull(accountId, "account id must not be null"),
                null, now, now, 0L);
    }

    /**
     * Admin-managed registration of an unclaimed person: no account, the
     * claim token lets them link their own account later. Phone is optional
     * here (validated when given — the player completes it at claim time);
     * nationality is an optional ISO country code.
     */
    static PlayerProfile unclaimed(String fullName, String phone, LocalDate birthDate,
            String nationality, String claimTokenHash, Instant now) {
        return new PlayerProfile(UUID.randomUUID(),
                validateName(fullName), normalizePhone(phone, false), validateBirthDate(birthDate, now),
                normalizeNationality(nationality),
                null, null, Objects.requireNonNull(claimTokenHash, "claim token hash must not be null"),
                now, now, 0L);
    }

    static PlayerProfile from(PlayerProfileDataValue data) {
        return new PlayerProfile(data.id(), data.fullName(), data.phone(), data.birthDate(),
                data.nationality(), data.photoPath(), data.accountId(), data.claimTokenHash(),
                data.createdAt(), data.updatedAt(), data.version());
    }

    PlayerProfileDataValue toData() {
        return PlayerProfileDataValue.builder()
                .id(id)
                .fullName(fullName)
                .phone(phone)
                .birthDate(birthDate)
                .nationality(nationality)
                .photoPath(photoPath)
                .accountId(accountId)
                .claimTokenHash(claimTokenHash)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(version)
                .build();
    }

    /** Claiming: binds the acting account and consumes the claim token (single use). */
    void linkAccount(UUID accountId, Instant now) {
        this.accountId = Objects.requireNonNull(accountId, "account id must not be null");
        this.updatedAt = now;
    }

    void clearClaimTokenHash(Instant now) {
        this.claimTokenHash = null;
        this.updatedAt = now;
    }

    /**
     * Self-service profile edit — the owner's full field set (name included).
     * Phone is REQUIRED and validated (liberal international format);
     * birthday must be a plausible past date; nationality must be an ISO
     * 3166-1 alpha-2 code (blank normalizes to null).
     */
    void updateDetails(String fullName, String phone, LocalDate birthDate, String nationality,
            Instant now) {
        this.fullName = validateName(fullName);
        this.phone = normalizePhone(phone, true);
        this.birthDate = validateBirthDate(birthDate, now);
        this.nationality = normalizeNationality(nationality);
        this.updatedAt = now;
    }

    void applyPhoto(String photoPath, Instant now) {
        Objects.requireNonNull(photoPath, "photo path must not be null");
        this.photoPath = photoPath;
        this.updatedAt = now;
    }

    /** The self-service required-info set is complete once a phone is saved. */
    boolean isComplete() {
        return phone != null;
    }

    private static String validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("player name must not be null");
        }
        String trimmed = name.strip();
        if (trimmed.isEmpty() || trimmed.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.PROFILE_FIELD_INVALID);
        }
        return trimmed;
    }

    /**
     * Phone is the required contact field on self-service save: a blank
     * value is a validation error there; admin-registered unclaimed profiles
     * may exist without one (required=false) until claimed. A malformed
     * value is always an error.
     */
    private static String normalizePhone(String phone, boolean required) {
        if (phone == null) {
            if (required) {
                throw new BusinessException(ErrorCode.PROFILE_FIELD_INVALID);
            }
            return null;
        }
        String trimmed = phone.strip();
        if (trimmed.isEmpty()) {
            if (required) {
                throw new BusinessException(ErrorCode.PROFILE_FIELD_INVALID);
            }
            return null;
        }
        if (!PHONE.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.PROFILE_FIELD_INVALID);
        }
        return trimmed;
    }

    private static LocalDate validateBirthDate(LocalDate birthDate, Instant now) {
        if (birthDate == null) {
            return null;
        }
        if (birthDate.isAfter(LocalDate.ofInstant(now, ZoneOffset.UTC))
                || birthDate.getYear() < BIRTH_YEAR_MIN) {
            throw new BusinessException(ErrorCode.PROFILE_FIELD_INVALID);
        }
        return birthDate;
    }

    /**
     * Nationality is optional; a given value must be an ISO 3166-1 alpha-2
     * code (the searchable dropdown supplies codes; flags are presentation).
     */
    private static String normalizeNationality(String nationality) {
        if (nationality == null) {
            return null;
        }
        String trimmed = nationality.strip().toUpperCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!COUNTRY_CODE.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.PROFILE_FIELD_INVALID);
        }
        return trimmed;
    }
}
