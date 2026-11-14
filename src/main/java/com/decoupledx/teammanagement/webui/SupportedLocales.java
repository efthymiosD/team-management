package com.decoupledx.teammanagement.webui;

import java.util.List;
import java.util.Locale;

/**
 * The locales the UI actually ships translations for. Adding a language
 * means: a messages_&lt;tag&gt;.properties bundle, a new entry here, and a
 * new option in the navigation language selector (fragments/navigation).
 *
 * <p>Every shipped language is always enabled — team-management has no
 * admin-controlled site-content module yet (the Reservation System gates
 * languages there); when that capability arrives, the gate lands in
 * {@code LocaleConfig}.</p>
 */
public final class SupportedLocales {

    public static final Locale DEFAULT = Locale.forLanguageTag("en");
    /** Display order: English, Polish, Greek. */
    public static final List<Locale> ALL =
            List.of(Locale.forLanguageTag("en"), Locale.forLanguageTag("pl"), Locale.forLanguageTag("el"));

    private SupportedLocales() {}

    /** Language tags of {@link #ALL}, lowercase, in display order ("en", "pl", "el"). */
    public static List<String> tags() {
        return ALL.stream().map(Locale::getLanguage).toList();
    }
}
