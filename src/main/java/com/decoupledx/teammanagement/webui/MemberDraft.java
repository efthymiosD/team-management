package com.decoupledx.teammanagement.webui;

/**
 * The add-member form's submitted-but-rejected draft (ADR-0003: records in
 * their own files) — rides the redirect as a flash attribute so every typed
 * value survives the error round-trip (input-preservation rule).
 */
record MemberDraft(String fullName, String phone, String birthDate, String nationality) {
}
