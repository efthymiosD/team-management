package com.decoupledx.teammanagement.webui;

import java.util.List;
import java.util.Locale;

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
