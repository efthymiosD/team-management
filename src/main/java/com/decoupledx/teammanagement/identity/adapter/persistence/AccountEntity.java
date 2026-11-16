package com.decoupledx.teammanagement.identity.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AccountEntity {

    @Id
    private UUID accountId;

    @Column(name = "idp_subject", nullable = false, unique = true)
    private String idpSubject;

    @Column(name = "display_name")
    @Setter
    private String displayName;

    @Column(nullable = true, unique = true)
    @Setter
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    AccountEntity(UUID accountId, String idpSubject, String displayName, String email,
            Instant createdAt) {
        this.accountId = accountId;
        this.idpSubject = idpSubject;
        this.displayName = displayName;
        this.email = email;
        this.createdAt = createdAt;
    }
}
