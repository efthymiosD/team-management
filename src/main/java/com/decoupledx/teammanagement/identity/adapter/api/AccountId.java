package com.decoupledx.teammanagement.identity.adapter.api;

import java.util.Objects;
import java.util.UUID;

public record AccountId(String value) {

    public AccountId {
        Objects.requireNonNull(value, "account id must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("account id must not be blank");
        }
    }

    public static AccountId of(String value) {
        return new AccountId(value);
    }

    public static AccountId random() {
        return new AccountId(UUID.randomUUID().toString());
    }
}
