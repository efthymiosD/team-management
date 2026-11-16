package com.decoupledx.teammanagement.webui;

/**
 * The profile page's submitted-but-rejected form draft (ADR-0003: records in
 * their own files). On a failed submit it rides the redirect as a flash
 * attribute so EVERY typed value survives the error round-trip (general
 * rule: player input is always preserved). String fields — rendered straight
 * back into the form inputs.
 */
record ProfileForm(String fullName, String phone, String birthDate, String nationality) {
}
