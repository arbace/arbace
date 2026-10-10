/*
 * jrt's own sun.util.locale.provider.JavaTimeDateTimePatternImpl for the Go build
 * (doc/go/JRT-NOTES.md, "Time"). Its two lookups are transcribed from openjdk/jdk26u at
 * baf63fb, src/java.base/share/classes/sun/util/locale/provider/JavaTimeDateTimePatternImpl.java,
 * Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
 * with the Classpath Exception (LICENSE.md); the rest is Arbace's own.
 */
package sun.util.locale.provider;

import java.time.DateTimeException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import sun.text.spi.JavaTimeDateTimePatternProvider;

/**
 * java.time's localized patterns (DateTimeFormatterBuilder.getLocalizedDateTimePattern) from the
 * locale's resources (LocaleResources over java.base's CLDR data), as the JDK's provider gives
 * them; the one provider of the Go build's one adapter.
 */
public class JavaTimeDateTimePatternImpl extends JavaTimeDateTimePatternProvider {
    static final JavaTimeDateTimePatternImpl INSTANCE = new JavaTimeDateTimePatternImpl();

    private JavaTimeDateTimePatternImpl() {
    }

    /** The locales of java.base's CLDR data: the root locale, en, en_US. */
    @Override
    public Locale[] getAvailableLocales() {
        return new Locale[] {Locale.ROOT, Locale.ENGLISH, Locale.US};
    }

    @Override
    public String getJavaTimeDateTimePattern(int timeStyle, int dateStyle, String calType, Locale locale) {
        LocaleResources lr = LocaleProviderAdapter.getResourceBundleBased().getLocaleResources(locale);
        return lr.getJavaTimeDateTimePattern(timeStyle, dateStyle, calType);
    }

    @Override
    public String getJavaTimeDateTimePattern(String requestedTemplate, String calType, Locale locale) {
        LocaleProviderAdapter lpa = LocaleProviderAdapter.getResourceBundleBased();
        return LocaleResources.candidateLocales(locale).stream()
                .map(lpa::getLocaleResources)
                .map(lr -> lr.getLocalizedPattern(requestedTemplate, calType))
                .filter(Objects::nonNull)
                .findFirst()
                .or(() -> calType.equals("generic") ? Optional.empty():
                        Optional.of(getJavaTimeDateTimePattern(requestedTemplate, "generic", locale)))
                .orElseThrow(() -> new DateTimeException("Requested template \"" + requestedTemplate +
                        "\" cannot be resolved in the locale \"" + locale + "\""));
    }
}
