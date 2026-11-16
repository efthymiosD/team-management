package com.decoupledx.teammanagement.team.adapter.persistence;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PlayerProfileEntity {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = true)
    private String phone;

    @Column(name = "birth_date", nullable = true)
    private LocalDate birthDate;

    @Column(nullable = true)
    private String nationality;

    @Column(name = "photo_path")
    private String photoPath;

    @Column(name = "account_id")
    @Setter
    private UUID accountId;

    @Column(name = "claim_token_hash")
    @Setter
    private String claimTokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @jakarta.persistence.Version
    private long version;

    PlayerProfileEntity(PlayerProfileDataValue data) {
        this.id = data.id();
        this.fullName = data.fullName();
        this.phone = data.phone();
        this.birthDate = data.birthDate();
        this.nationality = data.nationality();
        this.photoPath = data.photoPath();
        this.accountId = data.accountId();
        this.claimTokenHash = data.claimTokenHash();
        this.createdAt = data.createdAt();
        this.updatedAt = data.updatedAt();
    }

    void syncFrom(PlayerProfileDataValue data) {
        this.fullName = data.fullName();
        this.phone = data.phone();
        this.birthDate = data.birthDate();
        this.nationality = data.nationality();
        this.photoPath = data.photoPath();
        this.accountId = data.accountId();
        this.claimTokenHash = data.claimTokenHash();
        this.updatedAt = data.updatedAt();
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
}
