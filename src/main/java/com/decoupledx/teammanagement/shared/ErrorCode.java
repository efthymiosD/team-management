package com.decoupledx.teammanagement.shared;


/**
 * Application-owned error codes implementing the shared
 * {@code com.decoupledx.common.error.ErrorCode} contract. Deliberately minimal
 * at bootstrap: every entry is added together with the feature that owns it,
 * so codes stay tied to real domain rules instead of accumulating speculative
 * entries.
 */
public enum ErrorCode implements com.decoupledx.common.error.ErrorCode {

    UNEXPECTED_ERROR(500, "An unexpected error occurred");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
