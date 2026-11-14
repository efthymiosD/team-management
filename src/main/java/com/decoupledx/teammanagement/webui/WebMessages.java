package com.decoupledx.teammanagement.webui;

import com.decoupledx.common.error.BusinessException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single entry point for user-facing strings generated on the server side
 * (flash messages, view labels, error-code explanations). Keys come from the
 * shared bundles (src/main/resources/messages); the locale is the request's
 * resolved locale.
 *
 * <p>Business error codes map to {@code error.<code>} keys — the domain stays
 * language-agnostic and the web layer explains codes in the user's language.</p>
 */
@Component
@RequiredArgsConstructor
public class WebMessages {

    private final MessageSource messageSource;

    /** Plain key lookup (labels, flash copy, static fragments). */
    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, currentLocale());
    }

    /**
     * Explanation for a business exception in the user's language; unknown
     * codes fall back to the exception's own message.
     */
    public String errorMessage(BusinessException exception) {
        String key = "error." + exception.errorCode().name().toLowerCase();
        return messageSource.getMessage(key, null, exception.getMessage(), currentLocale());
    }

    private Locale currentLocale() {
        return LocaleContextHolder.getLocale();
    }
}
