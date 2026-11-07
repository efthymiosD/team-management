package com.decoupledx.teammanagement.shared;


/**
 * Application-owned error codes implementing the shared
 * {@code com.decoupledx.common.error.ErrorCode} contract. Deliberately minimal
 * at bootstrap: every entry is added together with the feature that owns it,
 * so codes stay tied to real domain rules instead of accumulating speculative
 * entries.
 */
public enum ErrorCode implements com.decoupledx.common.error.ErrorCode {

    UNEXPECTED_ERROR(500, "An unexpected error occurred"),
    TEAM_NOT_FOUND(404, "Team not found"),
    PROFILE_NOT_FOUND(404, "Player profile not found"),
    MEMBER_NOT_FOUND(404, "Member not found in this team"),
    TEAM_NAME_TAKEN(409, "A team with this name already exists"),
    ALREADY_MEMBER(409, "Player is already a member of this team"),
    NOT_TEAM_ADMIN(403, "Only team administrators may perform this action"),
    NOT_TEAM_MEMBER(403, "You are not a member of this team"),
    LAST_ADMIN(409, "Cannot remove or demote the last administrator of a team");

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
