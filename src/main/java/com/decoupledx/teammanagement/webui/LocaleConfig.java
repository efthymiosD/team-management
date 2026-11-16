package com.decoupledx.teammanagement.webui;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Locale resolution for the server-rendered UI:
 *
 * <ol>
 *   <li>explicit user choice persisted in the "locale" cookie (set via
 *       {@code ?lang=<tag>} through the LocaleChangeInterceptor),</li>
 *   <li>otherwise the browser's Accept-Language header,</li>
 *   <li>otherwise the default language (English).</li>
 * </ol>
 *
 * <p>Every resolved locale is constrained to the shipped languages
 * ({@link SupportedLocales}): a stale cookie or a foreign client value
 * silently falls back to the default instead of rendering an odd hybrid.
 * Unlike the Reservation System there is no admin-controlled enable/disable
 * yet — all shipped languages are always on.</p>
 */
@Configuration
class LocaleConfig implements WebMvcConfigurer {

    /** One year — the explicit choice is meant to survive browser restarts. */
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    private final ProfileCompletionGate profileCompletionGate;

    LocaleConfig(ProfileCompletionGate profileCompletionGate) {
        this.profileCompletionGate = profileCompletionGate;
    }

    @Bean
    LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("locale") {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                return sanitize(super.resolveLocale(request), request);
            }
        };
        resolver.setDefaultLocaleFunction(request ->
                sanitize(request.getLocale(), request));
        resolver.setCookieMaxAge(COOKIE_MAX_AGE);
        resolver.setCookieHttpOnly(false);
        return resolver;
    }

    /** Out-of-set locales fall back to the default language. */
    private Locale sanitize(Locale candidate, HttpServletRequest request) {
        if (candidate == null) {
            return SupportedLocales.DEFAULT;
        }
        boolean supported = SupportedLocales.ALL.stream()
                .anyMatch(shipped -> shipped.getLanguage().equals(candidate.getLanguage()));
        if (supported) {
            return candidate;
        }
        Locale requested = request.getLocale();
        if (requested != null && SupportedLocales.ALL.stream()
                .anyMatch(shipped -> shipped.getLanguage().equals(requested.getLanguage()))) {
            return requested;
        }
        return SupportedLocales.DEFAULT;
    }

    @Bean
    LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response, Object handler)
                    throws jakarta.servlet.ServletException {
                String requested = request.getParameter(getParamName());
                if (requested != null && !SupportedLocales.tags().contains(
                        requested.toLowerCase(Locale.ROOT))) {
                    return true;
                }
                return super.preHandle(request, response, handler);
            }
        };
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        // Post-registration completeness gate (owner feedback #6): the gate
        // itself exempts the surfaces it must not trap and only ever acts on
        // the server-rendered UI — never on /api/**.
        registry.addInterceptor(profileCompletionGate()).addPathPatterns("/**")
                .excludePathPatterns("/api/**", "/error");
    }

    ProfileCompletionGate profileCompletionGate() {
        return profileCompletionGate;
    }
}
