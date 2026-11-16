package com.decoupledx.teammanagement.webui;

/**
 * Presentation-only phone formatting (stored values keep the validated
 * compact/international shape): digits chunked with spaces for readability,
 * plus the compact digits for a {@code tel:} link. Not domain logic — never
 * round-trips into the domain.
 */
final class PhoneDisplay {

    private PhoneDisplay() {
    }

    /** "+48600123456" → "+48 600 123 456" (country code split, 3-digit chunks). */
    static String display(String phone) {
        String digits = digitsOf(phone);
        if (digits.length() < 4) {
            return phone;
        }
        String countryCode = digits.substring(0, countryCodeLength(digits));
        String national = digits.substring(countryCode.length());
        return "+" + countryCode + " " + chunkFromLeft(national);
    }

    /** Compact digits only, for the {@code tel:} URI. */
    static String href(String phone) {
        return "+" + digitsOf(phone);
    }

    private static String digitsOf(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }

    /** One-digit codes (1, 7) aside, assume a two-digit country code. */
    private static int countryCodeLength(String digits) {
        return digits.length() > 1 && (digits.charAt(0) == '1' || digits.charAt(0) == '7') ? 1 : 2;
    }

    /** "600123456" → "600 123 456"; the tail group carries the remainder. */
    private static String chunkFromLeft(String digits) {
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < digits.length(); i += 3) {
            if (i > 0) {
                grouped.append(' ');
            }
            grouped.append(digits, i, Math.min(i + 3, digits.length()));
        }
        return grouped.toString();
    }
}
